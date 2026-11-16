package org.bsc.langgraph4j.compat;

import dev.langchain4j.internal.compat.Java8Platform;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Objects;

/**
 * Java 8 phantom-reference cleanup used by the async generator. Actions must not retain their referents.
 * One daemon drains registered actions; it exits when idle so an unused application loader can be released.
 * @author 芋道源码
 */
public final class Cleaner {
    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();
    private static final Set<Cleanup> LIVE = ConcurrentHashMap.newKeySet();
    private static final Object WORKER_LOCK = new Object();
    private static Thread worker;
    private static volatile Object registrationFence;
    private Cleaner() {}
    public static Cleaner create() { return new Cleaner(); }

    /**

     * @author 芋道源码

     */
    public interface Cleanable { void clean(); }

    public Cleanable register(Object referent, Runnable action) {
        Cleanup cleanup = new Cleanup(Objects.requireNonNull(referent), Objects.requireNonNull(action));
        LIVE.add(cleanup);
        synchronized (WORKER_LOCK) {
            if (worker == null) {
                worker = Java8Platform.newThreadWithoutInheritedLocals(Cleaner::drain, "langgraph4j-java8-cleaner");
                worker.setDaemon(true);
                worker.setContextClassLoader(null);
                worker.start();
            }
        }
        // Keep the referent alive until registration is visible to the queue worker.
        // A volatile store provides an observable last use on Java 8 (before reachabilityFence exists).
        registrationFence = referent;
        registrationFence = null;
        return cleanup;
    }
    private static void drain() {
        for (;;) {
            try {
                Cleanup cleanup = (Cleanup) QUEUE.remove(1000);
                if (cleanup != null) {
                    try { cleanup.clean(); } catch (Throwable ignored) { /* other registered actions must still run */ }
                }
            } catch (InterruptedException ignored) { /* no public interruption API */ }
            synchronized (WORKER_LOCK) {
                if (LIVE.isEmpty()) { worker = null; return; }
            }
        }
    }

    /**

     * @author 芋道源码

     */
    private static final class Cleanup extends PhantomReference<Object> implements Cleanable {
        private final AtomicBoolean cleaned = new AtomicBoolean();
        private Runnable action;
        private Cleanup(Object referent, Runnable action) { super(referent, QUEUE); this.action = action; }
        @Override public void clean() {
            if (!cleaned.compareAndSet(false, true)) return;
            LIVE.remove(this);
            clear();
            Runnable run = action;
            action = null;
            run.run();
        }
    }
}
