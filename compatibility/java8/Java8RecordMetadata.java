import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

/**
 * Retains record canonical-constructor names without changing ordinary method metadata.
 *
 * @author 芋道源码
 */
public final class Java8RecordMetadata {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        if (!Files.isDirectory(root)) return;
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                byte[] bytes = Files.readAllBytes(file);
                ClassNode node = new ClassNode();
                new ClassReader(bytes).accept(node, 0);
                if (node.visibleAnnotations == null) continue;
                List<String> names = null;
                for (var annotation : node.visibleAnnotations) {
                    if (annotation.desc.equals("Ldev/langchain4j/internal/compat/Java8Record;")
                            && annotation.values != null) {
                        for (int i = 0; i < annotation.values.size(); i += 2) {
                            if (annotation.values.get(i).equals("value")) {
                                names = (List<String>) annotation.values.get(i + 1);
                            }
                        }
                    }
                }
                if (names == null) continue;
                List<Type> componentTypes = new ArrayList<>();
                for (String name : names) {
                    componentTypes.add(Type.getType(node.fields.stream().filter(f -> f.name.equals(name))
                            .findFirst().orElseThrow().desc));
                }
                String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, componentTypes.toArray(new Type[0]));
                boolean changed = false;
                for (MethodNode method : node.methods) {
                    if (method.name.equals("<init>") && method.desc.equals(descriptor)) {
                        if (method.parameters != null && method.parameters.size() == names.size()) continue;
                        method.parameters = new ArrayList<>();
                        for (String name : names) method.parameters.add(new ParameterNode(name, 0));
                        changed = true;
                    }
                }
                if (changed) {
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    Files.write(file, writer.toByteArray());
                }
            }
        }
    }
}
