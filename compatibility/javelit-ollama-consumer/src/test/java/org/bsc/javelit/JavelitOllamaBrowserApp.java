package org.bsc.javelit;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatRequestParameters;
import io.javelit.core.Jt;
import io.javelit.core.Server;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

/**
 * Runs the embedded Javelit and local Ollama workflow in an independent Boot 2 consumer.
 *
 * @author 芋道源码
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class JavelitOllamaBrowserApp {

    public static void main(String[] args) {
        String boot = SpringBootVersion.getVersion();
        String jdk = System.getProperty("java.version");
        if (!"2.7.18".equals(boot) || !"1.8".equals(System.getProperty("java.specification.version"))) {
            throw new IllegalStateException("Expected Spring Boot 2.7.18 and JDK 8, got " + boot + " / " + jdk);
        }
        new SpringApplicationBuilder(JavelitOllamaBrowserApp.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server javelitServer() {
        int port = Integer.getInteger("javelit.browser.port", 18501);
        return Server.builder(JavelitOllamaBrowserApp::render, port).host("127.0.0.1").build();
    }

    private static void render() {
        Jt.title("Javelit Ollama tools").use();
        Optional<JtSelectAiModel.Model> selected = JtSelectAiModel.get();
        if (!selected.isPresent() || selected.get().provider() != JtSelectAiModel.Model.Provider.OLLAMA) {
            return;
        }
        String modelName = selected.get().name();
        Jt.text("Selected model: " + modelName).use();
        String prompt = Jt.textArea("Message").key("ollama-message").placeholder("Enter a message").use();
        JtSessionValue<String> toolStatus = new JtSessionValue<>("tool-status");
        JtSessionValue<String> answer = new JtSessionValue<>("final-answer");
        if (Jt.button("Send").key("send-ollama-message").disabled(prompt.trim().isEmpty()).use()) {
            toolStatus.clear();
            answer.clear();
            try {
                CodeTool tool = new CodeTool();
                String response = runGraph("http://127.0.0.1:11434", modelName, prompt, tool);
                toolStatus.setValue("get_code executed " + tool.calls() + " time(s)");
                answer.setValue(response);
            } catch (Exception failure) {
                Jt.error(failure.toString()).use();
                return;
            }
        }
        toolStatus.value().ifPresent(value -> Jt.text(value).use());
        answer.value().ifPresent(value -> {
            Jt.markdown("### Final answer").use();
            Jt.text(value).use();
        });
    }

    static String runGraph(String baseUrl, String modelName, String prompt, CodeTool tool) throws Exception {
        OllamaChatModel model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .defaultRequestParameters(OllamaChatRequestParameters.builder()
                        .modelName(modelName)
                        .think(false)
                        .temperature(0.0)
                        .numThread(2)
                        .maxOutputTokens(128)
                        .build())
                .timeout(Duration.ofSeconds(40))
                .maxRetries(0)
                .build();
        return AgentExecutor.builder()
                .chatModel(model)
                .toolsFromObject(tool)
                .build()
                .compile(CompileConfig.builder().recursionLimit(8).build())
                .invoke(Collections.singletonMap("messages", UserMessage.from(prompt)))
                .flatMap(AgentExecutor.State::finalResponse)
                .orElseThrow(() -> new IllegalStateException("The graph returned no final answer"));
    }

    /**
     * Supplies a value the model only receives after the graph executes the tool.
     *
     * @author 芋道源码
     */
    public static final class CodeTool {
        private final AtomicInteger calls = new AtomicInteger();

        int calls() {
            return calls.get();
        }

        @Tool(name = "get_code", value = "Returns the current code. Call this tool to get it.")
        public String getCode() {
            calls.incrementAndGet();
            return "RAIN42";
        }
    }
}
