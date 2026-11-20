# Javelit 与本地 Ollama

独立工程从已安装的 LangGraph4j 与 LangChain4j JAR 启动 Boot 2 / JDK 8 上下文。用例检查 Javelit 目录只提供可调用工具的模型，再从该目录取出 `qwen3:0.6b`，交给 LangGraph4j 的 LangChain4j AgentExecutor，执行本地 `get_code` 工具并验证模型使用了工具结果。

先准备可访问的本地 Ollama 服务，拉取 `qwen3:0.6b` 与 `all-minilm:latest`。本次使用 Ollama 0.40.9。拉取模型可在 Ollama 所在容器内执行。

```bash
ollama pull qwen3:0.6b
ollama pull all-minilm:latest
```

先从芋道 LangChain4j 的 `main-jdk8` 仓库根目录，以 JDK 17 启动 Maven 并安装 Ollama 适配器。使用包含外部包 builder 修复的版本。

```bash
export JAVA_HOME=/path/to/jdk17
mvn -pl :langchain4j-ollama -am -DskipTests -Dmaven.javadoc.skip=true install
```

再从 LangGraph4j 的 `main-jdk8` 仓库根目录执行以下命令。Ollama 端点以斜杠结尾。

```bash
mvn -pl :langgraph4j-bom,:langgraph4j-javelit,:langgraph4j-langchain4j-agentexecutor \
  -am -DskipTests -Dmaven.javadoc.skip=true install
mvn -f compatibility/javelit-ollama-consumer/pom.xml clean test \
  -Dcompat.test.jvm=/path/to/jdk8/bin/java \
  -Djavelit.ollama.endpoint=http://127.0.0.1:11434/ \
  -Djavelit.ollama.tool-model=qwen3:0.6b \
  -Djavelit.ollama.embedding-model=all-minilm:latest
```

测试覆盖模型目录与图执行链。浏览器中的交互式 Javelit 页面需要按示例单独运行。
