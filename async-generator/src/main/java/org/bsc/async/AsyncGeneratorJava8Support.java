package org.bsc.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Java 8 implementation of private interface recursion.
 *
 * @author 芋道源码
 */
final class AsyncGeneratorJava8Support {
    private AsyncGeneratorJava8Support() {}
    static <E> CompletableFuture<Object> forEach(AsyncGenerator<E> self, Consumer<E> consumer) {
        AsyncGenerator.Data<E> next = self.next();
        if (next.isDone()) return completedFuture(next.resultValue());
        if (next.embed() != null) {
            return forEach(next.embed().generator(), consumer).thenCompose(value -> forEach(self, consumer));
        }
        return next.future().thenAccept(consumer).thenCompose(value -> forEach(self, consumer));
    }
    static <E, R> CompletableFuture<AsyncGenerator.ReduceResult<R>> reduce(
            AsyncGenerator<E> self, R result, BiFunction<R, E, R> reducer) {
        AsyncGenerator.Data<E> next = self.next();
        if (next.isDone()) return completedFuture(new AsyncGenerator.ReduceResult<>(result, next.resultValue()));
        return next.future().thenApply(value -> new AsyncGenerator.ReduceResult<>(reducer.apply(result, value), null))
                .thenCompose(value -> reduce(self, value.result(), reducer));
    }
    static <E> CompletableFuture<Object> finish(AsyncGenerator<E> self) {
        AsyncGenerator.Data<E> next = self.next();
        if (next.isDone()) return completedFuture(next.resultValue());
        if (next.embed() != null) return finish(next.embed().generator()).thenCompose(value -> finish(self));
        return next.future().thenCompose(value -> finish(self));
    }
}
