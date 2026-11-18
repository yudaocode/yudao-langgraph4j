package com.agui.json;

import com.agui.community.client.HttpAgent;
import com.agui.community.client.HttpAgentException;
import com.agui.community.core.agent.RunAgentInput;
import com.agui.community.core.event.Event;
import com.agui.community.core.event.RunFinishedEvent;
import com.agui.community.core.event.RunStartedEvent;
import com.agui.community.core.event.TextMessageContentEvent;
import com.agui.community.server.jdk.JdkAgentHttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.compat.jdkhttp.FlowAdapters;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 芋道源码
 */
class AgUiHttpJava8Test {

    private final AGUIJacksonSerializer serializer = new AGUIJacksonSerializer();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void streamsOrderedEventsWithUtf8OverHttp() {
        AtomicReference<RunAgentInput> request = new AtomicReference<>();
        server.createContext("/agent", new JdkAgentHttpHandler(input -> {
            request.set(input);
            return FlowAdapters.publisherToFlowPublisher(Flux.just(
                    new RunStartedEvent(input.threadId(), input.runId()),
                    new TextMessageContentEvent("message-1", "芋道"),
                    new RunFinishedEvent(input.threadId(), input.runId())));
        }, serializer));

        List<Event> events = run("/agent");

        assertEquals("thread-1", request.get().threadId());
        assertEquals(3, events.size());
        assertTrue(events.get(0) instanceof RunStartedEvent);
        assertEquals("芋道", ((TextMessageContentEvent) events.get(1)).delta());
        assertTrue(events.get(2) instanceof RunFinishedEvent);
    }

    @Test
    void reportsHttpStatusAsRunFailure() {
        server.createContext("/missing", exchange -> {
            try {
                exchange.sendResponseHeaders(404, -1);
            } finally {
                exchange.close();
            }
        });

        HttpAgentException failure = assertThrows(HttpAgentException.class, () -> run("/missing"));
        assertTrue(failure.getMessage().contains("404"));
    }

    private List<Event> run(String path) {
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        HttpAgent client = new HttpAgent(endpoint, serializer);
        RunAgentInput input = new RunAgentInput("thread-1", "run-1", Collections.emptyList(), Collections.emptyList());
        return FlowAdapters.flowPublisherToFlux(client.run(input)).collectList().block(Duration.ofSeconds(10));
    }
}
