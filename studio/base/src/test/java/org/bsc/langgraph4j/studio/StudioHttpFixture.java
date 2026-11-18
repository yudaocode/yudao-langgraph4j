package org.bsc.langgraph4j.studio;

import com.agui.community.core.agent.RunAgentInput;
import com.agui.community.core.message.UserMessage;
import com.agui.json.AGUIJacksonSerializer;
import dev.langchain4j.internal.compat.Java8Platform;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.studio.agui.AGUILangGraphStudioAgent;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 芋道源码
 */
public final class StudioHttpFixture {

    private StudioHttpFixture() {
    }

    public static LangGraphStudioServer.Instance graph() throws Exception {
        return LangGraphStudioServer.Instance.builder()
                .title("Java8 Studio Echo")
                .addInputStringArg("input")
                .graph(echoGraph())
                .build();
    }

    public static AGUILangGraphStudioAgent agent() throws Exception {
        return AGUILangGraphStudioAgent.builder()
                .id("echo").title("Java8 AG-UI Studio Echo").stateGraph(echoGraph())
                .addInputStringArg("input", true, UserMessage::content).build();
    }

    private static StateGraph<AgentState> echoGraph() throws Exception {
        return new StateGraph<>(AgentState::new)
                .addNode("echo", node_async(state -> Collections.singletonMap("output",
                        "echo " + state.<String>value("input").orElseThrow(AssertionError::new))))
                .addEdge(START, "echo")
                .addEdge("echo", END);
    }

    public static void assertStudioHttp(int port) throws Exception {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        assertTrue(read(port, "/", null).toLowerCase(java.util.Locale.ROOT).contains("<html"));
        assertTrue(read(port, "/init?instance=echo", null).contains("Java8 Studio Echo"));
        String stream = read(port, "/stream/echo?thread=java8-studio", "{\"input\":\"芋道\"}");
        assertTrue(stream.contains("echo 芋道"), stream);
    }

    public static void assertAgUiStudioHttp(int port) throws Exception {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        assertTrue(read(port, "/", null).toLowerCase(java.util.Locale.ROOT).contains("<html"));
        assertTrue(read(port, "/init?agent=echo", null).contains("Java8 AG-UI Studio Echo"));
        RunAgentInput request = new RunAgentInput("ag-ui-studio-thread", "ag-ui-studio-run",
                Collections.singletonList(new UserMessage("input", "芋道")), Collections.emptyList());
        String stream = read(port, "/stream/echo", new AGUIJacksonSerializer().serialize(request));
        assertTrue(stream.contains("RUN_STARTED"), stream);
        assertTrue(stream.contains("echo 芋道"), stream);
        assertTrue(stream.contains("RUN_FINISHED"), stream);
    }

    private static String read(int port, String path, String request) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setConnectTimeout(3_000);
        connection.setReadTimeout(10_000);
        try {
            if (request != null) {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                try (java.io.OutputStream body = connection.getOutputStream()) {
                    body.write(request.getBytes(StandardCharsets.UTF_8));
                }
            }
            assertEquals(200, connection.getResponseCode(), path);
            try (InputStream body = connection.getInputStream()) {
                return new String(Java8Platform.readAllBytes(body), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
