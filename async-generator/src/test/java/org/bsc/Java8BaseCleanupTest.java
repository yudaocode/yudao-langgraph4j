package org.bsc;

import org.bsc.async.AsyncGenerator;
import org.junit.jupiter.api.Test;
import java.util.concurrent.ExecutorService;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 芋道源码
 */
class Java8BaseCleanupTest {
    @Test void explicitCloseShutsDownTheOwnedExecutorExactlyOnce() {
        AsyncGenerator.Base<String> generator = new AsyncGenerator.Base<String>() {
            @Override public AsyncGenerator.Data<String> next() { return AsyncGenerator.Data.done(); }
        };
        ExecutorService executor = (ExecutorService) generator.executor();
        assertFalse(executor.isShutdown());
        generator.close();
        generator.close();
        assertTrue(generator.isClosed());
        assertTrue(executor.isShutdown());
        assertThrows(IllegalStateException.class, generator::executor);
    }
}
