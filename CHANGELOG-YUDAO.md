# 芋道兼容版变更记录

## 1.9.10-jdk17-preview-20261120

- Boot 3 和 4 的默认构建纳入芋道 LangChain4j adapter 和 AgentExecutor，使用 `cn.iocoder.langchain4j` 的 1.25.0 源码线。
- 独立消费者在 JDK 17、25 验证双 adapter 共用一份 graph core、聊天消息检查点序列化和连续两轮工具调用。

适用于 Boot 3.5.15 和 JDK 17／25。本版依赖已发布的 Spring AI `2.1.0-jdk17-preview-20261118` 和 LangChain4j `1.25.0-preview-20261120`。

上游提交 `f36ee0679`，兼容分支源码 `5b36688ce`。
