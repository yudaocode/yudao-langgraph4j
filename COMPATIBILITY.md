# LangGraph4j compatibility scope

| 分支 | Spring Boot / 运行 JDK | 图核心 / LangChain4j adapter | LangChain4j | Spring AI | 本仓BOM / Spring AI adapter |
| --- | --- | --- | --- | --- | --- |
| `main` | 4.x / 17、25 | 官方1.9.9 | 官方1.23.0 | cn.iocoder.spring-ai:2.1.0-jdk25-SNAPSHOT | cn.iocoder.langgraph4j:1.9.9-jdk25-SNAPSHOT |
| `main-jdk17` | 3.x / 17、25 | 官方1.9.9 | 官方1.23.0 | cn.iocoder.spring-ai:2.1.0-jdk17-SNAPSHOT | cn.iocoder.langgraph4j:1.9.9-jdk17-SNAPSHOT |
| `main-jdk8` | 2.x / 8 | 本仓1.9.9-jdk8-SNAPSHOT | 本仓1.24.0 stable / beta34-jdk8 | cn.iocoder.spring-ai:2.1.0-jdk8-SNAPSHOT | cn.iocoder.langgraph4j:1.9.9-jdk8-SNAPSHOT |


## Source and coordinates

Upstream LangGraph4j commit: `bbce202845fe99660fcee29a7dd7a5acb4100199` (1.9.9).
Java 8 LangChain4j backport baseline: upstream commit `886c258b91090d41c941e69ab58edf6aa020a9c9` (1.24.0/beta34 development tree).
Java 17+ deliberately uses official LangChain4j 1.23.0 matching upstream graph adapters.
ArtifactIds and Java packages are retained; fork artifacts use distinct `cn.iocoder.*` groups.
No Maven Central deployment has been performed. Versions retain upstream baseline numbers, add the isolated JDK route and use a date suffix on release; see VERSIONING.md.

## Included modules and verification

- main / main-jdk17: parent + BOM + `spring-ai/spring-ai-core`; production graph core and LangChain4j adapter are official 1.9.9. Spring AI adapter unit suite: 18 tests, zero failures/errors/skips on Java17; Boot4 adapter also tested on Java25.
- main-jdk8: parent + BOM + `langgraph4j-java8-compat` + `async-generator` + `langgraph4j-core` + both adapter children plus filesystem, SQLite, Redis, PostgreSQL, MySQL, MongoDB and CockroachDB savers plus DSL export (15 modules). Native8 async74, core261, LangChain4j15 and Spring AI21 tests passed (zero failures/errors/skips, component gates). Tests include streaming, tools, serialization, concurrent graph behavior, executor cleanup, JSON schema and duration parsing.
- Identical standalone Java consumer source on Boot2/Java8, Boot3/Java17 and25, Boot4/Java17 and25: two tests per combination. Real Spring contexts, local HTTP model responses, graph execution, MemorySaver checkpoint and framework Jackson message restoration. Responses are local fixtures, not real cloud models.
- Java8 runtime closure is audited for class-file level and statically referenced Java8/SLF4J APIs. Optional native Java9 Flow adapters and guarded legacy SLF4J binders in third-party jars remain documented audit findings; they are not executed on the verified Java8 path.
- Remaining optional database savers, studio, examples, agents and observability modules outside the default reactor retain upstream sources/descriptors. They are not claimed as ported or tested. SQLite, Redis and PostgreSQL saver tests are exercised locally, including real container-backed Redis/PostgreSQL. External cloud models, Windows shell acceptance, Javadoc and binary compatibility release gates are not claimed.

## Build and reproduce

Use JDK17 to build; native Java8 tests require a separately installed JDK8. Maven3.9.x is used. Build matching yudao-spring-ai first; for Java8 also build yudao-langchain4j main-jdk8 (including its parent and small compatibility artifact). Modern branches consume official graph core and LC artifacts from Central.

```sh
# main-jdk8: JAVA_HOME points to JDK17; JAVA8_HOME points to a JDK8 installation
./mvnw -Dcompat.test.jvm="$JAVA8_HOME/bin/java" -Dmaven.javadoc.skip=true -Drevapi.skip=true verify
# install after verification for the standalone consumer
./mvnw -Dcompat.test.jvm="$JAVA8_HOME/bin/java" -Dmaven.javadoc.skip=true -Drevapi.skip=true -DskipTests install
./mvnw -f compatibility-tests/boot-consumer/pom.xml -Dcompat.test.jvm="$JAVA8_HOME/bin/java" verify

# main-jdk17 / main: build and test on JDK17 (or choose native JDK25 explicitly)
./mvnw verify
./mvnw -DskipTests install
./mvnw -f compatibility-tests/boot-consumer/pom.xml verify
# Run either modern consumer on Java25 with both properties:
./mvnw -f compatibility-tests/boot-consumer/pom.xml -Dcompat.test.jvm="$JAVA25_HOME/bin/java" -Dcompat.expected.java=25 verify
```

Boot2 parent's Apache HTTP5/Core5 and Logback properties must match README. Import the fork BOM before other model-framework BOMs and inspect dependency convergence when combining an existing application's custom pins. Do not mix official and fork artifacts that provide the same Java packages (including ITU, async-generator and graph core).

## Java8 implementation details

- Build-time source lowering preserves original source/API names, record accessors/equality and runtime parameter metadata; no build-time compiler dependencies are shipped as runtime requirements.
- `async-generator` 5.2.0 tagged sources/tests are ported under our group; MIT copyright and provenance retained. Private interface methods move to a package-private companion. Flow uses the Spring AI bridge.
- Java8 Cleaner helper uses phantom references and a daemon queue. Explicit close releases its owned executor exactly once, rejects later executor creation, and retains upstream GC cleanup tests. This ownership fix applies to our Java8 port; modern branches still consume the official async implementation and were not independently revalidated for every upstream GC lifecycle behavior.
- Graph core depends on small compatibility helpers, not the full LangChain4j model library. Spring AI and LangChain4j dependencies are selected only by the respective adapters.
- Spring AI2's shared ITU compatibility module recompiles unmodified ITU1.17.0 against the Java8 platform API. Original major52 ITU jars call a Java9-only Math overload; the original jar is excluded. Duration parsing regression executes on Java8.
- SLF4J1.7.36 and Jackson2.21.4 follow the Spring AI2 producer pins. The Spring AI adapter's upper-bound exceptions are limited to those two coordinates; convergence remains active, and real schema/converter tests exercise this runtime.

## Upstream README

Only a fork preface is added to the root README. The upstream suffix is preserved byte-for-byte (SHA256 `fd1d349646213cfcb8204c1e54b7f9888c412482b6c10d232f00c5ff54dc318b`). Upstream instructions below that preface may describe modules outside this compatibility scope.

## Additional Java8 savers

The Java8 default reactor now includes filesystem-saver and sqlite-saver. Their real native8 suites passed10 and21 tests. SQLite AGUI/Javelit demo fixtures remain outside this support scope; core saver unit assertions are retained. Version-coordinate revalidation is recorded separately.

The complete 14-module native Java8 clean new-coordinate install passed 495 test executions with zero failures/errors/skips. Five Boot2/3/4 × supported-JDK consumer combinations passed two tests each. Default production-runtime bytecode scan: 147 jars / 72881 effective base classes, none above major52. Extra studio/web demo fixtures excluded from saver builds remain outside the supported scope.

## Optional saver coordinates

MySQL/MongoDB/CockroachDB: Java8 uses fork artifacts from the current BOM; Java17/25 uses official org.bsc.langgraph4j1.9.9 artifacts already imported by the modern BOM. The Java8 default14-module clean install passed495 tests with no failures/errors/skips. The same upstream59 saver contracts passed separately against the actual official jars on native17 and25, using real database containers. This acceptance is independent of Spring Boot: these saver APIs do not require a Boot starter. Java8 excludes MongoDB native record codec; driver baseline5.5.1, MySQL9.2.0 and PostgreSQL42.7.7 retained.

## Graph DSL export

Java8 adds `langgraph4j-dsl` under the compatibility BOM and group, expanding the default reactor to15 modules. A complete native8 clean install passed498 cases, zero failures/errors/skips. The three upstream DSL contracts cover JSON graph structure, schema resource and nested subgraphs; source lowering handles modern syntax without rewriting production source. Javadoc remains excluded because its parser does not use Jabel.

Modern branches use official `org.bsc.langgraph4j:langgraph4j-dsl:1.9.9`, already managed by the imported official BOM. The same three unchanged upstream contracts passed against actual official jars on native17 and25. These graph-only tests are independent of Boot. Add the module and reduce the compiled graph with `JsonDslGenerator`; the JSON schema resource is packaged in the jar. Other optional modules outside the default reactor remain unverified.

## DynamoDB saver

Java8 adds `langgraph4j-dynamodb-saver` to the compatibility BOM/reactor (16 modules). After the unchanged15-module498-case full gate, the added saver passed42 native8 cases, zero failures/errors, one upstream-disabled checkpoint-lineage case. Actual local DynamoDB and S3-compatible storage containers exercise checkpoint state, large-payload offloading and conditional writes. The test port mapping is dynamic to avoid concurrent host-port collisions; assertions are retained. This is an added component gate, not a new one-shot full16-module run.

The Java8 module aligns AWS SDK to the parent-compatible2.57.12 (above upstream module2.27.21), excludes automatic Apache/Netty transport choices and supplies URLConnection for synchronous runtime clients; applications may explicitly supply other HTTP clients. The existing Apache transport tests retain a test-only Apache client. HTTP4 test dependencies converge at httpcore4.4.16/commons-codec1.17.1. Jackson annotation metadata is optional, and the compile-only @Serial marker is omitted while serialVersionUID remains unchanged. Production closure182jars /79550 effective base classes is <=52; static API findings29 remain reviewed third-party optional paths.

Modern branches keep official `org.bsc.langgraph4j:langgraph4j-dynamodb-saver:1.9.9`; actual official jars passed the same42 contracts on native17 and25 (zero failures/errors, same upstream skip). Add optional S3 SDK explicitly when using offload and configure actual AWS credentials/endpoints separately. No AWS paid service was used in this gate.

## OpenTelemetry

Java8 adds the OpenTelemetry module to the BOM/default reactor (17 modules).
A complete serial native8 clean install passed547 cases with zero failures/errors and one existing DynamoDB lineage skip.
The seven new OTel cases exercise real OTLP HTTP export with SDK1.55.0, workflow hook spans, checked failure recording, baggage scope restoration, actual Logback1.2 logs with trace IDs, SDK metrics and local collector protobuf/file output.
Four upstream integration-style cases also passed separately on native8 with external exporters disabled; the seven local protocol tests cover real export.
The Boot2 consumer combines both framework adapters and OTel, passing eight native8 tests in the native2.7.18 parent dependency graph.

SDK1.55.0 and instrumentation2.21.0 are retained.
The Java8 transport excludes the SDK's transitive okhttp-jvm5 artifact and uses tested OkHttp4.12.0; Boot2 parent properties select okhttp4.12.0/kotlin1.9.25/Logback1.2.13.
The local development collector accepts port0, offers an explicit bind address and AutoCloseable lifecycle, and returns a connectable loopback endpoint for wildcard listeners.
The original proto/protobuf dependencies remain optional; applications using the internal collector must supply them explicitly.
Java8 builds default to one Maven thread to avoid parallel container startup exhaustion; no database assertions were removed.

Modern branches keep actual official org.bsc.langgraph4j1.9.9 OTel jars.
The same six hook/SDK/HTTP contracts passed independently on native17 and25 using those official artifacts; they do not assert compatibility of the Java8-only collector lifecycle additions with upstream.

The final selected-runtime audit includes17 default-module classpaths plus the refreshed Boot2 consumer:195 jars /80612 effective base classes, all <=52.
Static API findings31 retain the previously identified optional modern-JVM serializers/Flow/SLF4J paths plus OTel optional SLF4J2 key-value types; the native8 Logback1.2 capture test verifies the fallback when those types are absent.


## LangChain4j Agent Executor on Java 8

`cn.iocoder.langgraph4j:langgraph4j-langchain4j-agentexecutor` uses the same Java 8 BOM as the graph core. The module preserves the LangChain4j 1.24.0 branch and existing tool, skill and checkpoint APIs. The optional MCP library has provided scope. Add Jackson explicitly when selecting Jackson state serialization.

The 18-module build on native Java 8 reports 594 tests, zero failures or errors, and one previously disabled upstream case. The Agent Executor has 47 passing component tests. An independent Boot 2.7 consumer ran nine tests, including a local OpenAI-compatible HTTP tool-call loop across two turns on the same checkpoint thread. Java 8's runtime classpath contains 24 JARs and 5,193 base classes. Existing Studio/Javelit demo sources and cloud integration cases retain their own prerequisites.


## Spring AI Agent Executor on Java 8

Use `cn.iocoder.langgraph4j:langgraph4j-springai-agentexecutor` with this branch's BOM and Spring AI `2.1.0-jdk8-SNAPSHOT`. The library keeps the upstream model, tool and graph interfaces. Its Spring runtime uses the Spring Boot 2.7 generation.

The native JDK 8 default build covers 19 modules and 597 cases with one previously disabled upstream case. Three component tests cover approval-edge resumption with an external tool result and UTF-8 skill loading. The independent Boot 2.7 application verifies both Agent Executors against local HTTP model responses over two checkpoint turns. Studio, Javelit and TypeSafe/JBang demo sources keep their own framework requirements.


## Oracle and Hazelcast savers on Java 8

The default Java 8 reactor and BOM now include both savers. Oracle retains JDBC 23.9 via `ojdbc8` and the Jackson OSON provider. Both upstream test classes use their own Oracle Free 23.7 container and its built-in PDB. Hazelcast 5.2.5 is the Java 8 dependency, with the AP `IMap` storage path. The `CP_MAP` enum is retained so callers get an explicit error instead of a silent change in storage semantics. Enterprise CPMap was introduced after the Hazelcast Java 8 line.

The reconciled native Java 8 `clean install` reports cover 21 modules and 629 test cases (1 conditional skips). The two savers contribute 32 passing cases. An independent installed-artifact consumer with Boot 2.7.18 dependency management exercises write, reopen, read and release on real Oracle and Hazelcast. Its 26 runtime JARs contain 16,041 base classes at Java 8 bytecode level.


## AG-UI 0.1.1 on Boot 2 and Java 8

The new AG-UI Java 8 protocol JAR builds the published Community Java SDK 0.1.1 core, server and client, with the upstream source hashes and license texts in `META-INF`. The Java 8 source generator lowers records and Flow signatures while keeping AG-UI JSON names. The client reads SSE under downstream demand on a blocking executor using the existing Spring AI HTTP/Flow compatibility layer. The JSON adapter, LangGraph4j SDK and Boot 2 MVC controller ship as separate BOM-managed artifacts.

The 25-module `clean install` reports 641 tests (1 conditional skip). Protocol tests cover polymorphic JSON, in-band SSE event order, UTF-8 and HTTP errors. A standalone installed-artifact test starts Boot 2.7.18 on Java 8, posts to the controller and receives a run-start, graph-generated text and run-finished event. The installed runtime has 42 JARs and 12,634 Java 8 base classes. Boot 2 consumers using the Java HTTP client need HTTP Client 5.6.5 and HttpCore 5.4.4 as shown in the README. The Javelit demo still uses its Java 21 route.


## Studio on Java 8

Studio base uses `javax.servlet` and the Java 8 line of the Victools test generator (4.38.0). The server adapters use Jetty 9.4.58.v20250814 and Spring Boot 2.7.18. Both retain the upstream legacy and AG-UI assets, initialization and graph streaming flows. Runtime tests start both actual servers on loopback, load packaged HTML and initialization data, submit a UTF-8 graph input and receive the output. The independent Boot 2 and Jetty consumers repeat both routes using the installed JARs.

Native Java 8 clean install covers 28 modules and 647 test cases with 1 conditional skip. Studio contributes six passing cases. The two standalone consumers contribute four real HTTP cases separately.


## Quarkus Studio on Java 8

Quarkus 1.13.7.Final is the last Java 8 line. The adapter uses `javax` Servlet/CDI, RESTEasy Classic, and `META-INF/resources` for the upstream packaged Studio web assets. The test suite checks the page and init API. Deploy the whole `quarkus-app` directory and start `quarkus-run.jar` on Java 8.

The independent copied fast-jar executes HTML, initialization JSON and the configured sample graph over local HTTP. Its 107 archives contain 12,527 Java 8 base classes. The native Java 8 Maven reactor covers 29 modules with 649 cases and 1 conditional skip.
