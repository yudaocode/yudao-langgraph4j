package org.bsc.javelit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatRequestParameters;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Checks the installed Javelit model catalog against a real Ollama service.
 *
 * @author 芋道源码
 */
class JavelitOllamaConsumerTest {

    @Test
    void modelChooserIncludesToolModelsAndExcludesEmbeddingModels() throws Exception {
        URL endpoint = new URL(required("javelit.ollama.endpoint"));
        String toolModel = required("javelit.ollama.tool-model");
        String embeddingModel = required("javelit.ollama.embedding-model");
        try (ConfigurableApplicationContext boot = startBoot()) {
            Set<String> installed = new HashSet<>();
            try (InputStream body = new URL(endpoint, "api/tags").openStream()) {
                for (JsonNode model : new ObjectMapper().readTree(body).path("models")) {
                    installed.add(model.path("name").asText());
                }
            }
            assertTrue(installed.contains(toolModel), installed.toString());
            assertTrue(installed.contains(embeddingModel), installed.toString());
            List<JtSelectAiModel.Model> models = new OllamaToolModels(endpoint).list();
            assertTrue(
                    models.stream().anyMatch(model -> toolModel.equals(model.name())),
                    models.toString());
            assertFalse(
                    models.stream().anyMatch(model -> embeddingModel.equals(model.name())),
                    models.toString());
        }
    }

    @Test
    void selectedModelExecutesAToolAndAnswersWithItsResult() throws Exception {
        URL endpoint = new URL(required("javelit.ollama.endpoint"));
        String toolModel = required("javelit.ollama.tool-model");
        try (ConfigurableApplicationContext boot = startBoot()) {
            JtSelectAiModel.Model selected = new OllamaToolModels(endpoint).list().stream()
                    .filter(model -> toolModel.equals(model.name()))
                    .findFirst().orElseThrow(() -> new AssertionError("Tool model missing: " + toolModel));
            OllamaChatModel model = OllamaChatModel.builder()
                    .baseUrl(endpoint.toExternalForm())
                    .defaultRequestParameters(OllamaChatRequestParameters.builder()
                            .modelName(selected.name())
                            .think(false)
                            .temperature(0.0)
                            .numThread(2)
                            .maxOutputTokens(128)
                            .build())
                    .timeout(Duration.ofSeconds(40))
                    .maxRetries(0)
                    .build();
            CodeTool tool = new CodeTool();
            AgentExecutor.State state = AgentExecutor.builder()
                    .chatModel(model)
                    .toolsFromObject(tool)
                    .build()
                    .compile(CompileConfig.builder().recursionLimit(8).build())
                    .invoke(Collections.singletonMap(
                            "messages", UserMessage.from("Call get_code to find the code, then report that code.")))
                    .orElseThrow(AssertionError::new);
            assertEquals(1, tool.calls.get());
            String answer = state.finalResponse().orElseThrow(AssertionError::new);
            assertTrue(answer.contains("RAIN42"), answer);
        }
    }

    private static ConfigurableApplicationContext startBoot() {
        assertEquals("2.7.18", SpringBootVersion.getVersion());
        assertEquals("1.8", System.getProperty("java.specification.version"));
        return new SpringApplicationBuilder(Fixture.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off", "logging.level.root=warn")
                .run();
    }

    /**
     * @author 芋道源码
     */
    public static class CodeTool {

        private final AtomicInteger calls = new AtomicInteger();

        @Tool(name = "get_code", value = "Returns the current code. Call this tool to get it.")
        public String getCode() {
            calls.incrementAndGet();
            return "RAIN42";
        }
    }

    private static String required(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing fixture property " + property);
        }
        return value;
    }

    /**
     * @author 芋道源码
     */
    @Configuration(proxyBeanMethods = false)
    public static class Fixture {}
}
