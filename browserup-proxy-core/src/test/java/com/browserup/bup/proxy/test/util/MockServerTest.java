package com.browserup.bup.proxy.test.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Tests can subclass this to get access to a ClientAndServer instance for creating mock responses.
 */
public class MockServerTest {
    protected int mockServerPort;
    protected int mockServerHttpsPort;

    protected WireMockServer wireMockRule;

    @BeforeEach
    protected void setUpMockServer() {
        wireMockRule = new WireMockServer(options().port(0).httpsPort(0));
        wireMockRule.start();
        WireMock.configureFor("localhost", wireMockRule.port());
        mockServerPort = wireMockRule.port();
        mockServerHttpsPort = wireMockRule.httpsPort();
    }

    @AfterEach
    protected void tearDownMockServer() {
        if (wireMockRule != null && wireMockRule.isRunning()) {
            wireMockRule.stop();
        }
    }
}
