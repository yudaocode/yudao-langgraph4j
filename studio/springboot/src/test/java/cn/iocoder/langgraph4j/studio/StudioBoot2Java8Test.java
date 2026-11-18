package cn.iocoder.langgraph4j.studio;

import java.util.Collections;
import java.util.Map;
import org.bsc.langgraph4j.agui.sdk.AGUIAgentRegistry;
import org.bsc.langgraph4j.studio.LangGraphStudioServer;
import org.bsc.langgraph4j.studio.StudioHttpFixture;
import org.bsc.langgraph4j.studio.springboot.LangGraphStudioConfig;
import org.bsc.langgraph4j.studio.springboot.agui.AGUILangGraphStudioBaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author 芋道源码
 */
class StudioBoot2Java8Test {

    @Test
    void servesUiAndStreamsGraphOnBoot2() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .properties("server.port=0", "server.address=127.0.0.1", "spring.main.banner-mode=off").run()) {
            int port = Integer.parseInt(context.getEnvironment().getRequiredProperty("local.server.port"));
            StudioHttpFixture.assertStudioHttp(port);
        }
    }

    @Test
    void servesAgUiAndStreamsGraphOnBoot2() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AgUiApplication.class)
                .properties("server.port=0", "server.address=127.0.0.1", "spring.main.banner-mode=off",
                        "spring.web.resources.static-locations=classpath:/agui/static/").run()) {
            int port = Integer.parseInt(context.getEnvironment().getRequiredProperty("local.server.port"));
            StudioHttpFixture.assertAgUiStudioHttp(port);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application extends LangGraphStudioConfig {

        @Override
        public Map<String, LangGraphStudioServer.Instance> instanceMap() {
            try {
                return Collections.singletonMap("echo", StudioHttpFixture.graph());
            } catch (Exception failure) {
                throw new IllegalStateException("Unable to compile test graph", failure);
            }
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class AgUiApplication extends AGUILangGraphStudioBaseConfig {

        @Override
        @Bean
        protected AGUIAgentRegistry agentRegistry() throws Exception {
            return new AGUIAgentRegistry(StudioHttpFixture.agent());
        }
    }
}
