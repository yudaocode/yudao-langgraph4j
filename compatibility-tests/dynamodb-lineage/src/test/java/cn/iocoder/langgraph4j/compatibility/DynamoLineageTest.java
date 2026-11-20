package cn.iocoder.langgraph4j.compatibility;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.DynamoDBSaver;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Exercises checkpoint ancestry using installed jars and a standalone DynamoDB Local server.
 *
 * @author 芋道源码
 */
@Timeout(45)
class DynamoLineageTest {
    private static final RunnableConfig THREAD = RunnableConfig.builder().threadId("workflow").build();
    private final String table = "lg4j-lineage-" + UUID.randomUUID();

    @Test
    void aNewTurnContinuesFromThePreviousTurn() throws Exception {
        try (ConfigurableApplicationContext boot = startBoot(); DynamoDbClient client = client()) {
            CompiledGraph<AgentState> graph = graph(client, false);
            List<String> first = snapshots(graph, GraphInput.args(value("first")), THREAD);
            List<String> second = snapshots(graph, GraphInput.args(value("second")), THREAD);
            assertLinear(client, first, null);
            assertLinear(client, second, first.get(2));
            assertEquals("second:done", graph.getState(THREAD).state().value("mark").orElse(null));
        }
    }

    @Test
    void resumesFromThePersistedCheckpointWithANewClient() throws Exception {
        try (ConfigurableApplicationContext boot = startBoot()) {
            String interrupted;
            try (DynamoDbClient firstClient = client()) {
                CompiledGraph<AgentState> graph = graph(firstClient, true);
                List<String> first = snapshots(graph, GraphInput.args(value("wait")), THREAD);
                assertEquals(2, first.size());
                interrupted = first.get(1);
                assertEquals(first.get(0), parent(firstClient, interrupted));
            }
            try (DynamoDbClient replacement = client()) {
                CompiledGraph<AgentState> graph = graph(replacement, true);
                List<String> resumed = snapshots(graph, GraphInput.resume(value("resume")), THREAD);
                assertEquals(1, resumed.size());
                assertEquals(interrupted, parent(replacement, resumed.get(0)));
                assertEquals("resume:done", graph.getState(THREAD).state().value("mark").orElse(null));
            }
        }
    }

    @Test
    void forksFromAnOlderCheckpointWithoutReparentingTheOriginalBranch() throws Exception {
        try (ConfigurableApplicationContext boot = startBoot(); DynamoDbClient client = client()) {
            CompiledGraph<AgentState> graph = graph(client, false);
            List<String> original = snapshots(graph, GraphInput.args(value("original")), THREAD);
            String ancestor = original.get(1);
            List<String> branch = snapshots(graph, GraphInput.resume(value("fork")),
                    THREAD.withCheckPointId(ancestor));
            assertEquals(1, branch.size());
            assertNotEquals(original.get(2), branch.get(0));
            assertEquals(ancestor, parent(client, branch.get(0)));
            assertEquals(ancestor, parent(client, original.get(2)));
            assertEquals("original:done", graph.getState(THREAD.withCheckPointId(original.get(2)))
                    .state().value("mark").orElse(null));
            assertEquals("fork:done", graph.getState(THREAD.withCheckPointId(branch.get(0)))
                    .state().value("mark").orElse(null));
        }
    }

    private CompiledGraph<AgentState> graph(DynamoDbClient client, boolean interrupt) throws Exception {
        DynamoDBSaver saver = DynamoDBSaver.builder()
                .tableName(table)
                .createTableIfNotExists(true)
                .dynamoDbClient(client)
                .stateSerializer(new ObjectStreamStateSerializer<>(AgentState::new))
                .build();
        CompileConfig.Builder config = CompileConfig.builder().checkpointSaver(saver).releaseThread(false);
        if (interrupt) {
            config.interruptBefore("finish");
        }
        return new StateGraph<>(AgentState::new)
                .addNode("prepare", node_async(state -> Collections.singletonMap("prepared", true)))
                .addNode("finish", node_async(state -> Collections.singletonMap("mark",
                        state.value("label").orElseThrow(() -> new AssertionError("missing input")) + ":done")))
                .addEdge(START, "prepare").addEdge("prepare", "finish").addEdge("finish", END)
                .compile(config.build());
    }

    private static Map<String, Object> value(String label) {
        return Collections.singletonMap("label", label);
    }

    private static List<String> snapshots(CompiledGraph<AgentState> graph, GraphInput input, RunnableConfig config) {
        return graph.streamSnapshots(input, config).stream()
                .filter(StateSnapshot.class::isInstance)
                .map(output -> ((StateSnapshot<?>) output).config(config).checkPointId().get())
                .collect(Collectors.toList());
    }

    private void assertLinear(DynamoDbClient client, List<String> checkpoints, String firstParent) {
        assertEquals(3, checkpoints.size());
        assertEquals(firstParent, parent(client, checkpoints.get(0)));
        assertEquals(checkpoints.get(0), parent(client, checkpoints.get(1)));
        assertEquals(checkpoints.get(1), parent(client, checkpoints.get(2)));
    }

    private String parent(DynamoDbClient client, String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("PK", AttributeValue.builder().s("CHECKPOINT_workflow").build());
        key.put("SK", AttributeValue.builder().s(id).build());
        Map<String, AttributeValue> row = client.getItem(r -> r.tableName(table).key(key).consistentRead(true)).item();
        assertFalse(row.isEmpty(), "Missing checkpoint " + id);
        AttributeValue value = row.get("parentCheckpointId");
        return value == null ? null : value.s();
    }

    private static DynamoDbClient client() {
        return DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(required("lineage.endpoint")))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("fixture", "fixture")))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    private static ConfigurableApplicationContext startBoot() {
        assertEquals(required("lineage.boot"), SpringBootVersion.getVersion());
        assertEquals(required("lineage.jdk"), System.getProperty("java.specification.version"));
        return new SpringApplicationBuilder(Fixture.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off", "logging.level.root=warn")
                .run();
    }

    private static String required(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing " + property);
        }
        return value;
    }

    /** @author 芋道源码 */
    @Configuration(proxyBeanMethods = false)
    static class Fixture {}
}
