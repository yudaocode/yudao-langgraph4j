# Javelit 0.89.0 runtime for Java 8

The source and web assets come from [Javelit 0.89.0](https://github.com/javelit/javelit/tree/javelit-0.89.0), under Apache License 2.0. The Maven Central source JAR has SHA-256 `f3133e30b6a4b28d67b525b7a27a4cb2b8b9da50dd27eb759659939b1d977cc3`. The original notices are retained in the source and the license is packaged in the runtime JAR.

This runtime exposes the Java component API and Undertow-backed server. The Maven application supplies its classpath. The JBang fat JAR/CLI and custom Javadoc doclet run with the official Java 21 distribution. Application-file compilation uses the standard JDK compiler interface available on Java 8. Runtime code uses Undertow Core 2.2.41.Final, Tablesaw 0.43.1 and Flexmark 0.62.2. These versions were selected after checking the bytecode of the newer dependency lines.

Java 8 tests cover standard-compiler reload and real WebSocket renders. The builder hashes instance fields to keep component keys stable when classes contain coverage instrumentation.
