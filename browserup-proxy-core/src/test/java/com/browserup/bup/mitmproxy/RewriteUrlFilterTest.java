package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RewriteUrlFilterTest extends MockServerTest {
    private MitmProxyServer proxy;

    @AfterEach
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testRewriteMultipleMatches() throws Exception {
        String stubUrl = "/testRewriteHttpHost/finalModification";
        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Host", new EqualToPattern("localhost:" + mockServerPort))
                .willReturn(ok()
                .withBody("success")));

        Map<String, String> rewriteRules = new LinkedHashMap<>();
        rewriteRules.put("http://www\\.someotherhost\\.com:(\\d+)/(\\w+)", "http://localhost:\\1/\\2");
        rewriteRules.put("http://localhost:(\\d+)/(\\w+)", "http://localhost:\\1/\\2/finalModification");

        proxy = new MitmProxyServer();
        proxy.rewriteUrls(rewriteRules);

        proxy.start();

        String url = "http://www.someotherhost.com:" + mockServerPort + "/testRewriteHttpHost";
        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse firstResponse = client.execute(new HttpGet(url));
            assertEquals(200, firstResponse.getStatusLine().getStatusCode(), "Did not receive HTTP 200 from mock server");

            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(firstResponse.getEntity().getContent());
            assertEquals("success", firstResponseBody, "Did not receive expected response from mock server");

            CloseableHttpResponse secondResponse = client.execute(new HttpGet(url));
            assertEquals(200, secondResponse.getStatusLine().getStatusCode(), "Did not receive HTTP 200 from mock server");

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(secondResponse.getEntity().getContent());
            assertEquals("success", secondResponseBody, "Did not receive expected response from mock server");
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testRewriteHttpHost() throws Exception {
        String stubUrl = "/testRewriteHttpHost";
        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Host", new EqualToPattern("localhost:" + mockServerPort))
                .willReturn(ok()
                .withBody("success")));

        proxy = new MitmProxyServer();
        proxy.rewriteUrl("http://www\\.someotherhost\\.com:(\\d+)/(\\w+)", "http://localhost:\\1/\\2");

        proxy.start();

        String url = "http://www.someotherhost.com:" + mockServerPort + "/testRewriteHttpHost";
        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse firstResponse = client.execute(new HttpGet(url));
            assertEquals(200, firstResponse.getStatusLine().getStatusCode(), "Did not receive HTTP 200 from mock server");

            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(firstResponse.getEntity().getContent());
            assertEquals("success", firstResponseBody, "Did not receive expected response from mock server");

            CloseableHttpResponse secondResponse = client.execute(new HttpGet(url));
            assertEquals(200, secondResponse.getStatusLine().getStatusCode(), "Did not receive HTTP 200 from mock server");

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(secondResponse.getEntity().getContent());
            assertEquals("success", secondResponseBody, "Did not receive expected response from mock server");
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testRewriteHttpResource() throws Exception {
        String stubUrl = "/rewrittenresource";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success")));

        proxy = new MitmProxyServer();
        proxy.rewriteUrl("http://badhost:(\\d+)/badresource", "http://localhost:\\1/rewrittenresource");

        proxy.start();

        String url = "http://badhost:" + mockServerPort + "/badresource";
        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("success", firstResponseBody, "Did not receive expected response from mock server");

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("success", secondResponseBody, "Did not receive expected response from mock server");
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testRewriteHttpsResource() throws Exception {
        String stubUrl = "/rewrittenresource";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success")));

        proxy = new MitmProxyServer();
        proxy.setTrustAllServers(true);
        proxy.rewriteUrl("https://localhost:(\\d+)/badresource", "https://localhost:\\1/rewrittenresource");

        proxy.start();

        String url = "https://localhost:" + mockServerHttpsPort + "/badresource";
        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("success", firstResponseBody, "Did not receive expected response from mock server");

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("success", secondResponseBody, "Did not receive expected response from mock server");
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)));
    }
}
