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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class BaseAssertionsTest extends MockServerTest {
    public void requestToMockedServer(String url, String response) throws IOException {
        String respBody = NewProxyServerTestUtil.toStringAndClose(clientToProxy.execute(new HttpGet(mockedServerUrl + "/" + url)).getEntity().getContent());
        Assert.assertEquals("Did not receive expected response from mock server", response, respBody);
    }

    public void requestToMockedServer(String url) throws IOException {
        requestToMockedServer(url, SUCCESSFUL_RESPONSE_BODY);
    }

    @Before
    public void startUp() {
        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();
        proxy.newHar();

        clientToProxy = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort());
        mockedServerUrl = "http://localhost:" + mockServerPort;
        url = mockedServerUrl + "/" + URL_PATH;
    }

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    protected StubMapping mockResponseForPathWithDelay(String path, int delayMilliseconds) {
        return WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/" + path)).willReturn(WireMock.ok().withFixedDelay(delayMilliseconds).withBody(SUCCESSFUL_RESPONSE_BODY)));
    }

    public static void assertAssertionPassed(AssertionResult assertion) {
        assertTrue("Expected assertion to pass", assertion.getPassed());
        assertFalse("Expected assertion to pass", assertion.getFailed());
    }

    public static void assertAssertionFailed(AssertionResult assertion) {
        assertFalse("Expected assertion to fail", assertion.getPassed());
        assertTrue("Expected assertion to fail", assertion.getFailed());
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
