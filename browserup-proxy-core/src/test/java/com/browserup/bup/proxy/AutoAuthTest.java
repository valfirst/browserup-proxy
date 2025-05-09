package com.browserup.bup.proxy;

import java.io.IOException;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.auth.AuthType;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;

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

public class AutoAuthTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testBasicAuthAddedToHttpRequest() throws IOException {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        String stubUrl = "/basicAuthHttp";

        stubFor(get(urlEqualTo(stubUrl)).withHeader("Authorization", new EqualToPattern("Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA=="))
                .willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/basicAuthHttp"))
                    .getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testBasicAuthAddedToHttpsRequest() throws IOException {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        String stubUrl = "/basicAuthHttp";

        stubFor(get(urlEqualTo(stubUrl)).withHeader("Authorization", new EqualToPattern("Basic dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA=="))
                .willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(
                    "https://localhost:" + mockServerHttpsPort + "/basicAuthHttp")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testCanStopBasicAuth() throws IOException {
        // the base64-encoded rendering of "testUsername:testPassword" is dGVzdFVzZXJuYW1lOnRlc3RQYXNzd29yZA==
        String stubUrl = "/basicAuthHttp";

        stubFor(get(urlEqualTo(stubUrl)).withHeader("Authorization", AbsentPattern.ABSENT)
                .willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.autoAuthorization("localhost", "testUsername", "testPassword", AuthType.BASIC);
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.stopAutoAuthorization("localhost");

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/basicAuthHttp"))
                    .getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }
}
