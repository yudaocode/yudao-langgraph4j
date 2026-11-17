# Standalone Boot consumer

The Java test source is shared byte-for-byte across Boot2/3/4 branches. This project inherits the official Boot parent, imports the fork BOM and consumes installed artifacts; it does not inherit compatibility build tools. It tests a real application context and local HTTP model responses through graph execution/checkpoint serialization with both SDK adapters. No real supplier credentials or model calls are used.

Pass an actual target Java executable via `-Dcompat.test.jvm=...`. Boot4 and Boot3 default to17; Boot2 defaults to1.8. When using Java25, also pass `-Dcompat.expected.java=25`. These assertions are checked, not inferred from source/target. Install the branch artifacts and its producer SDKs into the same local Maven repository first.

The Java8 fixture also includes the optional OpenTelemetry module.
Six SDK/hook/log/metric/HTTP tests run beside the two real Boot graph-context tests, verifying the combined Spring AI/LangChain4j/OTel dependency graph.
The Boot2 parent properties explicitly select OkHttp4.12.0 and Kotlin1.9.25.
No remote collector is needed; actual OTLP HTTP export uses a local ephemeral server.
