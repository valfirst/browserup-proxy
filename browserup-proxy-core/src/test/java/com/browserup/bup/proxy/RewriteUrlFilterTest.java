package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.RewriteUrlFilter;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.common.collect.ImmutableList;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import java.util.Collection;

import static com.browserup.bup.filters.HttpsAwareFiltersAdapter.IS_HTTPS_ATTRIBUTE_NAME;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RewriteUrlFilterTest extends MockServerTest {
    public BrowserUpProxy proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRewriteWithCaptureGroups() {
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        when(mockHeaders.contains(HttpHeaderNames.HOST)).thenReturn(false);

        HttpRequest request = mock(HttpRequest.class);
        when(request.uri()).thenReturn("http://www.yahoo.com?param=someValue");
        when(request.headers()).thenReturn(mockHeaders);

        Collection<RewriteRule> rewriteRules = ImmutableList.of(new RewriteRule("http://www\\.(yahoo|bing)\\.com\\?(\\w+)=(\\w+)", "http://www.google.com?originalDomain=$1&$2=$3"));

        // mock out the netty ChannelHandlerContext for the isHttps() call in the filter
        Attribute<Boolean> mockIsHttpsAttribute = mock(Attribute.class);
        when(mockIsHttpsAttribute.get()).thenReturn(Boolean.FALSE);

        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
        io.netty.channel.Channel channelMock = mock(io.netty.channel.Channel.class);
        when(mockCtx.channel()).thenReturn(channelMock);
        when(channelMock.attr(AttributeKey.<Boolean>valueOf(IS_HTTPS_ATTRIBUTE_NAME))).thenReturn(mockIsHttpsAttribute);

        RewriteUrlFilter filter = new RewriteUrlFilter(request, mockCtx, rewriteRules);
        filter.clientToProxyRequest(request);

        verify(request).setUri("http://www.google.com?originalDomain=yahoo&param=someValue");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRewriteMultipleMatches() {
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        when(mockHeaders.contains(HttpHeaderNames.HOST)).thenReturn(false);

        HttpRequest request = mock(HttpRequest.class);
        when(request.uri()).thenReturn("http://www.yahoo.com?param=someValue");
        when(request.headers()).thenReturn(mockHeaders);

        Collection<RewriteRule> rewriteRules = ImmutableList.of(
                new RewriteRule("http://www\\.yahoo\\.com\\?(\\w+)=(\\w+)", "http://www.bing.com?new$1=new$2"),
                new RewriteRule("http://www\\.(yahoo|bing)\\.com\\?(\\w+)=(\\w+)", "http://www.google.com?originalDomain=$1&$2=$3")
        );

        // mock out the netty ChannelHandlerContext for the isHttps() call in the filter
        Attribute<Boolean> mockIsHttpsAttribute = mock(Attribute.class);
        when(mockIsHttpsAttribute.get()).thenReturn(Boolean.FALSE);

        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
        io.netty.channel.Channel channelMock = mock(io.netty.channel.Channel.class);
        when(mockCtx.channel()).thenReturn(channelMock);
        when(channelMock.attr(AttributeKey.<Boolean>valueOf(IS_HTTPS_ATTRIBUTE_NAME))).thenReturn(mockIsHttpsAttribute);

        RewriteUrlFilter filter = new RewriteUrlFilter(request, mockCtx, rewriteRules);
        filter.clientToProxyRequest(request);

        verify(request).setUri("http://www.google.com?originalDomain=bing&newparam=newsomeValue");
    }

    @Test
    public void testRewriteHttpHost() throws Exception {
        String stubUrl = "/testRewriteHttpHost";
        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("Host", new EqualToPattern("localhost:" + mockServerPort))
                .willReturn(ok()
                .withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.rewriteUrl("http://www\\.someotherhost\\.com:(\\d+)/(\\w+)", "http://localhost:$1/$2");

        proxy.start();

        String url = "http://www.someotherhost.com:" + mockServerPort + "/testRewriteHttpHost";
        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse firstResponse = httpClient.execute(new HttpGet(url));
            assertEquals("Did not receive HTTP 200 from mock server", 200, firstResponse.getStatusLine().getStatusCode());

            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(firstResponse.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody);

            CloseableHttpResponse secondResponse = httpClient.execute(new HttpGet(url));
            assertEquals("Did not receive HTTP 200 from mock server", 200, secondResponse.getStatusLine().getStatusCode());

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(secondResponse.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody);
        }

        WireMock.verify(2, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testRewriteHttpResource() throws Exception {
        String stubUrl = "/rewrittenresource";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.rewriteUrl("http://badhost:(\\d+)/badresource", "http://localhost:$1/rewrittenresource");

        proxy.start();

        String url = "http://badhost:" + mockServerPort + "/badresource";
        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody);

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody);
        }

        WireMock.verify(2, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testRewriteHttpsResource() throws Exception {
        String stubUrl = "/rewrittenresource";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.rewriteUrl("https://localhost:(\\d+)/badresource", "https://localhost:$1/rewrittenresource");

        proxy.start();

        String url = "https://localhost:" + mockServerHttpsPort + "/badresource";
        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String firstResponseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", firstResponseBody);

            String secondResponseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(url)).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", secondResponseBody);
        }

        WireMock.verify(2, getRequestedFor(urlEqualTo(stubUrl)));
    }
}
