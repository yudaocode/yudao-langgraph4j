package org.bsc.javelit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        try (ConfigurableApplicationContext boot =
                new SpringApplicationBuilder(Fixture.class)
                        .web(WebApplicationType.NONE)
                        .properties("spring.main.banner-mode=off", "logging.level.root=warn")
                        .run()) {
            assertEquals("2.7.18", SpringBootVersion.getVersion());
            assertEquals("1.8", System.getProperty("java.specification.version"));
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
