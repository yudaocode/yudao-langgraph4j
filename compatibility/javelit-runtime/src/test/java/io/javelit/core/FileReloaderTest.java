package io.javelit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author 芋道源码
 */
class FileReloaderTest {

    @Test
    void compilesAndReloadsApplicationFiles(@TempDir Path temp) throws Exception {
        Path app = temp.resolve("CompatibilityApp.java");
        writeApp(app, "芋道", "ready");
        FileReloader reloader = new FileReloader(Server.builder(app, 0)
                .buildSystem(BuildSystem.RUNTIME));
        try {
            reloader.reload().runnable().run();
            assertEquals("芋道ready", System.getProperty("javelit.compatibility.result"));
            writeApp(app, "芋道", "reload");
            reloader.reload().runnable().run();
            assertEquals("芋道reload", System.getProperty("javelit.compatibility.result"));
            Files.write(app, "not a valid Java source".getBytes(StandardCharsets.UTF_8));
            assertThrows(CompilationException.class, reloader::reload);
        } finally {
            System.clearProperty("javelit.compatibility.result");
        }
    }

    private static void writeApp(Path app, String prefix, String message) throws Exception {
        String source = "public class CompatibilityApp { public static void main(String[] args) {"
                + "System.setProperty(\"javelit.compatibility.result\", \"" + prefix + "\" + new Value().get()); }"
                + "private static class Value { String get() { return \"" + message + "\"; } } }";
        Files.write(app, source.getBytes(StandardCharsets.UTF_8));
    }
}
