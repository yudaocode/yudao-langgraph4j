package org.bsc.langgraph4j.studio.jetty;

import java.util.concurrent.TimeUnit;
import org.bsc.langgraph4j.studio.StudioHttpFixture;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

/**
 * @author 芋道源码
 */
class StudioJettyJava8Test {

    @Test
    void servesUiAndStreamsGraphOnJetty() throws Exception {
        LangGraphStudioServer4Jetty studio = LangGraphStudioServer4Jetty.builder()
                .port(0)
                .instance("echo", StudioHttpFixture.graph())
                .build();
        ServerConnector connector = (ServerConnector) studio.server.getConnectors()[0];
        connector.setHost("127.0.0.1");
        try {
            studio.start().get(10, TimeUnit.SECONDS);
            StudioHttpFixture.assertStudioHttp(connector.getLocalPort());
        } finally {
            studio.server.stop();
        }
    }
}
