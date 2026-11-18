# yudao-langgraph4j 兼容版

> 版本命名：保留当前源码基线的官方版本号；共用现代制品不加 JDK 后缀，隔离维护的兼容路线添加 `jdk8` / `jdk17` / `jdk25`。SNAPSHOT 尚未发布 Maven Central。JDK 路线后缀不等于最低字节码版本，框架依赖仍按 Boot 版本选择。
> 完整版本规则与预览命名见 [VERSIONING.md](VERSIONING.md)。


保留 LangGraph4j 的 Java 包名与 API，支持 Spring Boot 2/3/4 的组合。Java 8 分支尽量回迁较高版本；Java 17+ 优先复用官方核心和 LangChain4j adapter，仅维护 Spring AI 适配层。上游源码基线为 1.9.9（`bbce202845fe99660fcee29a7dd7a5acb4100199`）。

| 分支 | Spring Boot / 运行 JDK | 图核心 / LangChain4j adapter | LangChain4j | Spring AI | 本仓BOM / Spring AI adapter |
| --- | --- | --- | --- | --- | --- |
| `main` | 4.x / 17、25 | 官方1.9.9 | 官方1.23.0 | cn.iocoder.spring-ai:2.1.0-jdk25-SNAPSHOT | cn.iocoder.langgraph4j:1.9.9-jdk25-SNAPSHOT |
| `main-jdk17` | 3.x / 17、25 | 官方1.9.9 | 官方1.23.0 | cn.iocoder.spring-ai:2.1.0-jdk17-SNAPSHOT | cn.iocoder.langgraph4j:1.9.9-jdk17-SNAPSHOT |
| `main-jdk8` | 2.x / 8 | 本仓1.9.9-jdk8-SNAPSHOT | 本仓1.24.0 stable / beta34-jdk8 | cn.iocoder.spring-ai:2.1.0-jdk8-SNAPSHOT | cn.iocoder.langgraph4j:1.9.9-jdk8-SNAPSHOT |

当前分支：`main-jdk8`，兼容版版本号：`1.9.9-jdk8-SNAPSHOT`。上述 `-SNAPSHOT` 当前需从源码安装；尚未发布 Maven Central，后续发布统一使用 `cn.iocoder.*` 坐标，不覆盖官方 artifact。

## 如何使用

在应用的 `dependencyManagement` 中导入当前分支 BOM：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.iocoder.langgraph4j</groupId>
            <artifactId>langgraph4j-bom</artifactId>
            <version>1.9.9-jdk8-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

按模型框架选择 adapter；BOM 已管理核心、adapter 与匹配的模型框架版本：

```xml
<!-- Spring AI 路线 -->
<dependency>
    <groupId>cn.iocoder.langgraph4j</groupId>
    <artifactId>langgraph4j-spring-ai</artifactId>
</dependency>
<!-- LangChain4j 路线；按需选择，也可同时使用两条路线 -->
<dependency>
    <groupId>cn.iocoder.langgraph4j</groupId>
    <artifactId>langgraph4j-langchain4j</artifactId>
</dependency>
```

模型 provider 仍需按需添加，例如 Spring AI 路线的 `cn.iocoder.spring-ai:spring-ai-openai`；LangChain4j 路线在 Java 8 用 `cn.iocoder.langchain4j:langchain4j-open-ai`，Java 17+ 用 `dev.langchain4j:langchain4j-open-ai`。对应版本由 BOM 管理。使用一种路线时无需添加另一种 adapter。

Boot 2 使用官方 `spring-boot-starter-parent` 时，还需设置以下属性，避免父 POM 直接管理的旧版本覆盖兼容要求：

```xml
<properties>
    <httpclient5.version>5.6.5</httpclient5.version>
    <httpcore5.version>5.4.4</httpcore5.version>
    <logback.version>1.2.13</logback.version>
</properties>
```

默认验证范围、源码构建顺序、完整消费者示例见 [COMPATIBILITY.md](COMPATIBILITY.md) 和 [Boot 消费者](compatibility-tests/boot-consumer)。当前不将仓库里全部可选 saver、studio、samples 列为兼容完成；API key / 外部模型服务未参与本轮验证。


## 可选 checkpoint saver

MySQL、MongoDB、CockroachDB 已纳入兼容验证。Boot3/4 + JDK17/25 复用官方 `org.bsc.langgraph4j` 的1.9.9制品；Boot2/JDK8 使用本仓 `cn.iocoder.langgraph4j` 的1.9.9-jdk8-SNAPSHOT。当前分支BOM已管理对应版本，按需添加：

```xml
<dependency>
    <groupId>cn.iocoder.langgraph4j</groupId>
    <artifactId>langgraph4j-mysql-saver</artifactId>
</dependency>
```

MongoDB、CockroachDB 分别改为 `langgraph4j-mongodb-saver`、`langgraph4j-cockroachdb-saver`。数据库连接与 `StateSerializer` 按模块builder配置；测试涵盖真实容器中的checkpoint恢复、隔离与历史记录。Java8 Mongo驱动排除仅支持Java17的可选record codec，使用标准BSON/POJO编码。

## 可选图 DSL 导出

DSL 导出已完成 Java8 /17 /25 验证，按需添加（版本由当前分支 BOM 管理）：

```xml
<dependency>
    <groupId>cn.iocoder.langgraph4j</groupId>
    <artifactId>langgraph4j-dsl</artifactId>
</dependency>
```

使用 `org.bsc.langgraph4j.dsl.JsonDslGenerator`：`workflow.compile().reduce(new JsonDslGenerator<>())` 得到 JSON 图定义；模块自带 `langgraph4j-dsl.schema.json`。Java8 使用兼容制品，Java17/25 复用官方1.9.9，不额外维护同名现代分叉。导出包含条件边与嵌套子图，不包含模型服务调用。

## 可选 DynamoDB saver

DynamoDB saver 已完成真实 JDK8/17/25 容器测试，按需引入（版本由当前分支 BOM 管理）：

```xml
<dependency>
    <groupId>cn.iocoder.langgraph4j</groupId>
    <artifactId>langgraph4j-dynamodb-saver</artifactId>
</dependency>
```

使用 `org.bsc.langgraph4j.checkpoint.DynamoDBSaver` builder，传入已配置的 `DynamoDbClient` 与状态序列化器；连接地址、AWS凭据由应用提供。大状态的 S3 offload 需另引入 AWS SDK `s3` 并提供 `S3Client`。Java8 路线将 AWS SDK 统一为已验证的 `2.57.12`，默认使用 URL Connection HTTP 实现；现代路线复用官方1.9.9的依赖。本轮各 JVM 均为42项、零失败/错误、1项上游禁用（尚待core支持checkpoint lineage），数据库与S3使用本地DynamoDB/对象存储容器，不代表AWS云账号验收。

## 可选 OpenTelemetry

按需引入以下模块，版本由当前分支 BOM 管理：

```xml
<dependency>
    <groupId>cn.iocoder.langgraph4j</groupId>
    <artifactId>langgraph4j-opentelemetry</artifactId>
</dependency>
```

JDK 8 使用本仓兼容模块；JDK 17、25 复用官方 1.9.9 制品。
Java8 保留 OpenTelemetry SDK 1.55.0 / instrumentation 2.21.0，日志对齐 SLF4J1.7 / Logback1.2.13，HTTP sender 使用已验证的 OkHttp4.12.0。
Boot2 parent 还需设置 `okhttp.version=4.12.0`、`kotlin.version=1.9.25`，完整组合示例见 Java8 分支的 `compatibility-tests/boot-consumer`。
Java8 的7项本地协议/SDK测试与4项上游集成用例均通过；后4项关闭远端 exporter，实际HTTP导出和本地三信号收集由前7项覆盖。
官方制品在 JDK17/25 分别通过同一组6项 hooks/SDK/HTTP 测试；Java8 的 Boot2 组合消费者8项通过。
这些验证未使用远端 Collector、Jaeger 或付费模型。

## LangChain4j Agent Executor（Boot 2 / JDK 8）

导入本分支的 BOM 后，按需添加：

```xml
<dependency>
    <groupId>cn.iocoder.langgraph4j</groupId>
    <artifactId>langgraph4j-langchain4j-agentexecutor</artifactId>
</dependency>
```

通过 `AgentExecutor.builder()` 或 `AgentExecutorEx.builder()` 传入 LangChain4j 的模型与工具，编译为 `StateGraph` 后可使用 checkpoint saver 恢复同一线程。使用 MCP 工具时另引入兼容版 `langchain4j-mcp`；使用 Jackson 序列化时引入 `jackson-databind`。LangChain4j 保留 `1.24.0-jdk8-SNAPSHOT`，MCP 采用该分支对应的 beta 制品。

原生 JDK8 通过 47 项组件测试及独立 Boot2 应用的双轮 HTTP 工具调用和检查点恢复。Studio/Javelit 演示和需要云端凭据的测试保留各自的运行前提。


## Spring AI Agent Executor（Boot 2 / JDK 8）

导入本分支的 BOM 后，按需添加：

```xml
<dependency>
    <groupId>cn.iocoder.langgraph4j</groupId>
    <artifactId>langgraph4j-springai-agentexecutor</artifactId>
</dependency>
```

使用 `org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor` 或 `AgentExecutorEx`，传入 Spring AI `ChatModel` 和工具。Java8 路线复用 `cn.iocoder.spring-ai` 的 `2.1.0-jdk8-SNAPSHOT`，兼容 BOM 管理依赖。支持批注工具、审批中断与恢复、技能文件以及同线程检查点。独立 Boot 2.7 消费验证见 `compatibility-tests/boot-consumer`。


## Oracle 与 Hazelcast Saver（JDK 8）

导入本分支 BOM 后，按实际存储选用 `cn.iocoder.langgraph4j:langgraph4j-oracle-saver` 或 `cn.iocoder.langgraph4j:langgraph4j-hazelcast-saver`。

- Oracle 使用 `ojdbc8:23.9.0.25.07`。V1/V2 均可通过 `DataSource`、状态序列化器和 `CreateOption` 配置，数据库需支持 JSON 类型。
- Hazelcast 另加 `com.hazelcast:hazelcast:5.2.5`，通过 `HazelcastSaver.builder().hazelcastInstance(instance)` 接入 `IMap`。JSON 存储需另提供 Jackson 2。Enterprise `CPMap` 使用现代 Java 路线。

该批次默认构建共 21 个模块。独立安装包已在原生 JDK 8 上验证实际 Oracle Free 23.7，以及 Hazelcast 成员与 TCP 客户端之间的检查点读写和释放。

## AG-UI（Boot 2 / JDK 8）

导入本分支 BOM，再引入 `cn.iocoder.langgraph4j:langgraph4j-ag-ui-springboot` 和 Boot 2 的 `spring-boot-starter-web`。将自定义的 `AGUIAgent` 放入 `AGUIAgentRegistry`，并注册 `new AGUISSEController(registry, new AGUIJacksonSerializer())`。客户端向 `/sse/{agentId}` POST AG-UI JSON，从 SSE 响应消费事件。

Java AG-UI Client 与 JDK HTTP Server 使用 `langgraph4j-ag-ui-protocol-java8`，承接上游 Community SDK `0.1.1`。若应用同时用 Boot 2 父 POM 和兼容版 Java HTTP 客户端，设置 `httpclient5.version=5.6.5` 与 `httpcore5.version=5.4.4`，避免 Boot 2 将 HTTP 库降到发行时的版本。JSON 使用 Jackson 2.21.4 BOM。独立消费验证运行于 Boot `2.7.18` 和 JDK 8。

AG-UI JSON、SDK 与 Spring Boot 适配加入后，JDK 8 默认构建共 25 个模块。

---

<!-- 以下保留官方 README 原文 -->

#  ![avatar](https://avatars.githubusercontent.com/u/212100817?s=60&v=4)️ Welcome to LangGraph4j

### *Enabling Graph Engineering for Agentic AI Workflows in Java*

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg) [![docs](https://img.shields.io/badge/Site-Documentation-blue)][documentation] [![DeepWiki](https://img.shields.io/badge/DeepWiki-docs-0b6?style=flat)](https://deepwiki.com/langgraph4j/langgraph4j) [![Static Badge](https://img.shields.io/badge/maven--snapshots-1.9--SNAPSHOT-blue)][snapshots] [![Maven Central](https://img.shields.io/maven-central/v/org.bsc.langgraph4j/langgraph4j-core.svg)][releases][![discord](https://img.shields.io/discord/1364514593765986365?logo=discord&style=flat)](https://discord.gg/szVVztSYKh)

LangGraph for Java. A library for building stateful, multi-agents applications with LLMs, built for work with [langchain4j] and [Spring AI]
> It is inspired by [LangGraph] solution, part of [LangChain AI project].

## Releases

‼️ **the repo has been updated to `1.9`, the version `1.8` has been moved to the branch `support/1.8.x`.**.
Take a look to [What's new in release 1.9](https://langgraph4j.github.io/langgraph4j/1.9/whats-new-v1.9/)


| Date         | Release        | info
|--------------|----------------| ---
| Nov 7, 2026 | `1.9.9` | last release


| Release line | Java baseline | Notes |
|---|---|---|
| 1.9.x | Java 17+ | Release with new (experimental) features and improvements for preparing move to `2.0` |
| `1.9-SNAPSHOT` development builds | Java 17+ | Snapshot users should expect active development and pre-release changes |
| `1.8.x` LTS release | Java 17+ | This release will be maintained for LTS support in branch `support/1.8.x`. Only bugfix and/or minor improvement |


## Star History

<a href="https://star-history.dera.page/#langgraph4j/langgraph4j&Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://star-history.dera.page/svg?repos=langgraph4j/langgraph4j&type=Date&theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://star-history.dera.page/svg?repos=langgraph4j/langgraph4j&type=Date" />
    <img alt="Star History Chart" src="https://star-history.dera.page/svg?repos=langgraph4j/langgraph4j&type=Date" />
  </picture>
</a>

# Getting Started

Welcome to LangGraph4j! This guide will help you understand the core concepts of LangGraph4j, install it, and build your first application.

## Introduction

LangGraph4j is a Java library for building stateful, multi-agent applications with Large Language Models (LLMs). It is inspired by the Python library [LangGraph](https://github.com/langchain-ai/langgraph) and is designed to work seamlessly with popular Java LLM frameworks like [Langchain4j](https://github.com/langchain4j/langchain4j) and [Spring AI](https://spring.io/projects/spring-ai).

At its core, LangGraph4j allows you to define cyclical graphs where different components (agents, tools, or custom logic) can interact in a stateful manner. This is crucial for building complex applications that require memory, context, and the ability for different "agents" to collaborate or hand off tasks.

## Core Features & Benefits

LangGraph4j offers several features and benefits:

*   **Stateful Execution:** Manage and update a shared state across graph nodes, enabling memory and context awareness.
*   **Cyclical Graphs:** Unlike traditional DAGs, LangGraph4j supports cycles, essential for agent-based architectures where control flow can loop back (e.g., an agent retrying a task or asking for clarification).
*   **Explicit Control Flow:** Clearly define the paths and conditions for transitions between nodes in your graph.
*   **Modularity:** Build complex systems from smaller, reusable components (nodes).
*   **Flexibility:** Integrate with various LLM providers and custom Java logic.
*   **Observability & Debugging:**
    *   **Checkpoints:** Save the state of your graph at any point and replay or inspect it later. This is invaluable for debugging and understanding complex interactions.
    *   **Graph Visualization:** Generate visual representations of your graph using PlantUML or Mermaid to understand its structure.
*   **Asynchronous & Streaming Support:** Build responsive applications with non-blocking operations and stream results from LLMs.
*   **Playground & Studio:** A web UI to visually inspect, run, and debug your graphs.

## Pattern Matrix

| Pattern | Best for | Main abstraction | Start here |
|---|---|---|---|
| First graph / linear flow | Learning the core execution model | `StateGraph`, normal edges, shared state | [`## Your First Graph - A Simple Example`](#your-first-graph---a-simple-example) |
| Conditional routing | Router-style decisions and dynamic control flow | Conditional edges | [`### Edges`](#edges) |
| Stateful checkpointed flow | Long-running or resumable workflows | `CheckpointSaver`, `CompileConfig` | [`### Checkpoints (Persistence)`](#checkpoints-persistence) |
| Framework-integrated agents | Using LangGraph4j with Java AI frameworks | LangChain4j / Spring AI integrations | [`## BONUS: built-in integrations`](#bonus-built-in-integrations) |
| Visual debugging and inspection | Observing and iterating on graphs interactively | Studio | [`## Studio 🤩 - Running Your Graph visually`](#studio--running-your-graph-visually) |

## Core Concepts Explained

Understanding these concepts is key to using LangGraph4j effectively:

### `StateGraph<S extends AgentState>`

The `StateGraph` is the primary class you'll use to define the structure of your application. It's where you add nodes and edges to create your graph. It is parameterized by an `AgentState`.

### `AgentState`

The `AgentState` (or a class extending it) represents the shared state of your graph. It's essentially a map (`Map<String, Object>`) that gets passed from node to node. Each node can read from this state and return updates to it.

*   **Schema:** The structure of the state is defined by a "schema," which is a `Map<String, Channel.Reducer>`. Each key in the map corresponds to an attribute in the state.
*   **`Channel.Reducer`:** A reducer defines how updates to a state attribute are handled. For example, a new value might overwrite the old one, or it might be added to a list of existing values.
*   **`Channel.Default<T>`:** Provides a default value for a state attribute if it's not already set.
*   **`Channel.Appender<T>` / `MessageChannel.Appender<M>`:** A common type of reducer that appends the new value to a list associated with the state attribute. This is useful for accumulating messages, tool calls, or other sequences of data. `MessageChannel.Appender` is specifically designed for chat messages and can also handle message deletion by ID.

### `Nodes`

Nodes are the building blocks of your graph that perform actions. A node is typically a function (or a class implementing `NodeAction<S>` or `AsyncNodeAction<S>`) that:
1.  Receives the current `AgentState` as input.
2.  Performs some computation (e.g., calls an LLM, executes a tool, runs custom business logic).
3.  Returns a `Map<String, Object>` representing updates to the state. These updates are then applied to the `AgentState` according to the schema's reducers.

Nodes can be synchronous or asynchronous (`CompletableFuture`).

### `Edges`

Edges define the flow of control between nodes.

*   **Normal Edges:** An unconditional transition from one node to another. After node A completes, control always passes to node B. You define these with `addEdge(sourceNodeName, destinationNodeName)`.
*   **Conditional Edges:** The next node is determined dynamically based on the current `AgentState`. After a source node completes, an `EdgeAction<S>` (or `AsyncEdgeAction<S>`) function is executed. This function receives the current state and returns the name of the next node to execute. This allows for branching logic (e.g., if an agent decided to use a tool, go to the "execute_tool" node; otherwise, go to the "respond_to_user" node). Conditional edges are defined with `addConditionalEdges(...)`.
*   **Entry Points:** You can also define conditional entry points to your graph using `addConditionalEntryPoint(...)`.

### `Compilation`

Once you've defined all your nodes and edges in a `StateGraph`, you `compile()` it into a `CompiledGraph<S extends AgentState>`. This compiled graph is an immutable, runnable representation of your logic. Compilation validates the graph structure (e.g., checks for orphaned nodes).

### `Checkpoints (Persistence)`

LangGraph4j allows you to save (`Checkpoint`) the state of your graph at any step. This is extremely useful for:
*   **Debugging:** Inspect the state at various points to understand what happened.
*   **Resuming:** Restore a graph to a previous state and continue execution.
*   **Long-running processes:** Persist the state of long-running agent interactions.
You'll typically use a `CheckpointSaver` implementation (e.g., `MemorySaver` for in-memory storage, or you can implement your own for persistent storage).

Useful starting points for persistence:

- [`langgraph4j-mysql-saver/README.md`](langgraph4j-mysql-saver/README.md) for MySQL-backed checkpoints
- [`langgraph4j-filesystem-saver/README.md`](langgraph4j-filesystem-saver/README.md) for file-backed V2 checkpoints
- [`langgraph4j-postgres-saver/README.md`](langgraph4j-postgres-saver/README.md) for PostgreSQL-backed checkpoints
- [`langgraph4j-redis-saver/README.md`](langgraph4j-redis-saver/README.md) for Redis-backed checkpoints
- [`langgraph4j-mongodb-saver/README.md`](langgraph4j-mongodb-saver/README.md) for MongoDB-backed checkpoints

If you only want to see the minimal integration point first, look for `CompileConfig.builder().checkpointSaver(...)` in the saver module examples before diving into the full storage details.

## Project Structure

```
langgraph4j/
├── langgraph4j-bom/                     # LangGraph4j dependency management
├── langgraph4j-core/                    # LangGraph4j core components
├── langgraph4j-opentelemetry/           # LangGraph4j Hook & OpenTelemetry integration module
├── langgraph4j-mysql-saver              # LangGraph4j persistent checkpoint saver based on MySQL
├── langgraph4j-filesystem-saver         # LangGraph4j persistent checkpoint saver based on files
├── langgraph4j-oracle-saver             # LangGraph4j persistent checkpoint saver based on OracleDB
├── langgraph4j-postgres-saver           # LangGraph4j persistent checkpoint saver based on PostgresSQL
├── langgraph4j-dynamodb-saver           # LangGraph4j persistent checkpoint saver based on Amazon DynamoDB
├── langgraph4j-cockroachdb-saver        # LangGraph4j persistent checkpoint saver based on CockroachDB
├── langgraph4j-mongodb-saver            # LangGraph4j persistent checkpoint saver based on MongoDB
├── langgraph4j-redis-saver              # LangGraph4j persistent checkpoint saver based on Redis
├── langgraph4j-hazelcast-saver          # LangGraph4j persistent checkpoint saver based on Hazelcast
├── langgraph4j-sqlite-saver             # LangGraph4j persistent checkpoint saver based on SQLite
├── langgraph4j-dsl                      # LangGraph4j domain-specific language (DSL) module
├── ag-ui                                # [AG UI protocol](https://docs.ag-ui.com/introduction) integration
│   ├── langgraph4j-ag-ui-json           # LangGraph4j AG UI JSON module
│   ├── langgraph4j-ag-ui-sdk            # LangGraph4j AG UI SDK module
│   └── langgraph4j-ag-ui-springboot     # LangGraph4j AG UI Spring Boot module
├── langchain4j/                         # LangChain4j integration
│   ├── langchain4j-core/                # LangChain4j core components (integration required)
│   └── langchain4j-agent/               # LangChain4j agent executor
├── spring-ai/                           # Spring AI integration
│   └── spring-ai-core/                  # Spring AI core components (integration required)
│   └── spring-ai-agent/                 # Spring AI agent executor
├── studio/                              # LangGraph4j Studio (web UI)
│   └── base/                            # Base classes and interfaces
│   └── jetty/                           # Jetty server implementation
│   └── quarkus/                         # Quarkus server implementation
│   └── springboot/                      # Spring Boot implementation
├── how-tos/                             # How-tos and examples
├── javelit/                             # LangGraph4j & Javelit
```



## Installation

To use LangGraph4j in your project, you need to add it as a dependency.

**Maven:**

Make sure you are using Java 17 or later.

**Latest Stable Version (Recommended):**
```xml
<properties>
    <langgraph4j.version>1.9.9</langgraph4j.version> <!-- Check for the actual latest version -->
</properties>

<!-- Optional: Add the Bill of Materials (BOM) to manage langgraph4j module versions -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.bsc.langgraph4j</groupId>
      <artifactId>langgraph4j-bom</artifactId>
      <version>${langgraph4j.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.bsc.langgraph4j</groupId>
        <artifactId>langgraph4j-core</artifactId>
    </dependency>
    <!-- Add other langgraph4j modules if needed, e.g., langgraph4j-langchain4j -->
</dependencies>
```
*(Note: Always check the [Maven Central Repository](https://central.sonatype.com/search?q=g%3Aorg.bsc.langgraph4j) for the latest version number.)*

**Development Snapshot Version:**
If you want to use the latest unreleased features, you can use a snapshot version.
```xml
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-core</artifactId>
    <version>1.9.9</version> <!-- Or the current snapshot version -->
</dependency>
```
You might need to configure your `settings.xml` or `pom.xml` to include the Sonatype OSS snapshots repository:
```xml
<repositories>
    <repository>
        <id>sonatype-oss-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## Your First Graph - A Simple Example

Let's create a very simple graph that has two nodes: `greeter` and `responder`.
The `greeter` node will add a greeting message to the state.
The `responder` node will add a response message based on the greeting.

Before you start, the shortest path is:

1. Make sure you are on Java 17+ and have added `langgraph4j-core` to your project.
2. Copy the example below exactly once to understand the basic `StateGraph` + shared-state flow.
3. Run the graph locally and confirm you can see the state evolve across the two nodes.
4. After that first success, jump to the built-in integrations or checkpointing sections depending on whether you want framework integration or persistence next.

**1. Define the State:**
Our state will hold a list of messages.

```java
// Define the state for our graph
class SimpleState extends AgentState {
    public static final String MESSAGES_KEY = "messages";

    // Define the schema for the state.
    // MESSAGES_KEY will hold a list of strings, and new messages will be appended.
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            MESSAGES_KEY, Channels.appender(ArrayList::new)
    );

    public SimpleState(Map<String, Object> initData) {
        super(initData);
    }

    public List<String> messages() {
        return this.<List<String>>value("messages")
                .orElse( List.of() );
    }
}
```

**2. Define the Nodes:**

```java
// Node that adds a greeting
class GreeterNode implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) {
        System.out.println("GreeterNode executing. Current messages: " + state.messages());
        return Map.of(SimpleState.MESSAGES_KEY, "Hello from GreeterNode!");
    }
}

// Node that adds a response
class ResponderNode implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) {
        System.out.println("ResponderNode executing. Current messages: " + state.messages());
        List<String> currentMessages = state.messages();
        if (currentMessages.contains("Hello from GreeterNode!")) {
            return Map.of(SimpleState.MESSAGES_KEY, "Acknowledged greeting!");
        }
        return Map.of(SimpleState.MESSAGES_KEY, "No greeting found.");
    }
}
```

**3. Define and Compile the Graph:**

```java
public class SimpleGraphApp {
    
    public static void main(String[] args) throws GraphStateException {
        // Initialize nodes
        GreeterNode greeterNode = new GreeterNode();
        ResponderNode responderNode = new ResponderNode();

        // Define the graph structure
       var stateGraph = new StateGraph<>(SimpleState.SCHEMA, initData -> new SimpleState(initData))
            .addNode("greeter", node_async(greeterNode))
            .addNode("responder", node_async(responderNode))
            // Define edges
            .addEdge(START, "greeter") // Start with the greeter node
            .addEdge("greeter", "responder")
            .addEdge("responder", END)   // End after the responder node
             ;
        // Compile the graph
        var compiledGraph = stateGraph.compile();

        // Run the graph

        compiledGraph.stream(inputs, config) // start streaming outputs from the graph execution
                .forEachAsync( output -> // process each output as it is produced
                    System.out.println("Step executed: " + output))
                .thenApply( GraphResult::from ) // holds final result in GraphResult
                .thenAccept( result -> { // process the final result
                    if (result.isStateDataOrCheckpointSaverTag()) {
                        System.out.println("Final state: " + result.asStateDataOrLastCheckpointStateData());
                    } else if (result.isInterruptionMetadata()) {
                        System.out.println("Graph interrupted: " + result.asInterruptionMetadata());
                    }
                });

    }
}
```

**Explanation:**

*   We defined `SimpleState` with a `MESSAGES_KEY` that uses `AppenderChannel` to accumulate strings.
*   `GreeterNode` adds a "Hello" message.
*   `ResponderNode` checks for the greeting and adds an acknowledgment.
*   The `StateGraph` is defined, nodes are added, and edges specify the flow: `START` -> `greeter` -> `responder` -> `END`.
*   `stateGraph.compile()` creates the runnable `CompiledGraph`.
*   `compiledGraph.stream(initialState)` executes the graph. We iterate through the stream to get the final state. Each item in the stream represents the state after a node has executed.

This example demonstrates the basic workflow: define state, define nodes, wire them with edges, compile, and run.

## Running Your Graph

As shown in the example, you typically run a compiled graph using one of its execution methods:

*   `stream(S initialState, RunnableConfig config)`: Executes the graph and returns an `AsyncGenerator<S>`. Each yielded item is the state `S` after a node has completed. This is useful for observing the state at each step or for streaming partial results.
*   `invoke(S initialState, RunnableConfig config)`: Executes the graph and returns a `CompletableFuture<S>` that completes with the final state of the graph after it reaches an `END` node.

The `RunnableConfig` can be used to pass in runtime configuration.

## Studio 🤩 - Running Your Graph visually

**Langgraph4j Studio** is an embeddable web application for visualizing and experimenting with graphs:

To explore the **Langgraph4j Studio** go to [studio]

## BONUS: built-in integrations

### Integration Paths

| Integration | Best for | Quick links |
|---|---|---|
| LangChain4j | Projects already using LangChain4j chat models, tools, and agent patterns | [`langchain4j/README.md`](langchain4j/README.md), [`langchain4j/langchain4j-agent/README.md`](langchain4j/langchain4j-agent/README.md) |
| Spring AI | Spring Boot and Spring-native MCP / agent applications | [`spring-ai/README.md`](spring-ai/README.md), [`spring-ai/spring-ai-agent/README.md`](spring-ai/spring-ai-agent/README.md) |

### LangChain4j 

As default use case to proof [LangChain4j] integration, We have implemented **AgentExecutor (aka ReACT Agent)** using LangGraph4j. In the [project's module][agent-executor], you can the complete working code with tests. Feel free to checkout and use it as a reference.
Below usage example of the `AgentExecutor`.

#### Define Tools

```java
public class TestTool {

    @Tool("tool for test AI agent executor")
    String execTest(@P("test message") String message) {
        return format( "test tool ('%s') executed with result 'OK'", message);
    }

    @Tool("return current number of system thread allocated by application")
    int threadCount() {
        return Thread.getAllStackTraces().size();
    }

}
```

#### Run Agent

```java

var model OllamaChatModel.builder()
                .modelName( <model name> )
                .baseUrl("http://localhost:11434")
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .logRequests(true)
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .build();

var agent = AgentExecutor.builder()
            .chatModel(model)
            .toolsFromObject(new TestTool())
            .build()
            .compile();

agent.stream( Map.of( "messages", "perform test twice and return number of current active threads" ) )
                .forEachAsync( item -> System.out.println( item ));

```

### Spring AI 

As default use case to proof [Spring AI] integration, We have implemented **AgentExecutor (aka ReACT Agent)** using LangGraph4j. In the [project's module][spring-ai-agent], you can the complete working code with tests. Feel free to checkout and use it as a reference.
Below usage example of the `AgentExecutor`.

#### Define Tools

```java
public class TestTool {

    @Tool(description = "tool for test AI agent executor")
    String execTest( @ToolParam(description ="test message") String message ) {
        return format( "test tool ('%s') executed with result 'OK'", message);
    }

    @Tool(description = "return current number of system thread allocated by application")
    int threadCount() {
        return Thread.getAllStackTraces().size();
    }

}
```

#### Run Agent

```java

var model = OllamaChatModel.builder()
                    .ollamaApi(OllamaApi.builder()
                            .baseUrl("http://localhost:11434")
                            .build())
                    .options(OllamaChatOptions.builder()
                            .model(model)
                            .temperature(0.0)
                            .build())
                    .build();

var agent = AgentExecutor.builder()
        .chatModel(model)
        .toolsFromObject(new TestTool())
        .build()
        .compile()
        ;

agent.stream( Map.of( "messages", "perform test twice and return number of current active threads" ) )
        .forEachAsync( item -> System.out.println( item ));
```

## Key Capabilities Overview

LangGraph4j is packed with features to build sophisticated agentic applications:

*   **Asynchronous Operations:** Nodes and edges can be asynchronous (returning `CompletableFuture`), allowing for non-blocking I/O operations, especially when dealing with LLM calls.
*   **Streaming:** Natively supports streaming responses from LLMs through nodes, enabling real-time output. See [how-tos/llm-streaming.ipynb].
*   **Checkpoints (Persistence & Time Travel):** Save and load the state of your graph. This allows you to resume long-running tasks, debug by inspecting intermediate states, and even "time travel" to previous states. See [how-tos/persistence.ipynb] and [how-tos/time-travel.ipynb].
*   **Graph Visualization:** Generate [PlantUML] or [Mermaid] diagrams of your graph to visualize its structure, which aids in understanding and debugging. See [how-tos/plantuml.ipynb].
*   **Playground & Studio:** LangGraph4j comes with an embeddable web UI (Studio) that allows you to visualize, run, and interact with your graphs in real-time. This is excellent for development and debugging.
*   **Child Graphs (Subgraphs):** Compose complex graphs by nesting smaller, reusable graphs within nodes of a parent graph. This promotes modularity and reusability. See [how-tos/subgraph-as-nodeaction.ipynb], [how-tos/subgraph-as-compiledgraph.ipynb], and [how-tos/subgraph-as-stategraph.ipynb].
*   **Parallel Execution:** Configure parts of your graph to execute multiple nodes in parallel, improving performance for tasks that can be run concurrently. See [how-tos/parallel-branch.ipynb].
*   **Threads (Multi-turn Conversations):** Manage distinct, parallel execution threads within a single graph instance, each with its own checkpoint history. This is vital for handling multiple user sessions or conversations simultaneously.

## Next Steps

Now that you have a basic understanding of LangGraph4j, here's how you can continue your journey:

*   **Explore the `how-to`:** The [`how-tos/`](how-tos) directory in the repository contains Jupyter notebooks (runnable with Java kernels like [IJava](https://github.com/SpencerPark/IJava)) that demonstrate various features with code examples.
*   **Study the Examples:** Check out the examples from [here](https://github.com/langgraph4j/langgraph4j-examples) for more complete application examples, including integrations with Langchain4j and Spring AI.
*   **Consult the Javadocs:** For detailed information on classes and methods, refer to the [API documentation (Javadocs)][javadocs]. *(Link might need updating if official project documentation site changes)*
*   **Experiment!** The best way to learn is by doing. Try modifying the examples or building your own simple graphs.

We hope this guide helps you get started with LangGraph4j. Happy building!

# References

## Videos

* [LangGraph4j live discussion with Johannes Rabauer](https://www.linkedin.com/events/yourtool-callingloopisn-tanagen7494246562873716736/theater/)

## Projects using LangGraph4j

* [LangGraph4j for AI Agents payments | @AgentPay](https://github.com/explorercosmic7-arch/langgraph4j-agentpay-example)
* [Agentic CI/CD Orchestrator](https://github.com/veerjain-1/agentic-cicd-orchestrator)
* [AI-Powered Hiring Workflow](https://github.com/ykpraveen/langchain4j-sample)
* [OpenHuskyAgent (_Agent Runtime Platform_)](https://github.com/HandleCoding/OpenHuskyAgent/tree/main)
* [spring-ai-alibaba (_use an embedded version in spring-ai-alibaba-graph-core_)](https://github.com/alibaba/spring-ai-alibaba/tree/main)
* [An ACP bridge to langraph4j and langchain4j](https://github.com/OsgiliathEnterprise/acp-langgraph-langchain-bridge)
* [A deep research assistant based on the Langgraph4j](https://github.com/imfangs/langgraph4j-deep-researcher)
* [research4j - Build your own perplexity for your applications](https://github.com/bhavuklabs/research4j)
* [Multi Agent Banking Assistant with Java using Langraph4j](https://github.com/Azure-Samples/agent-openai-java-banking-assistant-langgraph4j)
* [Java Async Generator, a Java version of Javascript async generator](https://github.com/bsorrentino/java-async-generator)
* [AIDEEPIN: Ai-based productivity tools (Chat,Draw,RAG,Workflow etc)](https://github.com/moyangzhan/langchain4j-aideepin)
* [Dynamo Multi AI Agent POC: Unlock the Power of Spring AI and LangGraph4J](https://github.com/Breezeware-OS/dynamo-multi-ai-agent-langgraph4j-starter)
* [LangChain4j & LangGraph4j Integrate LangFuse](https://github.com/Kugaaa/langchain4j-langfuse)


## Articles
* [Spring AI vs LangGraph4j: Java AI Framework Comparison](https://springdevpro.com/spring-ai/comparison/spring-ai-vs-langgraph4j/)
* [Building an Agentic Harness with Spring AI and LangGraph4j](https://dev.to/lbobylev/building-an-agentic-harness-with-spring-ai-2j1p)
* LangGraph4j Series
  * [Part 1: State Management, Channels Reducers and Human in the Loop](https://www.futurecraft.dev/ai/State-Management/)
  * [Part 2: Node Hooks, applyBefore, applyWrap, and applyAfter Explained](https://www.futurecraft.dev/ai/Node-Hooks-in-LangGraph4j/)
  * [Part 3: Framework-Agnostic Nodes — The Adapter Pattern, Explained](https://www.futurecraft.dev/ai/Framework-Agnostic-Nodes-in-LangGraph4j/)
* [LangGraph4j: 5 Practical Steps](https://www.linkedin.com/pulse/langgraph4j-5-practical-steps-vincent-vauban-uot1e)
* [Building a Multi-Agent Hiring Workflow with Langchain4j and Langgraph4j on Spring Boot](https://dev.to/ykpraveen/building-a-multi-agent-hiring-workflow-with-langchain4j-and-langgraph4j-on-spring-boot-2a15)
* [Spring AI Recipe: Building a Graph-Based Agentic Workflow with LangGraph4j](https://www.linkedin.com/pulse/spring-ai-recipe-building-graph-based-agentic-workflow-craig-walls-ztfqc/)
* [Skill-Based Sub-Agents with LangGraph4j and Spring AI](https://dev.to/bsorrentino/skill-based-sub-agents-with-langgraph4j-and-spring-ai-52b0)
* [Building AI-Powered Workflows in Java: A Hands-On Guide to Spring AI and LangGraph4j](https://medium.com/@kandisa.ganesh/building-ai-powered-workflows-in-java-a-hands-on-guide-to-spring-ai-and-langgraph4j-805464ab8a2e)
* [Beyond the Basics: Building Real-World AI Workflows with LangGraph4j](https://medium.com/@kandisa.ganesh/beyond-the-basics-building-real-world-ai-workflows-with-langgraph4j-8e2f7765c505)
* [LangGraph4j Hooks and OpenTelemetry](https://medium.com/dev-genius/langgraph4j-hooks-and-opentelemetry-3bd8de02f9d1)
* [LangGraph4j Deep Agents (Agent 2.0)](https://bsorrentino.github.io/bsorrentino/ai/2025/10/17/langgraph4j-deepagents.html)
* [LangGraph4j Meets AG-UI - Building UI/UX in AI Agents era](https://bsorrentino.github.io/bsorrentino/ai/2025/08/21/LangGraph4j-meets-AG-UI.html)
* [LangGraph4j - Implementing Human-in-the-Loop at ease](https://bsorrentino.github.io/bsorrentino/ai/2025/07/13/LangGraph4j-Agent-with-approval.html)
* [LangGraph4j - Multi-Agent handoff implementation with Spring AI](https://bsorrentino.github.io/bsorrentino/ai/2025/05/10/Langgraph4j-agent-handoff.html)
* [Microsoft JDConf 2025 - AI Agents Graph: Your following tool in your Java AI journey](https://youtu.be/Sp36wdpobpI?si=dXDTD9k16hM8A8KQ)
* [Enhancing AI Agent Development: A Hands-On Weekend with LangGraph4J](https://www.linkedin.com/pulse/enhancing-ai-agent-development-hands-on-weekend-langgraph4j-chung-ha-iu3be/)
* [LangGraph4j Generator - Visually scaffold LangGraph Java code](https://dev.to/bsorrentino/langgraph4j-generator-2ika)
* [AI Agent in Java with LangGraph4j](https://bsorrentino.github.io/bsorrentino/ai/2024/05/20/langgraph-for-java.html)
* [Building Stateful Multi AI Agents -LangGraph4J & Spring AI](https://medium.com/@ganeshmoorthy5999/building-stateful-multi-ai-agents-langgraph4j-spring-ai-c0046e293d00)

## Useful links

* [Shypd (_The largest open index of AI tools_)](https://shypd.ai/tools/langgraph4j)
* [DeepWiki](https://deepwiki.com/langgraph4j/langgraph4j)
* [Content7](https://context7.com/langgraph4j/langgraph4j)
* [olud.ai (_The AI hub_)](https://olud.ai/project/langgraph4j-langgraph4j.html)

## Books

* [Applied AI for Enterprise Java Development(Red Hat Developer e-books)](https://developers.redhat.com/e-books/applied-ai-enterprise-java-development)

<!--
* [LangGraph - LangChain Blog][langgraph.blog]
-->

[LangChain AI project]: https://github.com/langchain-ai
[langchain4j]: https://github.com/langchain4j/langchain4j
[Spring AI]: https://spring.io/projects/spring-ai
[langgraph]: https://github.com/langchain-ai/langgraph
[documentation]: https://langgraph4j.github.io/langgraph4j/
[releases]: https://central.sonatype.com/search?q=a%3Alanggraph4j-parent
[snapshots]: https://central.sonatype.com/repository/maven-snapshots/org/bsc/langgraph4j/
[PlantUML]: https://plantuml.com
[Mermaid]: https://mermaid.js.org
[documentation]: https://langgraph4j.github.io/langgraph4j/
[javadocs]: https://langgraph4j.github.io/langgraph4j/apidocs/
[springai-agentexecutor]: spring-ai/spring-ai-agent
[agent-executor]: langchain4j/langchain4j-agent
[spring-ai-agent]: spring-ai/spring-ai-agent
[Studio]: studio/README.md
[Jetty]: https://jetty.org
[Spring Boot]: https://spring.io/projects/spring-boot
[Quarkus]: https://quarkus.io
[CompletableFuture]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
[article01]: https://bsorrentino.github.io/bsorrentino/ai/2024/05/20/langgraph-for-java.html
[langgraph.blog]: https://blog.langchain.dev/langgraph/
[langchain]: https://github.com/langchain-ai/langchain/
[langgraph]: https://github.com/langchain-ai/langgraph
[langchain.agents]: https://python.langchain.com/docs/modules/agents/
[AgentExecutor]: https://github.com/langchain-ai/langchain/blob/master/libs/langchain/langchain/agents/agent.py
[java-async-generator]: https://github.com/bsorrentino/java-async-generator

[how-tos/plantuml.ipynb]: how-tos/plantuml.ipynb
[how-tos/persistence.ipynb]: how-tos/persistence.ipynb
[how-tos/llm-streaming.ipynb]: how-tos/llm-streaming.ipynb
[how-tos/time-travel.ipynb]: how-tos/time-travel.ipynb
[how-tos/subgraph-as-nodeaction.ipynb]: how-tos/subgraph-as-nodeaction.ipynb
[how-tos/subgraph-as-compiledgraph.ipynb]: how-tos/subgraph-as-compiledgraph.ipynb
[how-tos/subgraph-as-stategraph.ipynb]: how-tos/subgraph-as-stategraph.ipynb
