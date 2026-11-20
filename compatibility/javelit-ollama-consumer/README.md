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

## 浏览器复验

本目录提供独立的嵌入式 Javelit 页面，运行在 Boot 2.7.18 和原生 JDK 8。页面使用 Javelit 的模型选择组件、LangGraph4j AgentExecutor 和 Ollama 原生聊天 API。

让 Ollama 在本机 `127.0.0.1:11434` 提供上文两个模型。为验证浏览器确实切换了模型，可在 Ollama 所在环境中先执行 `ollama cp qwen3:0.6b qwen3:alternate`。

在 LangGraph4j 仓库根目录执行以下命令。启动 Maven 时保留 JDK 17，`JAVA8_HOME` 指向 JDK 8 安装目录。

```bash
cd compatibility/javelit-ollama-consumer
mvn test-compile dependency:build-classpath -Dmdep.outputFile=target/browser-classpath
"$JAVA8_HOME/bin/java" -cp "target/test-classes:$(cat target/browser-classpath)" \
  org.bsc.javelit.JavelitOllamaBrowserApp
```

1. 用 Chrome 打开 [本地 Javelit 页面](http://127.0.0.1:18501/)，在 Local 下选择 `ollama:qwen3:0.6b`，确认 **Selected model** 显示 `qwen3:0.6b`。
2. 输入 `Call get_code to find the code, then report that code.`，按 Tab 提交输入状态，再单击 **Send**。
3. 确认页面显示 `get_code executed 1 time(s)`，且 **Final answer** 包含 `RAIN42`。

如页面端口已占用，可在 `java` 命令中加 `-Djavelit.browser.port=<端口>`。
