package org.bsc.async;

import org.springframework.ai.compat.jdkhttp.Flow;
import dev.langchain4j.internal.compat.FlowAdapters;
import reactor.core.publisher.Sinks;

/**
 * Native8 publisher fixture replacing Java9 SubmissionPublisher, not a replacement public API.
 *
 * @author 芋道源码
 */
final class Java8TestPublisher<T> implements Flow.Publisher<T>, AutoCloseable {
    private final Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer();
    @Override public void subscribe(Flow.Subscriber<? super T> subscriber) {
        FlowAdapters.toFlowPublisher(sink.asFlux()).subscribe(subscriber);
    }
    boolean hasSubscribers() { return sink.currentSubscriberCount() > 0; }
    int submit(T item) {
        Sinks.EmitResult result = sink.tryEmitNext(item);
        if (result != Sinks.EmitResult.OK && result != Sinks.EmitResult.FAIL_CANCELLED
                && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) throw new IllegalStateException(result.toString());
        return 0;
    }
    @Override public void close() { sink.tryEmitComplete(); }
}
