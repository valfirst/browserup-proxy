package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNoException;

public class BindAddressTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testClientBindAddress() throws Exception {
        String stubUrl = "/clientbind";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        // bind to loopback. ProxyServerTest.getNewHtpClient creates an HTTP client that connects to a proxy at 127.0.0.1
        proxy = new BrowserUpProxyServer();
        proxy.start(0, InetAddress.getLoopbackAddress());

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/clientbind"));
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test(expected = HttpHostConnectException.class)
    public void testClientBindAddressCannotConnect() throws Exception {
        String stubUrl = "/clientbind";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        // find the local host address to bind to that isn't loopback. since ProxyServerTest.getNewHtpClient creates an HTTP client that
        // connects to a proxy at 127.0.0.1, the HTTP client should *not* be able to connect to the proxy
        InetAddress localHostAddr;
        try {
            localHostAddr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            assumeNoException("Could not get a localhost address. Skipping test.", e);
            return;
        }

        proxy = new BrowserUpProxyServer();
        proxy.start(0, localHostAddr);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            httpClient.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/clientbind"));
        }
    }

    @Test
    public void testServerBindAddress() throws Exception {
        String stubUrl = "/serverbind";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        // bind outgoing traffic to loopback. since the mockserver is running on localhost with a wildcard address, this should succeed.
        proxy = new BrowserUpProxyServer();
        proxy.start(0, null, InetAddress.getLoopbackAddress());

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/serverbind"));
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testServerBindAddressCannotConnect() throws Exception {
        // bind outgoing traffic to loopback. since loopback cannot reach external addresses, this should fail.
        proxy = new BrowserUpProxyServer();
        proxy.start(0, null, InetAddress.getLoopbackAddress());

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://www.google.com"));
            assertEquals("Expected a 502 Bad Gateway when connecting to an external address after binding to loopback", 502, response.getStatusLine().getStatusCode());
        }
    }
}
