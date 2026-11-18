package com.agui.community.client;

import com.agui.community.core.agent.Agent;
import com.agui.community.core.agent.RunAgentInput;
import com.agui.community.core.event.Event;
import com.agui.community.core.serialization.Serializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import org.springframework.ai.compat.jdkhttp.FlowAdapters;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * An {@link Agent} that runs against a remote AG-UI endpoint over HTTP. It
 * serializes the {@link RunAgentInput} as JSON, POSTs it to the configured
 * endpoint, and decodes the Server-Sent Events response into a stream of
 * {@link Event}s.
 *
 * <p>The HTTP transport and the injected {@link Serializer} are independent of the concrete JSON library.
 *
 * <p>Each subscription to the returned publisher triggers a fresh run: the
 * request is sent when a subscriber subscribes, events are emitted in order, and
 * the publisher completes when the stream ends or signals {@code onError} if the
 * request fails or returns an error status.
 */
public final class HttpAgent implements Agent {

    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(5);

    /**
     * Shared executor used by the {@linkplain #HttpAgent(URI, Serializer) default
     * constructor}. Consuming an SSE response is a long-lived blocking operation,
     * so running it on a bounded pool shared across the JVM (such as
     * {@code ForkJoinPool.commonPool()}) risks starving that pool — a burst of
     * concurrent runs could occupy every worker and deadlock. A cached thread
     * pool sidesteps this: it creates threads on demand (reusing idle ones) and
     * is effectively unbounded, so a blocking stream read never starves other
     * runs.
     */
    private static final Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "ag-ui-http-stream");
        thread.setDaemon(true);
        return thread;
    });

    private final URI endpoint;
    private final Serializer serializer;
    private final HttpClient httpClient;
    private final Executor executor;
    private final Duration requestTimeout;

    /**
     * Creates an agent using a default {@link HttpClient} and a cached thread
     * pool for stream processing.
     *
     * @param endpoint   the URI of the remote AG-UI endpoint
     * @param serializer the serializer used to encode the input and decode events
     */
    public HttpAgent(URI endpoint, Serializer serializer) {
        this(endpoint, serializer, HttpClient.newHttpClient(), DEFAULT_EXECUTOR,
                DEFAULT_REQUEST_TIMEOUT);
    }

    /**
     * Creates an agent with full control over the HTTP client, executor and
     * request timeout.
     *
     * @param endpoint       the URI of the remote AG-UI endpoint
     * @param serializer     the serializer used to encode the input and decode
     *                       events
     * @param httpClient     the HTTP client to use
     * @param executor       the executor on which the (blocking) response stream
     *                       is consumed and events are published
     * @param requestTimeout the per-request timeout
     */
    public HttpAgent(URI endpoint, Serializer serializer, HttpClient httpClient, Executor executor,
                     Duration requestTimeout) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
    }

    @Override
    public Flow.Publisher<Event> run(RunAgentInput input) {
        Objects.requireNonNull(input, "input must not be null");
        Flux<Event> source = Flux.using(() -> open(input), body -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
            SseEventParser parser = new SseEventParser();
            return Flux.<Event>generate(sink -> {
                try {
                    Optional<String> data = nextEvent(reader, parser);
                    if (data.isPresent()) {
                        sink.next(decode(data.get()));
                    } else {
                        sink.complete();
                    }
                } catch (Exception failure) {
                    sink.error(failure);
                }
            });
        }, HttpAgent::close).subscribeOn(Schedulers.fromExecutor(executor));
        return FlowAdapters.publisherToFlowPublisher(source);
    }

    private InputStream open(RunAgentInput input) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(serializer.serialize(input)))
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                try (InputStream ignored = response.body()) {
                    throw new HttpAgentException("AG-UI endpoint returned HTTP " + response.statusCode());
                }
            }
            return response.body();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw failure;
        }
    }

    private static Optional<String> nextEvent(BufferedReader reader, SseEventParser parser) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            Optional<String> data = parser.feed(line);
            if (data.isPresent()) {
                return data;
            }
        }
        return parser.flush();
    }

    private static void close(InputStream body) {
        try {
            body.close();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private Event decode(String data) {
        return serializer.deserialize(data, Event.class);
    }
}
