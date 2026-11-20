# DynamoDB 检查点父子链

测试使用本地安装的 `cn.iocoder.langgraph4j:langgraph4j-dynamodb-saver:1.9.9-jdk17-SNAPSHOT` 与同版本的 core。每项测试启动真实 Spring Boot 上下文，连接独立的 DynamoDB Local，并直接核对持久化的 `parentCheckpointId`。

测试覆盖多轮执行、新建 DynamoDB 客户端后继续中断的图、从旧检查点分叉后分别读取两条分支。

```bash
export JAVA_HOME=/path/to/jdk17
mvn -pl :langgraph4j-bom,:langgraph4j-dynamodb-saver -am -DskipTests install
docker run -d --name langgraph-lineage-test -p 127.0.0.1::8000 \
  amazon/dynamodb-local:latest -jar DynamoDBLocal.jar -inMemory -sharedDb
endpoint="http://$(docker port langgraph-lineage-test 8000/tcp)"
mvn -f compatibility-tests/dynamodb-lineage/pom.xml clean test \
  "-Dlineage.endpoint=$endpoint" -Dlineage.boot=3.5.15 -Dlineage.jdk=17 \
  -Dcompat.test.jvm=/path/to/jdk17/bin/java
docker rm -fv langgraph-lineage-test
```

使用 JDK 25 复验时，设置对应 Java 路径和 `-Dlineage.jdk=25`。
