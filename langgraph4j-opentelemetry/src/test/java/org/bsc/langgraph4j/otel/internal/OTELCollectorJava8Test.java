package org.bsc.langgraph4j.otel.internal;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises protobuf decoding and Java 8 file output through the real local collector.
 *
 * @author 芋道源码
 */
class OTELCollectorJava8Test {

    @TempDir
    Path directory;

    @Test
    void receivesAllThreeSignalsAndWritesUtf8Files() throws Exception {
        try (OTELInternalHttpCollector collector = OTELInternalHttpCollector.builder()
                .bindAddress("127.0.0.1").port(0).outputDir(directory).buildAndStart()) {
            ExportLogsServiceRequest logs = ExportLogsServiceRequest.newBuilder()
                    .addResourceLogs(ResourceLogs.newBuilder().addScopeLogs(ScopeLogs.newBuilder()
                            .addLogRecords(LogRecord.newBuilder()
                                    .setBody(AnyValue.newBuilder().setStringValue("你好 café"))))).build();
            assertEquals(200, post(collector.logsEndpoint(), logs.toByteArray()));
            assertEquals(200, post(collector.tracerEndpoint(), ExportTraceServiceRequest.getDefaultInstance().toByteArray()));
            assertEquals(200, post(collector.metricEndpoint(), ExportMetricsServiceRequest.getDefaultInstance().toByteArray()));
            assertTrue(new String(Files.readAllBytes(directory.resolve("otlp-logs.json")), StandardCharsets.UTF_8).contains("你好 café"));
            assertTrue(Files.exists(directory.resolve("otlp-spans.json")));
            assertTrue(Files.exists(directory.resolve("otlp-metric.json")));
        }
    }

    private int post(URL endpoint, byte[] body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection(java.net.Proxy.NO_PROXY);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-protobuf");
        try {
            try (java.io.OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            int status = connection.getResponseCode();
            try (java.io.InputStream input = status < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                while (input.read() >= 0) { }
            }
            return status;
        }
        finally {
            connection.disconnect();
        }
    }
}
