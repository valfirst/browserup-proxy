package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.auth.AuthType;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import de.sstoehr.harreader.model.Har;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.net.InetSocketAddress;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class ChainedProxyAuthTest extends MockServerTest {
    private MitmProxyServer proxy;

    private MitmProxyServer upstreamMitmProxy;

    private HttpProxyServer upstreamProxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
        if (upstreamMitmProxy != null) {
            upstreamMitmProxy.abort();
        }
        if (upstreamProxy != null) {
            upstreamProxy.abort();
        }
    }

    @Test
    public void testUpstreamProxyIsDown() throws Exception {
        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withPort(0)
                .start();

        String stubUrl = "/proxyauth";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress());
        proxy.setTrustAllServers(true);
        proxy.start();
        upstreamProxy.stop();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse result = client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/proxyauth"));
            assertEquals("Did not receive 502 BAD GATEWAY from mitmproxy", 502, result.getStatusLine().getStatusCode());
        }

        verify(0, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testMitmproxyUsesUpstreamProxy() throws Exception {
        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withPort(0)
                .start();

        String stubUrl = "/proxyauth";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress());
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/proxyauth")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testUpstreamAndDownstreamProxiesGetRequestIfNonProxyHostDoNotMatch() throws Exception {
        upstreamMitmProxy = new MitmProxyServer();
        upstreamMitmProxy.setTrustAllServers(true);
        upstreamMitmProxy.start();

        String stubUrl = "/proxyauth";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();

        proxy.setChainedProxy(new InetSocketAddress("localhost", upstreamMitmProxy.getPort()));
        proxy.setChainedProxyHTTPS(true);
        proxy.setTrustAllServers(true);
        proxy.setChainedProxyNonProxyHosts(Arrays.asList("bbc.com"));
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/proxyauth")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Har downStreamHar = proxy.getHar();
        Har upStreamHar = upstreamMitmProxy.getHar();

        assertEquals("Expected to get exactly one entry in har from downstream proxy", 1, downStreamHar.getLog().getEntries().size());
        assertEquals("Expected to get exactly one entry in har from upstream proxy", 1, upStreamHar.getLog().getEntries().size());

        assertEquals("Expected to get the same request URL in entries from downstream and upstream proxies",
                downStreamHar.getLog().getEntries().get(0).getRequest().getUrl(),
                upStreamHar.getLog().getEntries().get(0).getRequest().getUrl());

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    @Ignore
    public void testUpstreamProxyDoesNotGetRequestIfNonProxyHostMatch() throws Exception {
        String stubUrl = "/proxyauth";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        upstreamMitmProxy = new MitmProxyServer();
        upstreamMitmProxy.setTrustAllServers(true);
        upstreamMitmProxy.start();

        proxy = new MitmProxyServer();

        proxy.setChainedProxy(new InetSocketAddress("localhost", upstreamMitmProxy.getPort()));
        proxy.setChainedProxyHTTPS(true);
        proxy.setTrustAllServers(true);
        proxy.setChainedProxyNonProxyHosts(Arrays.asList("*localhost*"));
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/proxyauth")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Har downStreamHar = proxy.getHar();
        Har upStreamHar = upstreamMitmProxy.getHar();

        assertEquals("Expected to get exactly one entry in har from downstream proxy", 1, downStreamHar.getLog().getEntries().size());
        assertEquals("Expected to get exactly no entries in har from upstream proxy", 0, upStreamHar.getLog().getEntries().size());

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testMitmproxyUsesHttpsUpstreamProxy() throws Exception {
        upstreamMitmProxy = new MitmProxyServer();
        upstreamMitmProxy.setTrustAllServers(true);
        upstreamMitmProxy.start();

        String stubUrl = "/proxyauth";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();

        proxy.setChainedProxy(new InetSocketAddress("localhost", upstreamMitmProxy.getPort()));
        proxy.setChainedProxyHTTPS(true);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/proxyauth")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testAutoProxyAuthSuccessful() throws Exception {
        String proxyUser = "proxyuser";
        String proxyPassword = "proxypassword";

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withProxyAuthenticator(new ProxyAuthenticator() {
                    @Override
                    public boolean authenticate(String user, String password) {
                        return proxyUser.equals(user) && proxyPassword.equals(password);
                    }

                    @Override
                    public String getRealm() {
                        return "some-realm";
                    }
                })
                .withPort(0)
                .start();

        String stubUrl = "/proxyauth";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress());
        proxy.setTrustAllServers(true);
        proxy.chainedProxyAuthorization(proxyUser, proxyPassword, AuthType.BASIC);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/proxyauth")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Ignore("To investigate: whether HTTP status should be 407 or 502")
    @Test
    public void testAutoProxyAuthFailure() throws Exception {
        String proxyUser = "proxyuser";
        String proxyPassword = "proxypassword";

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withProxyAuthenticator(new ProxyAuthenticator() {
                    @Override
                    public boolean authenticate(String user, String password) {
                        return proxyUser.equals(user) && proxyPassword.equals(password);
                    }

                    @Override
                    public String getRealm() {
                        return "some-realm";
                    }
                })
                .withPort(0)
                .start();

        String stubUrl = "/proxyauth";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withStatus(500).withBody("shouldn't happen")));

        proxy = new MitmProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress());
        proxy.chainedProxyAuthorization(proxyUser, "wrongpassword", AuthType.BASIC);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/proxyauth"));
            assertEquals("Expected to receive a Bad Gateway due to incorrect proxy authentication credentials", 502, response.getStatusLine().getStatusCode());
        }

        verify(lessThan(1), getRequestedFor(urlEqualTo(stubUrl)));
    }
}
