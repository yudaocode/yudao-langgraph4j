package cn.iocoder.langgraph4j.agui;

import com.agui.community.client.HttpAgent;
import com.agui.community.core.agent.RunAgentInput;
import com.agui.community.core.event.Event;
import com.agui.community.core.event.RunFinishedEvent;
import com.agui.community.core.event.RunStartedEvent;
import com.agui.community.core.event.TextMessageContentEvent;
import com.agui.json.AGUIJacksonSerializer;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.agui.sdk.AGUIAgentBase;
import org.bsc.langgraph4j.agui.sdk.AGUIAgentRegistry;
import org.bsc.langgraph4j.agui.sdk.AGUISSEController;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.compat.jdkhttp.FlowAdapters;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 芋道源码
 */
class AgUiBoot2RuntimeTest {

    @Test
    void runsGraphOverBoot2Sse() {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .properties("server.port=0", "spring.main.banner-mode=off", "server.address=127.0.0.1").run()) {
            String port = context.getEnvironment().getRequiredProperty("local.server.port");
            HttpAgent client = new HttpAgent(URI.create("http://127.0.0.1:" + port + "/sse/echo"),
                    new AGUIJacksonSerializer());
            RunAgentInput input = new RunAgentInput("compat-thread", "compat-run",
                    Collections.emptyList(), Collections.emptyList());

            List<Event> events = FlowAdapters.flowPublisherToFlux(client.run(input))
                    .collectList().block(Duration.ofSeconds(10));

            assertEquals(3, events.size());
            assertTrue(events.get(0) instanceof RunStartedEvent);
            assertEquals("回声 compat-thread", ((TextMessageContentEvent) events.get(1)).delta());
            assertEquals("compat-run", ((RunFinishedEvent) events.get(2)).runId());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {

        @Bean
        AGUISSEController controller() {
            return new AGUISSEController(new AGUIAgentRegistry(new EchoAgent()), new AGUIJacksonSerializer());
        }
    }

    private static final class EchoAgent extends AGUIAgentBase {

        EchoAgent() {
            super("echo");
        }

        @Override
        protected CompiledGraph<AgentState> newGraph() throws Exception {
            return new StateGraph<>(AgentState::new)
                    .addNode("echo", node_async(state -> Collections.singletonMap("output",
                            "回声 " + state.<String>value("thread").orElseThrow(AssertionError::new))))
                    .addEdge(START, "echo")
                    .addEdge("echo", END)
                    .compile();
        }

        @Override
        protected GraphInput graphInput(RunAgentInput input) {
            return GraphInput.args(Collections.singletonMap("thread", input.threadId()));
        }

        @Override
        protected Collection<? extends Event> onNextEvents(RunAgentInput input, NodeOutput<? extends AgentState> output) {
            if ("echo".equals(output.node())) {
                return Collections.singletonList(new TextMessageContentEvent("echo-message",
                        output.state().<String>value("output").orElseThrow(AssertionError::new)));
            }
            return Collections.emptyList();
        }
    }
}
