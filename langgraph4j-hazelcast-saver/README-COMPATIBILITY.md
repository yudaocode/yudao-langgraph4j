# Hazelcast Saver 的 Docker 验证

在 `main-jdk8` 上，同一套 Saver 契约用例可使用嵌入式 Hazelcast 或真实的远程客户端。以下示例用原生 JDK 8 客户端访问 Docker 里的 Hazelcast Community 5.2.5。测试使用随机隔离的 map。

```bash
hazelcast_jar="$HOME/.m2/repository/com/hazelcast/hazelcast/5.2.5/hazelcast-5.2.5.jar"
docker run -d --name langgraph-hazelcast-it -p 127.0.0.1::5701 --cpus 2 \
  --mount "type=bind,src=$hazelcast_jar,dst=/app/hazelcast.jar,readonly" \
  --mount "type=bind,src=$PWD/langgraph4j-hazelcast-saver/src/test/resources/hazelcast-docker.yaml,dst=/app/hazelcast.yaml,readonly" \
  amazoncorretto:8 java -Xms64m -Xmx256m -Dhazelcast.config=/app/hazelcast.yaml \
  -cp /app/hazelcast.jar com.hazelcast.core.server.HazelcastMemberStarter
mvn -pl :langgraph4j-hazelcast-saver -am -Dtest=HazelcastSaverTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dcompat.test.jvm=/path/to/jdk8/bin/java \
  "-Dlanggraph4j.hazelcast.test.address=$(docker port langgraph-hazelcast-it 5701/tcp)" test
docker rm -fv langgraph-hazelcast-it
```

用 JDK 17 运行 Maven。Hazelcast 5.2.5 JAR 可通过仓库构建下载。真实客户端验证 `IMap` 的检查点写入、恢复、释放、二进制及 JSON 序列化。`CP_MAP` 使用现代 Hazelcast Enterprise 路线。
