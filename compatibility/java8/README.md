# Java 8 build tooling

The Java17 build uses the yudao-langchain4j parent build, with local source-lowering and record-metadata tools. Runtime graph core only uses its small helper artifact. Original source names/packages and record behavior are retained; unsupported modern platform APIs require explicit replacements, not just source lowering.

The source generator and metadata helper derive from the Apache-2.0 licensed yudao-langchain4j compatibility tools; original headers are retained. Local additions handle Cleaner, copyOf method references, three-argument Stream.iterate and static Java9 helper imports. String.stripIndent lowering is restricted to a real String receiver so domain helpers with the same name are preserved.

After packaging and generating the runtime dependency classpaths, the bytecode audit checks base classes in all selected production jars:

```sh
./mvnw dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/java8-runtime-classpath.txt
./mvnw -f compatibility-tests/boot-consumer/pom.xml dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/java8-runtime-classpath.txt
python3 compatibility/java8/audit-runtime-bytecode.py
```

Multi-release overlays and module-info are intentionally excluded for Java8. Bytecode level alone is insufficient: native8 tests and static platform-member auditing found and fixed the shared ITU Math overload bug. Optional native9 Flow adapters and guarded legacy SLF4J binders in third-party jars are separate known findings; this script does not inspect member availability.
