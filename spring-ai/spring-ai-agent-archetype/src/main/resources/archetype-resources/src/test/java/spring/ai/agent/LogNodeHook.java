#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agent;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.hook.NodeHook;
import ${package}.spring.ai.agentexecutor.AgentExecutorEx;
import org.bsc.langgraph4j.utils.CollectionsUtils;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import static java.util.concurrent.CompletableFuture.completedFuture;

@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY, getterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, isGetterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, setterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
@dev.langchain4j.internal.compat.Java8Record({ "consumer" })
public final class LogNodeHook extends dev.langchain4j.internal.compat.Java8RecordBase implements NodeHook.BeforeCall<AgentExecutorEx.State>, NodeHook.AfterCall<AgentExecutorEx.State> {

    public NodeHook.BeforeCall<AgentExecutorEx.State> asBeforeCall() {
        return this;
    }

    public NodeHook.AfterCall<AgentExecutorEx.State> asAfterCall() {
        return this;
    }

    @Override
    public CompletableFuture<Map<String, Object>> applyBefore(String nodeId, AgentExecutorEx.State state, RunnableConfig config) {
        consumer.accept(java.lang.String.format("""
            ${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} %s `STARTED`
            ```
            %s
            ```
            """, config.nodePath(), state.messages().stream().map(Object::toString).collect(Collectors.joining("\n\n"))));
        return completedFuture(dev.langchain4j.internal.compat.Java8Collections.mapOf());
    }

    @Override
    public CompletableFuture<Map<String, Object>> applyAfter(String nodeId, AgentExecutorEx.State state, RunnableConfig config, Map<String, Object> partialResult) {
        consumer.accept(java.lang.String.format("""
            ${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} %s `FINISHED`
            ```
            %s
            ```
            """, config.nodePath(), CollectionsUtils.toString(partialResult)));
        return completedFuture(partialResult);
    }

    private final Consumer<String> consumer;

    public Consumer<String> consumer() {
        return this.consumer;
    }

    @com.fasterxml.jackson.annotation.JsonCreator()
    public LogNodeHook(@com.fasterxml.jackson.annotation.JsonProperty("consumer") Consumer<String> consumer) {
        this.consumer = consumer;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        LogNodeHook that = (LogNodeHook) other;
        return java.util.Objects.equals(this.consumer, that.consumer);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.consumer);
        return result;
    }

    @Override
    public String toString() {
        return "LogNodeHook[consumer=" + java.lang.String.valueOf((Object) this.consumer) + "]";
    }
}
