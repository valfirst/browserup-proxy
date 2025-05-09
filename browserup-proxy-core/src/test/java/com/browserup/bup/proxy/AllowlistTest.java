package com.browserup.bup.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.AllowlistFilter;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AllowlistTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testAllowlistCannotShortCircuitCONNECT() {
        HttpRequest request = mock();
        when(request.method()).thenReturn(HttpMethod.CONNECT);
        when(request.uri()).thenReturn("somedomain.com:443");
        when(request.protocolVersion()).thenReturn(HttpVersion.HTTP_1_1);

        ChannelHandlerContext mockCtx = mock();

        // create a allowlist filter that allowlists no requests (i.e., all requests should return the specified HTTP 500 status code)
        AllowlistFilter filter = new AllowlistFilter(request, mockCtx, true, 500, new ArrayList<>());
        HttpResponse response = filter.clientToProxyRequest(request);

        assertNull("Allowlist short-circuited HTTP CONNECT. Expected all HTTP CONNECTs to be allowlisted.", response);
    }

    @Test
    public void testNonAllowlistedHttpRequestReturnsAllowlistStatusCode() throws IOException {
        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.allowlistRequests(List.of("http://localhost/.*"), 500);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://www.someother.domain/someresource"));
            assertEquals("Did not receive allowlist status code in response", 500, response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected allowlist response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testNonAllowlistedHttpsRequestReturnsAllowlistStatusCode() throws IOException {
        String url = "/nonallowlistedresource";

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("should never be returned")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.allowlistRequests(List.of("https://some-other-domain/.*"), 500);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/nonallowlistedresource"));
            assertEquals("Did not receive allowlist status code in response", 500, response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected allowlist response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testAllowlistedHttpRequestNotShortCircuited() throws IOException {
        String url = "/allowlistedresource";

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("allowlisted")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.allowlistRequests(List.of("http://localhost:" + mockServerPort + "/.*"), 500);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/allowlistedresource"));
            assertEquals("Did not receive expected response from mock server for allowlisted url", 200,
                    response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals("Did not receive expected response body from mock server for allowlisted url", "allowlisted",
                    responseBody);
        }
    }

    @Test
    public void testAllowlistedHttpsRequestNotShortCircuited() throws IOException {
        String url = "/allowlistedresource";

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("allowlisted")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.allowlistRequests(List.of("https://localhost:" + mockServerHttpsPort + "/.*"), 500);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/allowlistedresource"));
            assertEquals("Did not receive expected response from mock server for allowlisted url", 200,
                    response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals("Did not receive expected response body from mock server for allowlisted url", "allowlisted",
                    responseBody);
        }
    }

    @Test
    public void testCanAllowlistSpecificHttpResource() throws IOException {
        String url = "/allowlistedresource";

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("allowlisted")));

        String url2 = "/nonallowlistedresource";

        stubFor(get(urlEqualTo(url2)).willReturn(ok().withBody("should never be returned")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.allowlistRequests(List.of("http://localhost:" + mockServerPort + "/allowlistedresource"), 500);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse nonAllowlistedResponse = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/nonallowlistedresource"));
            assertEquals("Did not receive allowlist status code in response", 500,
                    nonAllowlistedResponse.getStatusLine().getStatusCode());

            String nonAllowlistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    nonAllowlistedResponse.getEntity().getContent());
            assertThat("Expected allowlist response to contain 0-length body",
                    nonAllowlistedResponseBody, is(emptyOrNullString()));

            CloseableHttpResponse allowlistedResponse = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/allowlistedresource"));
            assertEquals("Did not receive expected response from mock server for allowlisted url", 200,
                    allowlistedResponse.getStatusLine().getStatusCode());

            String allowlistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    allowlistedResponse.getEntity().getContent());
            assertEquals("Did not receive expected response body from mock server for allowlisted url", "allowlisted",
                    allowlistedResponseBody);
        }
    }

    @Test
    public void testCanAllowlistSpecificHttpsResource() throws IOException {
        String url = "/allowlistedresource";

        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("allowlisted")));

        String url2 = "/nonallowlistedresource";

        stubFor(get(urlEqualTo(url2)).willReturn(ok().withBody("should never be returned")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.allowlistRequests(List.of("https://localhost:" + mockServerHttpsPort + "/allowlistedresource"), 500);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse nonAllowlistedResponse = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/nonallowlistedresource"));
            assertEquals("Did not receive allowlist status code in response", 500,
                    nonAllowlistedResponse.getStatusLine().getStatusCode());

            String nonAllowlistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    nonAllowlistedResponse.getEntity().getContent());
            assertThat("Expected allowlist response to contain 0-length body",
                    nonAllowlistedResponseBody, is(emptyOrNullString()));

            CloseableHttpResponse allowlistedResponse = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/allowlistedresource"));
            assertEquals("Did not receive expected response from mock server for allowlisted url", 200,
                    allowlistedResponse.getStatusLine().getStatusCode());

            String allowlistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    allowlistedResponse.getEntity().getContent());
            assertEquals("Did not receive expected response body from mock server for allowlisted url",
                    "allowlisted", allowlistedResponseBody);
        }
    }
}
