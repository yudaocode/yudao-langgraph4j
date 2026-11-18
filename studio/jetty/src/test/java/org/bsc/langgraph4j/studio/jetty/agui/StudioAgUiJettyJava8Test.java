package org.bsc.langgraph4j.studio.jetty.agui;

import java.util.concurrent.TimeUnit;
import org.bsc.langgraph4j.studio.StudioHttpFixture;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

/**
 * @author 芋道源码
 */
class StudioAgUiJettyJava8Test {

    @Test
    void servesAgUiAndStreamsGraphOnJetty() throws Exception {
        AGUILangGraphStudioServer4Jetty studio = AGUILangGraphStudioServer4Jetty.builder()
                .port(0).agent(StudioHttpFixture.agent()).build();
        ServerConnector connector = (ServerConnector) studio.server.getConnectors()[0];
        connector.setHost("127.0.0.1");
        try {
            studio.start().get(10, TimeUnit.SECONDS);
            StudioHttpFixture.assertAgUiStudioHttp(connector.getLocalPort());
        } finally {
            studio.server.stop();
        }
    }
}
