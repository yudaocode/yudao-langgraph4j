# Javelit 与本地 Ollama

测试从安装后的 LangGraph4j 制品启动 Boot 2 / JDK 8 上下文，调用 Javelit 的 Ollama 目录查询。Docker 中的 Ollama 需同时存在一个支持 `tools` 的模型和一个只有 `embedding` 的模型。测试确认两者均在目录中，并验证筛选结果。本轮使用 Ollama 0.40.9、`qwen3.8:latest` 和 `all-minilm:latest`。

```bash
export JAVA_HOME=/path/to/jdk17
mvn -pl :langgraph4j-bom,:langgraph4j-javelit -am -DskipTests install
mvn -f compatibility/javelit-ollama-consumer/pom.xml clean test \
  -Dcompat.test.jvm=/path/to/jdk8/bin/java \
  -Djavelit.ollama.endpoint=http://127.0.0.1:11434/ \
  -Djavelit.ollama.tool-model=qwen3.8:latest \
  -Djavelit.ollama.embedding-model=all-minilm:latest
```

端点以斜杠结尾。模型通过 Ollama 的 `/api/tags` 和 `/api/show` 查询。
