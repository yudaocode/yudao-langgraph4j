package cn.iocoder.langgraph4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.langchain4j.serializer.jackson.LC4jJacksonStateSerializer;
import org.bsc.langgraph4j.spring.ai.serializer.jackson.SpringAIJacksonStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Same Java source verifies each Boot/JDK combination against local provider protocol fixtures.
 * @author 芋道源码
 */
@Timeout(30)
class GraphCompatibilityTest {
    private MockWebServer server;
    private ConfigurableApplicationContext context;

    @BeforeEach void start() throws Exception {
        assertEquals(1, Collections.list(StateGraph.class.getClassLoader()
                .getResources("org/bsc/langgraph4j/StateGraph.class")).size(),
                "Spring AI and LangChain4j adapters must share one LangGraph4j core");
        assertEquals(System.getProperty("compat.expected.java"), System.getProperty("java.specification.version"));
        server = new MockWebServer();
        server.start();
        context = new SpringApplicationBuilder(App.class).web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off", "logging.level.root=WARN",
                        "compat.provider.url=" + server.url("/").toString()).run();
    }
    @AfterEach void close() throws Exception {
        try { if (context != null) context.close(); }
        finally { if (server != null) server.close(); }
    }
    private void answer(String value) {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(
                "{\"id\":\"local\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"mock\","
                + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"" + value
                + "\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":1,\"total_tokens\":3}}"));
    }

    @Test void springAiModelRunsThroughGraphAndCheckpointSerializer() throws Exception {
        answer("spring-ok");
        org.springframework.ai.chat.model.ChatModel model = context.getBean(org.springframework.ai.chat.model.ChatModel.class);
        SpringAIJacksonStateSerializer<DemoState> serializer = new SpringAIJacksonStateSerializer<>(DemoState::new);
        StateGraph<DemoState> definition = new StateGraph<>(serializer);
        definition.addNode("model", node_async(state -> {
            String output = model.call(state.<String>value("prompt").orElseThrow(AssertionError::new));
            Map<String, Object> updates = new HashMap<>();
            updates.put("answer", output);
            updates.put("history", Arrays.asList(
                    org.springframework.ai.chat.messages.UserMessage.builder().text("hello").build(),
                    org.springframework.ai.chat.messages.AssistantMessage.builder().content(output).build()));
            return updates;
        })).addEdge(START, "model").addEdge("model", END);
        CompiledGraph<DemoState> app = definition.compile(CompileConfig.builder().checkpointSaver(new MemorySaver()).releaseThread(false).build());
        RunnableConfig config = RunnableConfig.builder().threadId("spring-ai-local").build();
        DemoState result = app.invoke(Collections.singletonMap("prompt", "hello"), config).orElseThrow(AssertionError::new);
        assertEquals("spring-ok", result.<String>value("answer").orElseThrow(AssertionError::new));
        List<?> history = app.getState(config).state().<List<?>>value("history").orElseThrow(AssertionError::new);
        assertEquals(2, history.size());
        assertTrue(history.get(0) instanceof org.springframework.ai.chat.messages.UserMessage);
        assertTrue(history.get(1) instanceof org.springframework.ai.chat.messages.AssistantMessage);
        assertTrue(server.takeRequest().getPath().endsWith("/chat/completions"));
    }

    @Test void langchain4jModelRunsThroughGraphAndCheckpointSerializer() throws Exception {
        answer("lc-ok");
        OpenAiChatModel model = context.getBean(OpenAiChatModel.class);
        LC4jJacksonStateSerializer<DemoState> serializer = new LC4jJacksonStateSerializer<>(DemoState::new);
        StateGraph<DemoState> definition = new StateGraph<>(serializer);
        definition.addNode("model", node_async(state -> {
            String output = model.chat(state.<String>value("prompt").orElseThrow(AssertionError::new));
            Map<String, Object> updates = new HashMap<>();
            updates.put("answer", output);
            updates.put("history", Arrays.asList(dev.langchain4j.data.message.UserMessage.from("hello"),
                    dev.langchain4j.data.message.AiMessage.from(output)));
            return updates;
        })).addEdge(START, "model").addEdge("model", END);
        CompiledGraph<DemoState> app = definition.compile(CompileConfig.builder().checkpointSaver(new MemorySaver()).releaseThread(false).build());
        RunnableConfig config = RunnableConfig.builder().threadId("langchain4j-local").build();
        DemoState result = app.invoke(Collections.singletonMap("prompt", "hello"), config).orElseThrow(AssertionError::new);
        assertEquals("lc-ok", result.<String>value("answer").orElseThrow(AssertionError::new));
        List<?> history = app.getState(config).state().<List<?>>value("history").orElseThrow(AssertionError::new);
        assertEquals(2, history.size());
        assertTrue(history.get(0) instanceof dev.langchain4j.data.message.UserMessage);
        assertTrue(history.get(1) instanceof dev.langchain4j.data.message.AiMessage);
        assertTrue(server.takeRequest().getPath().endsWith("/chat/completions"));
    }

    /**

     * @author 芋道源码

     */
    public static class DemoState extends AgentState {
        public DemoState(Map<String, Object> data) { super(data); }
    }

    /**

     * @author 芋道源码

     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
        @Bean(destroyMethod = "close") JdkHttpClient lcHttpClient() { return JdkHttpClient.builder().build(); }
        @Bean OpenAiChatModel langchainModel(JdkHttpClient client, Environment environment) {
            // Reuse the explicit test-owned transport so the application context can close it deterministically.
            JdkHttpClientBuilder builder = new JdkHttpClientBuilder() {
                @Override public JdkHttpClient build() { return client; }
            };
            return OpenAiChatModel.builder().httpClientBuilder(builder).baseUrl(environment.getRequiredProperty("compat.provider.url") + "v1")
                    .apiKey("local-protocol-fixture").modelName("mock").maxRetries(0).build();
        }
        @Bean org.springframework.ai.openai.OpenAiChatModel springModel(Environment environment) {
            return org.springframework.ai.openai.OpenAiChatModel.builder()
                    .options(org.springframework.ai.openai.OpenAiChatOptions.builder()
                            .baseUrl(environment.getRequiredProperty("compat.provider.url") + "v1")
                            .apiKey("local-protocol-fixture").model("mock").maxRetries(0)
                            .timeout(java.time.Duration.ofSeconds(5)).build()).build();
        }
    }
}
