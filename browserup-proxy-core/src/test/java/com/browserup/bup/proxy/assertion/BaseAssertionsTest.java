package com.browserup.bup.proxy.assertion;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BaseAssertionsTest extends MockServerTest {
    public void requestToMockedServer(String url, String response) throws IOException {
        String respBody = NewProxyServerTestUtil.toStringAndClose(clientToProxy.execute(new HttpGet(mockedServerUrl + "/" + url)).getEntity().getContent());
        Assertions.assertEquals(response, respBody, "Did not receive expected response from mock server");
    }

    public void requestToMockedServer(String url) throws IOException {
        requestToMockedServer(url, SUCCESSFUL_RESPONSE_BODY);
    }

    @BeforeEach
    public void startUp() {
        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();
        proxy.newHar();

        clientToProxy = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort());
        mockedServerUrl = "http://localhost:" + mockServerPort;
        url = mockedServerUrl + "/" + URL_PATH;
    }

    @AfterEach
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    protected StubMapping mockResponseForPathWithDelay(String path, int delayMilliseconds) {
        return WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/" + path)).willReturn(WireMock.ok().withFixedDelay(delayMilliseconds).withBody(SUCCESSFUL_RESPONSE_BODY)));
    }

    public static void assertAssertionPassed(AssertionResult assertion) {
        assertTrue(assertion.getPassed(), "Expected assertion to pass");
        assertFalse(assertion.getFailed(), "Expected assertion to pass");
    }

    public static void assertAssertionFailed(AssertionResult assertion) {
        assertFalse(assertion.getPassed(), "Expected assertion to fail");
        assertTrue(assertion.getFailed(), "Expected assertion to fail");
    }

    public static void assertAssertionHasNoEntries(AssertionResult assertion) {
        assertThat("Expected assertion result has no entries", assertion.getRequests(), hasSize(0));
    }

    protected static final String SUCCESSFUL_RESPONSE_BODY = "success";
    protected static final String URL_PATH = "some-url";
    protected static final int DEFAULT_RESPONSE_DELAY = 2000;
    protected static final int FAST_RESPONSE_DELAY = 1000;
    protected static final int TIME_DELTA_MILLISECONDS = 500;
    protected String url;
    protected String mockedServerUrl;
    protected BrowserUpProxy proxy;
    protected CloseableHttpClient clientToProxy;
}
