package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class RewriteUrlFilterTest extends MockServerTest {
    private MitmProxyServer proxy;

    @After
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
            assertEquals("Did not receive HTTP 200 from mock server", 200, firstResponse.getStatusLine().getStatusCode());

            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(firstResponse.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody);

            CloseableHttpResponse secondResponse = client.execute(new HttpGet(url));
            assertEquals("Did not receive HTTP 200 from mock server", 200, secondResponse.getStatusLine().getStatusCode());

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(secondResponse.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody);
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
            assertEquals("Did not receive HTTP 200 from mock server", 200, firstResponse.getStatusLine().getStatusCode());

            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(firstResponse.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody);

            CloseableHttpResponse secondResponse = client.execute(new HttpGet(url));
            assertEquals("Did not receive HTTP 200 from mock server", 200, secondResponse.getStatusLine().getStatusCode());

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(secondResponse.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody);
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
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody);

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody);
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
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody);

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody);
        }

        verify(2, getRequestedFor(urlEqualTo(stubUrl)));
    }
}
