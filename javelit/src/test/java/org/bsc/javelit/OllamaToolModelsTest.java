package org.bsc.javelit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author 芋道源码
 */
class OllamaToolModelsTest {

    @Test
    void discoversOnlyModelsWithToolsUsingNativeOllamaEndpoints() throws IOException {
        HttpServer fixture = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fixture.createContext("/api/tags", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            reply(exchange, 200, "{\"models\":[{\"name\":\"工具模型\"},{\"name\":\"text\"},{\"name\":\"removed\"}]}");
        });
        fixture.createContext("/api/show", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            String name = new ObjectMapper().readTree(exchange.getRequestBody()).path("model").asText();
            if ("removed".equals(name)) {
                reply(exchange, 404, "{\"error\":\"not found\"}");
            } else {
                reply(exchange, 200, "工具模型".equals(name)
                        ? "{\"capabilities\":[\"completion\",\"tools\"]}"
                        : "{\"capabilities\":[\"completion\"]}");
            }
        });
        fixture.start();
        try {
            String endpoint = "http://127.0.0.1:" + fixture.getAddress().getPort();
            assertEquals(Collections.singletonList(new JtSelectAiModel.Model(
                    JtSelectAiModel.Model.Provider.OLLAMA, "工具模型")),
                    new OllamaToolModels(new URL(endpoint + "/")).list());
            assertThrows(IOException.class, () -> new OllamaToolModels(new URL(endpoint + "/missing/")).list());
        } finally {
            fixture.stop(0);
        }
    }

    private static void reply(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(body);
        }
    }
}
