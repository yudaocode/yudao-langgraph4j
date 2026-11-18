package org.bsc.javelit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses Ollama's native catalog and model-detail APIs to find models with tool calling.
 *
 * @author 芋道源码
 */
final class OllamaToolModels {

    private static final int TIMEOUT_MILLIS = 3000;
    private final URL endpoint;
    private final ObjectMapper mapper = new ObjectMapper();

    OllamaToolModels(URL endpoint) {
        this.endpoint = endpoint;
    }

    List<JtSelectAiModel.Model> list() throws IOException {
        List<JtSelectAiModel.Model> models = new ArrayList<>();
        for (JsonNode item : request("api/tags", null).path("models")) {
            String name = item.path("name").asText();
            if (name.isEmpty()) {
                continue;
            }
            try {
                JsonNode model = request("api/show", mapper.createObjectNode().put("model", name));
                for (JsonNode capability : model.path("capabilities")) {
                    if ("tools".equalsIgnoreCase(capability.asText())) {
                        models.add(new JtSelectAiModel.Model(JtSelectAiModel.Model.Provider.OLLAMA, name));
                        break;
                    }
                }
            } catch (IOException unavailableModel) {
                // The model may have been removed since the catalog request.
            }
        }
        return models;
    }

    private JsonNode request(String path, JsonNode input) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint, path).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        try {
            if (input != null) {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                byte[] payload = mapper.writeValueAsBytes(input);
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                }
            }
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Ollama " + path + " returned HTTP " + status);
            }
            try (InputStream body = connection.getInputStream()) {
                return mapper.readTree(body);
            }
        } finally {
            connection.disconnect();
        }
    }
}
