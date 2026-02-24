package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;

/**
 * Tests host remapping using the {@link com.browserup.bup.proxy.dns.AdvancedHostResolver#remapHost(java.lang.String, java.lang.String)}
 * and related methods exposes by {@link BrowserUpProxy#getHostNameResolver()}.
 */
public class RemapHostsTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testRemapHttpHost() throws Exception {
        // mock up a response to serve

        String stubUrl = "/remapHttpHost";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);

        proxy.getHostNameResolver().remapHost("www.someaddress.notreal", "localhost");

        proxy.start();

        int proxyPort = proxy.getPort();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://www.someaddress.notreal:" + mockServerPort + "/remapHttpHost")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testRemapHttpsHost() throws Exception {
        // mock up a response to serve
        String stubUrl = "/remapHttpsHost";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);

        proxy.getHostNameResolver().remapHost("www.someaddress.notreal", "localhost");

        proxy.start();

        int proxyPort = proxy.getPort();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("https://www.someaddress.notreal:" + mockServerHttpsPort + "/remapHttpsHost")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }
}
