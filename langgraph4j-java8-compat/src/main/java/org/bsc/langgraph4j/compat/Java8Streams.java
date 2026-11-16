package org.bsc.langgraph4j.compat;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lazy predicate-bounded stream iteration for Java 8.
 *
 * @author 芋道源码
 */
public final class Java8Streams {
    private Java8Streams() {}
    public static <T> Stream<T> iterate(T seed, Predicate<? super T> predicate, UnaryOperator<T> next) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(next);
        Iterator<T> iterator = new Iterator<T>() {
            private T value = seed;
            private boolean first = true;
            private boolean ready;
            private boolean available;
            @Override public boolean hasNext() {
                if (!ready) {
                    if (first) first = false;
                    else value = next.apply(value);
                    available = predicate.test(value);
                    ready = true;
                }
                return available;
            }
            @Override public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                ready = false;
                return value;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }
}
