package org.bsc.langgraph4j.checkpoint;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPMap;
import com.hazelcast.cp.CPSubsystem;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the saver against the public Hazelcast CPMap API with an in-memory test double.
 *
 * @author 芋道源码
 */
class HazelcastCPMapContractTest extends AbstractCheckpointSaverTest {

    private final Map<String, CPMap<?, ?>> maps = new ConcurrentHashMap<>();

    private final CPSubsystem cpSubsystem = proxy(CPSubsystem.class, (instance, method, args) -> {
        if (method.getName().equals("getMap")) {
            return maps.computeIfAbsent((String) args[0], name -> newMap());
        }
        throw new UnsupportedOperationException(method.getName());
    });

    private final HazelcastInstance member = proxy(HazelcastInstance.class, (instance, method, args) -> {
        if (method.getName().equals("getCPSubsystem")) {
            return cpSubsystem;
        }
        throw new UnsupportedOperationException(method.getName());
    });

    @Override
    protected HazelcastSaver buildCheckpointSaver(
            StateSerializer<? extends AgentState> serializer, @Nullable String threadId) {
        return HazelcastSaver.builder()
                .hazelcastInstance(member)
                .mapType(HazelcastSaver.MapType.CP_MAP)
                .mapName(threadId == null ? "checkpoints" : "checkpoints-" + threadId)
                .stateSerializer(serializer)
                .build();
    }

    @ParameterizedTest
    @EnumSource(StateSerializerEnum.class)
    void reopensCheckpointAndReleasesOnlyItsThread(StateSerializerEnum serializer) throws Exception {
        var writer = buildCheckpointSaver(serializer.stateSerializer, null);
        var config = RunnableConfig.builder().threadId("active").build();
        var other = RunnableConfig.builder().threadId("other").build();

        writer.put(config, Checkpoint.builder().nodeId("node").nextNodeId("next").state(Map.of("step", 1)).build());
        writer.put(other, Checkpoint.builder().nodeId("node").nextNodeId("next").state(Map.of("step", 2)).build());

        var reader = buildCheckpointSaver(serializer.stateSerializer, null);
        assertEquals(Map.of("step", 1), reader.get(config).orElseThrow().state());
        reader.release(config);

        assertTrue(writer.get(config).isEmpty());
        assertEquals(Map.of("step", 2), writer.get(other).orElseThrow().state());
    }

    private static CPMap<?, ?> newMap() {
        Map<String, String> entries = new ConcurrentHashMap<>();
        return proxy(CPMap.class, (instance, method, args) -> switch (method.getName()) {
            case "get" -> entries.get(args[0]);
            case "set" -> {
                entries.put((String) args[0], (String) args[1]);
                yield null;
            }
            case "delete" -> {
                entries.remove(args[0]);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
