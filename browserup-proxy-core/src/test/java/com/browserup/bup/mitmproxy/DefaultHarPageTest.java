package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultHarPageTest extends MockServerTest {
    private static final String SUCCESSFUL_RESPONSE_BODY = "success";
    private static final String FIRST_URL = "first-url";

    private MitmProxyServer proxy;
    private CloseableHttpClient clientToProxy;

    @BeforeEach
    public void startUp() {
        proxy = new MitmProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        clientToProxy = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort());
    }

    @AfterEach
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void defaultPageIsCreatedForSuccessfulRequest() throws Exception {
        mockResponseForPath(FIRST_URL);

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals(SUCCESSFUL_RESPONSE_BODY, respBody, "Did not receive expected response from mock server");
        assertEquals("Default", proxy.getHar().getLog().getEntries().get(0).getPageref(), "Expected first request entry to have initial page ref");
        assertThat("Expected 1 page available", proxy.getHar().getLog().getPages(), Matchers.hasSize(1));
    }

    @Test
    public void defaultPageIsCreatedForHarEntryIfConnectionFailed() throws Exception {
        mockResponseForPath(FIRST_URL);
        int nonResponsivePort = mockServerPort + 1;

        String firstUrl = "http://localhost:" + nonResponsivePort + "/" + FIRST_URL;

        toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals("Default", proxy.getHar().getLog().getEntries().get(0).getPageref(), "Expected first request entry to have initial page ref");
        assertThat("Expected 1 page available", proxy.getHar().getLog().getPages(), Matchers.hasSize(1));
    }

    private void mockResponseForPath(String path) {
        stubFor(get(urlEqualTo("/" + path)).willReturn(ok().withBody(SUCCESSFUL_RESPONSE_BODY)));
    }
}
