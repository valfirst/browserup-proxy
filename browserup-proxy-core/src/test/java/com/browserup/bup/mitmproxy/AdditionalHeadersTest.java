package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class AdditionalHeadersTest extends MockServerTest {

    private MitmProxyServer proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testAdditionalHeaderIsAdded() throws Exception {
        String stubUrl = "/dummyPath";
        String customHeaderName = "CustomHeaderName";
        String customHeaderValue = "CustomHeaderValue";

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader(customHeaderName, new EqualToPattern(customHeaderValue))
                .willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();
        proxy.addHeader(customHeaderName, customHeaderValue);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("http://localhost:" + mockServerPort + stubUrl)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testAdditionalHeadersAreAdded() throws Exception {
        String stubUrl = "/dummyPath";

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("CustomHeaderName1", "CustomHeaderValue1");
        headers.put("CustomHeaderName2", "CustomHeaderValue2");

        var stub = get(urlEqualTo(stubUrl))
                .willReturn(ok().withBody("success"));

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            stub.withHeader(entry.getKey(), new EqualToPattern(entry.getValue()));
        }

        stubFor(stub);

        proxy = new MitmProxyServer();
        proxy.addHeaders(headers);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("http://localhost:" + mockServerPort + stubUrl)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testAdditionalHeadersAreAddedExceptOne() throws Exception {
        String stubUrl = "/dummyPath";

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("CustomHeaderName1", "CustomHeaderValue1");
        headers.put("CustomHeaderName2", "CustomHeaderValue2");
        headers.put("CustomHeaderName3", "CustomHeaderValue3");

        var stub = get(urlEqualTo(stubUrl))
                .willReturn(ok().withBody("success"));

        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(notFound()));

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            stub.withHeader(entry.getKey(), new EqualToPattern(entry.getValue()));
        }

        stubFor(stub);

        Map<String, String> firstTwo = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (count++ < 2) {
                firstTwo.put(entry.getKey(), entry.getValue());
            }
        }

        proxy = new MitmProxyServer();
        proxy.addHeaders(firstTwo);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            int responseCode = client.execute(new HttpGet("http://localhost:" + mockServerPort + stubUrl)).getStatusLine().getStatusCode();
            assertEquals("Expected to get 404 response", 404, responseCode);
        }

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testAdditionalHeadersAreAddedAndOneDeleted() throws Exception {
        String stubUrl = "/dummyPath";

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("CustomHeaderName1", "CustomHeaderValue1");
        headers.put("CustomHeaderName2", "CustomHeaderValue2");
        headers.put("CustomHeaderName3", "CustomHeaderValue3");

        var stub = get(urlEqualTo(stubUrl))
                .willReturn(ok().withBody("success"));

        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(notFound()));

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            stub.withHeader(entry.getKey(), new EqualToPattern(entry.getValue()));
        }

        stubFor(stub);

        proxy = new MitmProxyServer();
        proxy.addHeaders(headers);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("http://localhost:" + mockServerPort + stubUrl)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        proxy.removeHeader(headers.entrySet().iterator().next().getKey());

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            int responseCode = client.execute(new HttpGet("http://localhost:" + mockServerPort + stubUrl)).getStatusLine().getStatusCode();
            assertEquals("Expected to get 404 response", 404, responseCode);
        }

        assertEquals("Expected to get 2 headers left after removing", 2, proxy.getAllHeaders().size());

        verify(2, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testAdditionalHeadersAreAddedAndAllDeleted() throws Exception {
        String stubUrl = "/dummyPath";

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("CustomHeaderName1", "CustomHeaderValue1");
        headers.put("CustomHeaderName2", "CustomHeaderValue2");
        headers.put("CustomHeaderName3", "CustomHeaderValue3");

        var stubWithoutHeaders = get(urlEqualTo(stubUrl))
                .willReturn(ok().withBody("success"));

        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(notFound()));

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            stubWithoutHeaders.withHeader(entry.getKey(), new AbsentPattern(entry.getValue()));
        }

        stubFor(stubWithoutHeaders);

        proxy = new MitmProxyServer();
        proxy.addHeaders(headers);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            int responseCode = client.execute(new HttpGet("http://localhost:" + mockServerPort + stubUrl)).getStatusLine().getStatusCode();
            assertEquals("Expected to get 404 response", 404, responseCode);
        }

        proxy.removeAllHeaders();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet("http://localhost:" + mockServerPort + stubUrl)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        assertEquals("Expected to get no headers left after removing", 0, proxy.getAllHeaders().size());

        verify(2, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testFailsIfAdditionalHeaderNotAdded() throws Exception {
        String stubUrl = "/dummyPath";
        String customHeaderName = "CustomHeaderName";
        String customHeaderValue = "CustomHeaderValue";

        stubFor(get(urlEqualTo(stubUrl))
                .withHeader(customHeaderName, new EqualToPattern(customHeaderValue))
                .willReturn(ok().withBody("success")));

        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(notFound()));

        proxy = new MitmProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            int responseCode = client.execute(new HttpGet("http://localhost:" + mockServerPort + stubUrl)).getStatusLine().getStatusCode();
            assertEquals("Expected to get 404 response", 404, responseCode);
        }
    }
}
