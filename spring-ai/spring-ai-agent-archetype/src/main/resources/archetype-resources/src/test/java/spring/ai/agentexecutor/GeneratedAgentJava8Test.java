package ${package}.spring.ai.agentexecutor;

import java.util.Collections;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 芋道源码
 */
class GeneratedAgentJava8Test {

    @Test
    void runsGeneratedSpringAiAgent() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE).properties("spring.main.banner-mode=off")
                .run("--spring.main.web-application-type=none")) {
            CompiledGraph<AgentExecutor.State> graph = AgentExecutor.builder()
                    .chatModel(context.getBean(ChatModel.class)).build().compile();
            AgentExecutor.State result = graph.invoke(
                    GraphInput.args(Collections.singletonMap("messages", new UserMessage("hello"))),
                    RunnableConfig.empty()).orElseThrow(AssertionError::new);
            assertEquals("芋道-boot2", result.lastMessage().map(AssistantMessage.class::cast)
                    .map(AssistantMessage::getText).orElseThrow(AssertionError::new));
        }
    }

    @SpringBootConfiguration
    static class Application {

        @Bean
        ChatModel chatModel() {
            return prompt -> new ChatResponse(Collections.singletonList(
                    new Generation(new AssistantMessage("芋道-boot2"))));
        }
    }
}
