## 生成 Spring AI Agent 工程（Boot 2 / JDK 8）

本地 `install` 兼容分支后，用 Maven Archetype 生成自己的工程。

```bash
mvn org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=cn.iocoder.langgraph4j \
  -DarchetypeArtifactId=spring-ai-agent-archetype \
  -DarchetypeVersion=1.9.9-jdk8-SNAPSHOT \
  -DgroupId=com.example -DartifactId=agent-demo -Dpackage=com.example.agent \
  -Dversion=1.0.0-SNAPSHOT -DinteractiveMode=false
```

生成工程用 JDK 17 构建并输出 Java 8 字节码。使用 JDK 8 安装目录下的 `bin/java` 作为 Surefire 的 `-Djvm` 参数，可在目标 JVM 上运行生成工程的测试。内置测试通过真实 Boot 2.7.18 上下文执行生成的 `AgentExecutor`。已有模型示例和 Javelit 演示各自依赖相应运行环境。

Java 8 默认构建共 30 个模块。Javelit 独立 UI 依赖 `io.javelit:javelit:0.89.0`，运行该 UI 需 Java 21 或以上。

---

# Spring AI Agent Archetype

This Maven archetype creates a new Spring Boot project configured to work with LangGraph4j, providing a starting point for building AI agents powered by Spring AI.

## Usage

To generate a new project from this archetype, run the following Maven command in your terminal.

### Non-Interactive Mode

This command generates the project with predefined properties.

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.bsc.langgraph4j \
  -DarchetypeArtifactId=spring-ai-agent-archetype \
  -DarchetypeVersion=1.9.9
```

**Parameters:**

*   `archetypeGroupId`: The group ID of the archetype (`org.bsc.langgraph4j`).
*   `archetypeArtifactId`: The artifact ID of the archetype (`spring-ai-agent-archetype`).
*   `archetypeVersion`: The version of the archetype.
*   `groupId`: The group ID for your new project (e.g., `com.example`).
*   `artifactId`: The artifact ID for your new project (e.g., `my-spring-ai-agent`).
*   `version`: The version for your new project (e.g., `1.0.0`).
*   `package`: The package name for your new project (e.g., `com.example.mypackage`).`

### Interactive Mode

Alternatively, you can run the command in interactive mode and provide the values when prompted.

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.bsc.langgraph4j \
  -DarchetypeArtifactId=spring-ai-agent-archetype \
  -DarchetypeVersion=1.9.9
```

Maven will then prompt you to enter the `groupId`, `artifactId`, `version`, and `package` for your new project.

## Generated Project

After running the command, a new directory with the name of your `artifactId` (e.g., `my-spring-ai-agent`) will be created. This project contains a sample Spring Boot application demonstrating how to create a ReAct-style agent using LangGraph4j and Spring AI, complete with example tools and configurations for different AI models.

```
