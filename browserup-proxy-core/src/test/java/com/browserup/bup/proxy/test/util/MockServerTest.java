package com.browserup.bup.proxy.test.util;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Tests can subclass this to get access to a ClientAndServer instance for creating mock responses.
 */
public class MockServerTest {
    protected int mockServerPort;
    protected int mockServerHttpsPort;

    @RegisterExtension
    WireMockExtension wireMockRule = WireMockExtension.newInstance()
            .options(options().port(0).httpsPort(0))
            .build();

    @BeforeEach
    public void setUpMockServer() {
        mockServerPort = wireMockRule.getPort();
        mockServerHttpsPort = wireMockRule.getHttpsPort();
    }
}
