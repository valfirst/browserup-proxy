package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.auth.AuthType;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class AutoAuthTest extends MockServerTest {
    private MitmProxyServer proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testBasicAuthAddedToHttpRequest() throws Exception {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        String stubUrl = "/basicAuthHttp";

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Authorization", new EqualToPattern("Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA=="))
                .willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("http://localhost:" + mockServerPort + "/basicAuthHttp")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testBasicAuthAddedToHttpsRequest() throws Exception {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        String stubUrl = "/basicAuthHttp";

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Authorization", new EqualToPattern("Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA=="))
                .willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/basicAuthHttp")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testCanStopBasicAuth() throws Exception {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        String stubUrl = "/basicAuthHttp";

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Authorization", AbsentPattern.ABSENT)
                .willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC);
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.stopAutoAuthorization("localhost");

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("http://localhost:" + mockServerPort + "/basicAuthHttp")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }
}
