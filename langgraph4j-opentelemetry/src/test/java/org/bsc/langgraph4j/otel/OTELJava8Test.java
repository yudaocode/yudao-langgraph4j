package org.bsc.langgraph4j.otel;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executes workflow instrumentation and actual SDK export on the target Java 8 VM.
 *
 * @author 芋道源码
 */
class OTELJava8Test {

    private InMemorySpanExporter spans;
    private InMemoryLogRecordExporter logs;
    private InMemoryMetricReader metrics;
    private OpenTelemetrySdk sdk;

    @BeforeEach
    void initialize() {
        assertEquals(System.getProperty("compat.expected.java", "1.8"), System.getProperty("java.specification.version"));
        spans = InMemorySpanExporter.create();
        logs = InMemoryLogRecordExporter.create();
        metrics = InMemoryMetricReader.create();
        sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder()
                        .addSpanProcessor(SimpleSpanProcessor.create(spans)).build())
                .setLoggerProvider(SdkLoggerProvider.builder()
                        .addLogRecordProcessor(SimpleLogRecordProcessor.create(logs)).build())
                .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(metrics).build())
                .build();
        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(sdk);
    }

    @AfterEach
    void close() {
        GlobalOpenTelemetry.resetForTest();
        sdk.close();
    }

    @Test
    void nodeHookRecordsStateAndCompletesTheSpan() throws Exception {
        OTELWrapCallTraceHook<AgentState> hook = new OTELWrapCallTraceHook<>(
                new ObjectStreamStateSerializer<>(AgentState::new));
        Map<String, Object> expected = Collections.singletonMap("answer", "你好");
        Map<String, Object> result = hook.applyWrap("node", new AgentState(Collections.emptyMap()),
                RunnableConfig.empty(), (AsyncNodeActionWithConfig<AgentState>) (state, config) -> {
                    assertTrue(Span.current().getSpanContext().isValid());
                    return CompletableFuture.completedFuture(expected);
                }).get(5, TimeUnit.SECONDS);
        assertEquals(expected, result);
        assertEquals(1, spans.getFinishedSpanItems().size());
        SpanData span = spans.getFinishedSpanItems().get(0);
        assertEquals("evaluateNode", span.getName());
        assertEquals("node", span.getAttributes().get(AttributeKey.stringKey("nodeId")));
        assertEquals(StatusCode.OK, span.getStatus().getStatusCode());
        assertEquals(2, span.getEvents().size());
        assertTrue(span.getEvents().get(1).getAttributes().get(AttributeKey.stringKey("lg4j.state")).contains("你好"));
        assertFalse(Span.current().getSpanContext().isValid());
    }

    @Test
    void scopedBaggageIsVisibleOnlyDuringTheCall() throws Throwable {
        OTELObservable observable = new OTELObservable() { };
        Baggage baggage = Baggage.builder().put("principal", "用户").build();
        String result = observable.shareBaggageToCall(baggage,
                (String ignored) -> OTELObservable.sharedBaggage().getEntryValue("principal")).apply("input");
        assertEquals("用户", result);
        assertTrue(OTELObservable.sharedBaggage().isEmpty());
        assertEquals("用户", OTELObservable.attrsOf(baggage).get(AttributeKey.stringKey("principal")));
    }

    @Test
    void checkedFailureIsRecordedAndTheSpanEnds() {
        OTELObservable.TracerHolder holder = new OTELObservable.TracerHolder(sdk, "test");
        Span span = holder.spanBuilder("failure").startSpan();
        Exception expected = new Exception("失败");
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> holder.tryApplySpan(span, ignored -> { throw expected; }));
        assertSame(expected, thrown.getCause());
        assertEquals(1, spans.getFinishedSpanItems().size());
        assertEquals("exception", spans.getFinishedSpanItems().get(0).getEvents().get(0).getName());
    }

    @Test
    void appenderExportsLogback12RecordsWithTheCurrentTrace() {
        Logger logger = (Logger) LoggerFactory.getLogger("java8-otel-local-test");
        Level previousLevel = logger.getLevel();
        OpenTelemetryAppender appender = new OpenTelemetryAppender();
        appender.setContext(logger.getLoggerContext());
        appender.setOpenTelemetry(sdk);
        appender.setCaptureKeyValuePairAttributes(true);
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        Span span = sdk.getTracer("test").spanBuilder("logging").startSpan();
        try (io.opentelemetry.context.Scope ignored = span.makeCurrent()) {
            logger.info("本地日志 {}", "你好");
        }
        finally {
            span.end();
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
        }
        assertEquals(1, logs.getFinishedLogRecordItems().size());
        assertEquals("本地日志 你好", logs.getFinishedLogRecordItems().get(0).getBodyValue().asString());
        assertEquals(span.getSpanContext().getTraceId(), logs.getFinishedLogRecordItems().get(0).getSpanContext().getTraceId());
    }

    @Test
    void meterHolderRecordsRealSdkCounterData() {
        OTELObservable.MeterHolder holder = new OTELObservable.MeterHolder(sdk, "test");
        holder.countBuilder("invocations").build().add(2, Attributes.of(AttributeKey.stringKey("route"), "jdk8"));
        assertEquals(1, metrics.collectAllMetrics().size());
        assertEquals(2, metrics.collectAllMetrics().iterator().next()
                .getLongSumData().getPoints().iterator().next().getValue());
    }

    @Test
    void realHttpExporterPreservesUnicode() throws Exception {
        AtomicReference<ExportTraceServiceRequest> received = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/traces", exchange -> {
            try {
                received.set(ExportTraceServiceRequest.parseFrom(exchange.getRequestBody()));
                exchange.getResponseHeaders().set("Content-Type", "application/x-protobuf");
                exchange.sendResponseHeaders(200, -1);
            }
            finally {
                exchange.close();
            }
        });
        server.start();
        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/traces").build();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();
        try {
            provider.get("java8-http-test").spanBuilder("你好 café").startSpan().end();
            assertTrue(provider.forceFlush().join(5, TimeUnit.SECONDS).isSuccess());
            assertNotNull(received.get());
            assertEquals("你好 café", received.get().getResourceSpans(0).getScopeSpans(0).getSpans(0).getName());
        }
        finally {
            provider.close();
            server.stop(0);
        }
    }
}
