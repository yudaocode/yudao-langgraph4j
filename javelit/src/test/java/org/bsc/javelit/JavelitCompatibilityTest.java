package org.bsc.javelit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javelit.core.Jt;
import io.javelit.core.Server;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Verifies genuine Javelit HTTP and WebSocket rendering for the original LangGraph4j components.
 *
 * @author 芋道源码
 */
class JavelitCompatibilityTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern COMPONENT_KEY = Pattern.compile("<jt-text-input[^>]*component-key=\"([^\"]+)\"");

    @Test
    @Timeout(20)
    void servesPageAndRerendersAfterComponentInput() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        Server server = Server.builder(() -> {
            String input = Jt.textInput("用户输入").key("message").use();
            Jt.markdown("**芋道" + input + "**").use();
            JtMultiSelect.builder().items(Collections.singleton("中文选项")).use();
        }, port).host("127.0.0.1").build();
        server.start();
        try {
            HttpURLConnection page = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/").openConnection();
            page.setConnectTimeout(3000);
            page.setReadTimeout(3000);
            String cookie;
            try {
                assertEquals(200, page.getResponseCode());
                try (InputStream input = page.getInputStream()) {
                    byte[] buffer = new byte[256];
                    int read = input.read(buffer);
                    assertTrue(new String(buffer, 0, read, StandardCharsets.UTF_8).contains("<!DOCTYPE html>"));
                }
                cookie = page.getHeaderFields().entrySet().stream()
                        .filter(header -> "Set-Cookie".equalsIgnoreCase(header.getKey()))
                        .flatMap(header -> header.getValue().stream())
                        .map(value -> value.split(";", 2)[0])
                        .collect(Collectors.joining("; "));
            } finally {
                page.disconnect();
            }
            Connection connection = new Connection(port, Collections.singletonMap("Cookie", cookie));
            try {
                assertTrue(connection.connectBlocking(3, TimeUnit.SECONDS));
                assertEquals("session_init", connection.nextEvent().path("type").asText());
                connection.send("{\"type\":\"path_update\",\"path\":\"/\",\"queryParameters\":{}}");
                String first = connection.render();
                assertTrue(first.contains("<jt-multi-select") && first.contains("中文选项"), first);
                Matcher key = COMPONENT_KEY.matcher(first);
                assertTrue(key.find(), first);
                String update = "{\"type\":\"component_update\",\"componentKey\":"
                        + JSON.writeValueAsString(key.group(1)) + ",\"value\":\"\\u4f60\\u597d\"}";
                connection.send(update);
                String updated = connection.render();
                assertTrue(updated.contains("&lt;strong&gt;芋道你好&lt;/strong&gt;"), updated);
            } finally {
                connection.closeBlocking();
            }
        } finally {
            server.stop();
        }
    }

    /**
     * @author 芋道源码
     */
    private static final class Connection extends WebSocketClient {
        private final BlockingQueue<String> events = new LinkedBlockingQueue<>();

        Connection(int port, Map<String, String> headers) {
            super(URI.create("ws://127.0.0.1:" + port + "/_/ws"), headers);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {}

        @Override
        public void onMessage(String message) {
            events.add(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {}

        @Override
        public void onError(Exception failure) {
            events.add(JSON.createObjectNode().put("type", "error").put("message", failure.toString()).toString());
        }

        JsonNode nextEvent() throws Exception {
            String message = events.poll(3, TimeUnit.SECONDS);
            assertTrue(message != null, "Timed out waiting for a Javelit websocket event");
            JsonNode event = JSON.readTree(message);
            assertTrue(!"error".equals(event.path("type").asText()), message);
            return event;
        }

        String render() throws Exception {
            StringBuilder page = new StringBuilder();
            for (int received = 0; received < 30; received++) {
                JsonNode event = nextEvent();
                String type = event.path("type").asText();
                if ("delta".equals(type)) {
                    page.append(event.path("html").asText());
                } else if ("status".equals(type) && "END".equals(event.path("status").asText())) {
                    return page.toString();
                }
            }
            throw new AssertionError("Javelit render did not complete");
        }
    }
}
