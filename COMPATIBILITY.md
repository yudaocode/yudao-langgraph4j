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
- main-jdk8: parent + BOM + `langgraph4j-java8-compat` + `async-generator` + `langgraph4j-core` + both adapter children plus filesystem, SQLite, Redis and PostgreSQL savers (11 modules). Native8 async74, core261, LangChain4j15 and Spring AI21 tests passed (zero failures/errors/skips, component gates). Tests include streaming, tools, serialization, concurrent graph behavior, executor cleanup, JSON schema and duration parsing.
- Identical standalone Java consumer source on Boot2/Java8, Boot3/Java17 and25, Boot4/Java17 and25: two tests per combination. Real Spring contexts, local HTTP model responses, graph execution, MemorySaver checkpoint and framework Jackson message restoration. Responses are local fixtures, not real cloud models.
- Java8 runtime closure is audited for class-file level and statically referenced Java8/SLF4J APIs. Optional native Java9 Flow adapters and guarded legacy SLF4J binders in third-party jars remain documented audit findings; they are not executed on the verified Java8 path.
- Optional database savers, studio, examples, agents and observability modules outside the default reactor retain upstream sources/descriptors. They are not claimed as ported or tested. SQLite, Redis and PostgreSQL saver tests are exercised locally, including real container-backed Redis/PostgreSQL. External cloud models, Windows shell acceptance, Javadoc and binary compatibility release gates are not claimed.

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
