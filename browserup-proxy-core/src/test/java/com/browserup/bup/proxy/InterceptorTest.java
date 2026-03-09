package com.browserup.bup.proxy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.io.ByteStreams;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.RequestFilterAdapter;
import com.browserup.bup.filters.ResponseFilterAdapter;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import com.browserup.bup.util.HttpObjectUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterceptorTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @AfterEach
    protected void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    void testCanShortCircuitResponse() throws IOException {
        String url1 = "/regular200";
        stubFor(get(urlMatching(url1)).willReturn(ok().withBody("success")));

        String url2 = "/shortcircuit204";
        stubFor(get(urlMatching(url2)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicBoolean interceptorFired = new AtomicBoolean(false);
        final AtomicBoolean shortCircuitFired = new AtomicBoolean(false);

        proxy.addFirstHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                        if (httpObject instanceof HttpRequest) {
                            interceptorFired.set(true);

                            HttpRequest httpRequest = (HttpRequest) httpObject;

                            if (httpRequest.method().equals(HttpMethod.GET) && httpRequest.uri().contains("/shortcircuit204")) {
                                HttpResponse httpResponse = new DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.NO_CONTENT);

                                shortCircuitFired.set(true);

                                return httpResponse;
                            }
                        }

                        return super.clientToProxyRequest(httpObject);
                    }
                };
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/regular200"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertTrue(interceptorFired.get(), "Expected interceptor to fire");
            assertFalse(shortCircuitFired.get(), "Did not expected short circuit interceptor code to execute");

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals("success", responseBody, "Did not receive expected response from mock server");

            verify(1, getRequestedFor(urlEqualTo(url1)));
        }

        interceptorFired.set(false);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/shortcircuit204"));

            assertTrue(interceptorFired.get(), "Expected interceptor to fire");
            assertTrue(shortCircuitFired.get(), "Expected interceptor to short-circuit response");

            assertEquals(204, response.getStatusLine().getStatusCode(), "Expected interceptor to return a 204 (No Content)");
            assertNull(response.getEntity(), "Expected no entity attached to response");
        }
    }

    @Test
    void testCanModifyResponseBodyLarger() throws IOException {
        final String originalText = "The quick brown fox jumps over the lazy dog";
        final String newText = "The quick brown frog jumps over the lazy aardvark";

        testModifiedResponse(originalText, newText);
    }

    @Test
    void testCanModifyResponseBodySmaller() throws IOException {
        final String originalText = "The quick brown fox jumps over the lazy dog";
        final String newText = "The quick brown fox jumped.";

        testModifiedResponse(originalText, newText);
    }

    @Test
    void testCanModifyRequest() throws IOException {
        String url = "/modifyrequest";
        stubFor(
                get(urlEqualTo(url)).
                        willReturn(ok().
                                withBody("success").
                                withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), "text/plain; charset=utf-8")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addFirstHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                        if (httpObject instanceof HttpRequest) {
                            HttpRequest httpRequest = (HttpRequest) httpObject;
                            httpRequest.setUri(httpRequest.uri().replace("/originalrequest", "/modifyrequest"));
                        }

                        return super.clientToProxyRequest(httpObject);
                    }
                };
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/originalrequest"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals("success", responseBody, "Did not receive expected response from mock server");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testRequestFilterCanModifyHttpRequestBody() throws IOException {
        final String originalText = "original body";
        final String newText = "modified body";

        String url = "/modifyrequest";
        stubFor(put(urlMatching(url)).
                withRequestBody(WireMock.equalTo(newText)).
                willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (contents.isText()) {
                if (contents.getTextContents().equals(originalText)) {
                    contents.setTextContents(newText);
                }
            }

            return null;
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpPut request = new HttpPut("http://localhost:" + mockServerPort + "/modifyrequest");
            request.setEntity(new StringEntity(originalText));
            CloseableHttpResponse response = httpClient.execute(request);
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals("success", responseBody, "Did not receive expected response from mock server");

            verify(1, putRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testRequestFilterCanModifyHttpsRequestBody() throws IOException {
        final String originalText = "original body";
        final String newText = "modified body";

        String url = "/modifyrequest";
        stubFor(put(urlMatching(url)).
                withRequestBody(WireMock.equalTo(newText)).
                willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (contents.isText()) {
                if (contents.getTextContents().equals(originalText)) {
                    contents.setTextContents(newText);
                }
            }

            return null;
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpPut request = new HttpPut("https://localhost:" + mockServerHttpsPort + "/modifyrequest");
            request.setEntity(new StringEntity(originalText));
            CloseableHttpResponse response = httpClient.execute(request);
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals("success", responseBody, "Did not receive expected response from mock server");

            verify(1, putRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testResponseFilterCanModifyBinaryContents() throws IOException {
        final byte[] originalBytes = new byte[]{1, 2, 3, 4, 5};
        final byte[] newBytes = new byte[]{20, 30, 40, 50, 60};

        String url = "/modifyresponse";
        stubFor(
                get(urlEqualTo(url)).
                        willReturn(ok().
                                withBody(originalBytes).
                                withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), "application/octet-stream")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            if (!contents.isText()) {
                if (Arrays.equals(originalBytes, contents.getBinaryContents())) {
                    contents.setBinaryContents(newBytes);
                }
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpGet request = new HttpGet("http://localhost:" + mockServerPort + "/modifyresponse");
            CloseableHttpResponse response = httpClient.execute(request);
            byte[] responseBytes = ByteStreams.toByteArray(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertThat("Did not receive expected response from mock server", responseBytes, equalTo(newBytes));

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testResponseFilterCanModifyHttpTextContents() throws IOException {
        final String originalText = "The quick brown fox jumps over the lazy dog";
        final String newText = "The quick brown fox jumped.";

        String url = "/modifyresponse";
        stubFor(
                get(urlEqualTo(url)).
                        willReturn(ok().
                                withBody(originalText).
                                withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), "text/plain; charset=utf-8")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            if (contents.isText()) {
                if (contents.getTextContents().equals(originalText)) {
                    contents.setTextContents(newText);
                }
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpGet request = new HttpGet("http://localhost:" + mockServerPort + "/modifyresponse");
            request.addHeader("Accept-Encoding", "gzip");
            CloseableHttpResponse response = httpClient.execute(request);
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals(newText, responseBody, "Did not receive expected response from mock server");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testResponseFilterCanModifyHttpsTextContents() throws IOException {
        final String originalText = "The quick brown fox jumps over the lazy dog";
        final String newText = "The quick brown fox jumped.";

        String url = "/modifyresponse";
        stubFor(
                get(urlEqualTo(url)).
                        willReturn(ok().
                                withBody(originalText).
                                withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), "text/plain; charset=utf-8")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            if (contents.isText()) {
                if (contents.getTextContents().equals(originalText)) {
                    contents.setTextContents(newText);
                }
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            HttpGet request = new HttpGet("https://localhost:" + mockServerHttpsPort + "/modifyresponse");
            request.addHeader("Accept-Encoding", "gzip");
            CloseableHttpResponse response = httpClient.execute(request);
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals(newText, responseBody, "Did not receive expected response from mock server");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testResponseInterceptorWithoutBody() throws IOException {
        String url = "/interceptortest";
        stubFor(
                head(urlMatching(url)).
                        willReturn(ok().
                                withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), "application/octet-stream")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicReference<byte[]> responseContents = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> responseContents.set(contents.getBinaryContents()));

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpHead("http://localhost:" + mockServerPort + "/interceptortest"));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals(0, responseContents.get().length, "Expected binary contents captured in interceptor to be empty");

            verify(1, headRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testResponseFilterOriginalRequestNotModified() throws IOException {
        String url = "/modifiedendpoint";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (request.uri().endsWith("/originalendpoint")) {
                request.setUri(request.uri().replaceAll("originalendpoint", "modifiedendpoint"));
            }

            return null;
        });

        final AtomicReference<String> originalRequestUri = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> originalRequestUri.set(messageInfo.getOriginalRequest().uri()));

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/originalendpoint"));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertThat("Expected URI on originalRequest to match actual URI of original HTTP request", originalRequestUri.get(), endsWith("/originalendpoint"));

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Disabled
    @Test
    void testMessageContentsNotAvailableWithoutAggregation() throws IOException {
        String url = "/endpoint";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicBoolean requestContentsNull = new AtomicBoolean(false);
        final AtomicBoolean responseContentsNull = new AtomicBoolean(false);

        proxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource((request, contents, messageInfo) -> {
            if (contents == null) {
                requestContentsNull.set(true);
            }

            return null;
        }, 0));

        proxy.addFirstHttpFilterFactory(new ResponseFilterAdapter.FilterSource((response, contents, messageInfo) -> {
            if (contents == null) {
                responseContentsNull.set(true);
            }
        }, 0));

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/endpoint"));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertTrue(requestContentsNull.get(), "Expected HttpMessageContents to be null in RequestFilter because HTTP message aggregation is disabled");
            assertTrue(responseContentsNull.get(), "Expected HttpMessageContents to be null in ResponseFilter because HTTP message aggregation is disabled");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testMitmDisabledHttpsRequestFilterNotAvailable() throws IOException {
        String url = "/mitmdisabled";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setMitmDisabled(true);

        proxy.start();

        final AtomicBoolean connectRequestFilterFired = new AtomicBoolean(false);
        final AtomicBoolean getRequestFilterFired = new AtomicBoolean(false);

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (request.method().equals(HttpMethod.CONNECT)) {
                connectRequestFilterFired.set(true);
            } else if (request.method().equals(HttpMethod.GET)) {
                getRequestFilterFired.set(true);
            }
            return null;
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/mitmdisabled"));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");

            assertTrue(connectRequestFilterFired.get(), "Expected request filter to fire on CONNECT");
            assertFalse(getRequestFilterFired.get(), "Expected request filter to fail to fire on GET because MITM is disabled");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testMitmDisabledHttpsResponseFilterNotAvailable() throws IOException {
        String url = "/mitmdisabled";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setMitmDisabled(true);

        proxy.start();

        // unlike the request filter, the response filter doesn't fire when the 200 response to the CONNECT is sent to the client.
        // this is because the response filter is triggered when the serverToProxyResponse() filtering method is called, and
        // the "200 Connection established" is generated by the proxy itself.

        final AtomicBoolean responseFilterFired = new AtomicBoolean(false);

        proxy.addResponseFilter((response, contents, messageInfo) -> responseFilterFired.set(true));

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/mitmdisabled"));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertFalse(responseFilterFired.get(), "Expected response filter to fail to fire because MITM is disabled");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    /**
     * Helper method for executing response modification tests.
     */
    private void testModifiedResponse(final String originalText, final String newText) throws IOException {
        String url = "/modifyresponse";
        stubFor(
                get(urlMatching(url)).
                        willReturn(ok().
                                withBody(originalText).
                                withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), "text/plain; charset=utf-8")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.addFirstHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpObject proxyToClientResponse(HttpObject httpObject) {
                        if (httpObject instanceof FullHttpResponse) {
                            FullHttpResponse httpResponseAndContent = (FullHttpResponse) httpObject;

                            String bodyContent = HttpObjectUtil.extractHttpEntityBody(httpResponseAndContent);

                            if (bodyContent.equals(originalText)) {
                                HttpObjectUtil.replaceTextHttpEntityBody(httpResponseAndContent, newText);
                            }
                        }

                        return super.proxyToClientResponse(httpObject);
                    }
                };
            }

            @Override
            public int getMaximumResponseBufferSizeInBytes() {
                return 10000;
            }
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/modifyresponse"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals(newText, responseBody, "Did not receive expected response from mock server");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testCanBypassFilterForRequest() throws IOException, InterruptedException {
        String url = "/bypassfilter";
        stubFor(get(urlMatching(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicInteger filtersSourceHitCount = new AtomicInteger();
        final AtomicInteger filterHitCount = new AtomicInteger();

        proxy.addFirstHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                if (filtersSourceHitCount.getAndIncrement() == 0) {
                    return null;
                } else {
                    return new HttpFiltersAdapter(originalRequest) {
                        @Override
                        public void serverToProxyResponseReceived() {
                            filterHitCount.incrementAndGet();
                        }
                    };
                }
            }
        });

        // during the first request, the filterRequest(...) method should return null, which will prevent the filter instance from
        // being added to the filter chain
        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/bypassfilter"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);

        assertEquals(1, filtersSourceHitCount.get(), "Expected filters source to be invoked on first request");
        assertEquals(0, filterHitCount.get(), "Expected filter instance to be bypassed on first request");

        // during the second request, the filterRequest(...) method will return a filter instance, which should be invoked during processing
        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/bypassfilter"));
            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);

        assertEquals(2, filtersSourceHitCount.get(), "Expected filters source to be invoked again on second request");
        assertEquals(1, filterHitCount.get(), "Expected filter instance to be invoked on second request (only)");

        verify(2, getRequestedFor(urlEqualTo(url)));
    }

    @Test
    void testHttpResponseFilterMessageInfoPopulated() throws IOException {
        String urlPattern = "/httpmessageinfopopulated.*";
        stubFor(
                get(urlMatching(urlPattern)).
                        withQueryParam("param1", WireMock.equalTo("value1")).
                        willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicReference<ChannelHandlerContext> requestCtx = new AtomicReference<>();
        final AtomicReference<HttpRequest> requestOriginalRequest = new AtomicReference<>();
        final AtomicBoolean requestIsHttps = new AtomicBoolean(false);
        final AtomicReference<String> requestFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> requestFilterUrl = new AtomicReference<>();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            requestCtx.set(messageInfo.getChannelHandlerContext());
            requestOriginalRequest.set(messageInfo.getOriginalRequest());
            requestIsHttps.set(messageInfo.isHttps());
            requestFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            requestFilterUrl.set(messageInfo.getUrl());
            return null;
        });

        final AtomicReference<ChannelHandlerContext> responseCtx = new AtomicReference<>();
        final AtomicReference<HttpRequest> responseOriginalRequest = new AtomicReference<>();
        final AtomicBoolean responseIsHttps = new AtomicBoolean(false);
        final AtomicReference<String> responseFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> responseFilterUrl = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            responseCtx.set(messageInfo.getChannelHandlerContext());
            responseOriginalRequest.set(messageInfo.getOriginalRequest());
            responseIsHttps.set(messageInfo.isHttps());
            responseFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            responseFilterUrl.set(messageInfo.getUrl());
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String requestUrl = "http://localhost:" + mockServerPort + "/httpmessageinfopopulated?param1=value1";
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertNotNull(requestCtx.get(), "Expected ChannelHandlerContext to be populated in request filter");
            assertNotNull(requestOriginalRequest.get(), "Expected originalRequest to be populated in request filter");
            assertFalse(requestIsHttps.get(), "Expected isHttps to return false in request filter");
            assertEquals(requestUrl, requestFilterOriginalUrl.get(), "Expected originalUrl in request filter to match actual request URL");
            assertEquals(requestUrl, requestFilterUrl.get(), "Expected url in request filter to match actual request URL");

            assertNotNull(responseCtx.get(), "Expected ChannelHandlerContext to be populated in response filter");
            assertNotNull(responseOriginalRequest.get(), "Expected originalRequest to be populated in response filter");
            assertFalse(responseIsHttps.get(), "Expected isHttps to return false in response filter");
            assertEquals(requestUrl, responseFilterOriginalUrl.get(), "Expected originalUrl in response filter to match actual request URL");
            assertEquals(requestUrl, responseFilterUrl.get(), "Expected url in response filter to match actual request URL");

            verify(1, getRequestedFor(urlMatching(urlPattern)));
        }
    }

    @Test
    void testHttpResponseFilterUrlReflectsModifications() throws IOException {
        String url = "/urlreflectsmodifications";
        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        final AtomicReference<String> requestFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> requestFilterUrl = new AtomicReference<>();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            requestFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            requestFilterUrl.set(messageInfo.getUrl());
            return null;
        });

        // request filters get added to the beginning of the filter chain, so add this uri-modifying request filter after
        // adding the capturing request filter above.
        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (request.uri().endsWith("/originalurl")) {
                String newUrl = request.uri().replaceAll("originalurl", "urlreflectsmodifications");
                request.setUri(newUrl);
            }
            return null;
        });

        final AtomicReference<String> responseFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> responseFilterUrl = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            responseFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            responseFilterUrl.set(messageInfo.getUrl());
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String originalRequestUrl = "http://localhost:" + mockServerPort + "/originalurl";
            String modifiedRequestUrl = "http://localhost:" + mockServerPort + "/urlreflectsmodifications";
            CloseableHttpResponse response = httpClient.execute(new HttpGet(originalRequestUrl));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals(originalRequestUrl, requestFilterOriginalUrl.get(), "Expected originalUrl in request filter to match actual request URL");
            assertEquals(modifiedRequestUrl, requestFilterUrl.get(), "Expected url in request filter to match modified request URL");

            assertEquals(originalRequestUrl, responseFilterOriginalUrl.get(), "Expected originalUrl in response filter to match actual request URL");
            assertEquals(modifiedRequestUrl, responseFilterUrl.get(), "Expected url in response filter to match modified request URL");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testHttpsResponseFilterUrlReflectsModifications() throws IOException {
        String url = "/urlreflectsmodifications";
        stubFor(get(urlEqualTo(url)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        final AtomicReference<String> requestFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> requestFilterUrl = new AtomicReference<>();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            requestFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            requestFilterUrl.set(messageInfo.getUrl());
            return null;
        });

        // request filters get added to the beginning of the filter chain, so add this uri-modifying request filter after
        // adding the capturing request filter above.
        proxy.addRequestFilter((request, contents, messageInfo) -> {
            if (request.uri().endsWith("/originalurl")) {
                String newUrl = request.uri().replaceAll("originalurl", "urlreflectsmodifications");
                request.setUri(newUrl);
            }
            return null;
        });

        final AtomicReference<String> responseFilterOriginalUrl = new AtomicReference<>();
        final AtomicReference<String> responseFilterUrl = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            responseFilterOriginalUrl.set(messageInfo.getOriginalUrl());
            responseFilterUrl.set(messageInfo.getUrl());
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String originalRequestUrl = "https://localhost:" + mockServerHttpsPort + "/originalurl";
            String modifiedRequestUrl = "https://localhost:" + mockServerHttpsPort + "/urlreflectsmodifications";
            CloseableHttpResponse response = httpClient.execute(new HttpGet(originalRequestUrl));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");
            assertEquals(originalRequestUrl, requestFilterOriginalUrl.get(), "Expected originalUrl in request filter to match actual request URL");
            assertEquals(modifiedRequestUrl, requestFilterUrl.get(), "Expected url in request filter to match modified request URL");

            assertEquals(originalRequestUrl, responseFilterOriginalUrl.get(), "Expected originalUrl in response filter to match actual request URL");
            assertEquals(modifiedRequestUrl, responseFilterUrl.get(), "Expected url in response filter to match modified request URL");

            verify(1, getRequestedFor(urlEqualTo(url)));
        }
    }

    @Test
    void testHttpsResponseFilterMessageInfoPopulated() throws IOException {
        String urlPattern = "/httpmessageinfopopulated.*";
        stubFor(
                get(urlMatching(urlPattern)).
                        withQueryParam("param1", WireMock.equalTo("value1")).
                        willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        final AtomicReference<ChannelHandlerContext> requestCtx = new AtomicReference<>();
        final AtomicReference<HttpRequest> requestOriginalRequest = new AtomicReference<>();
        final AtomicBoolean requestIsHttps = new AtomicBoolean(false);
        final AtomicReference<String> requestOriginalUrl = new AtomicReference<>();

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            requestCtx.set(messageInfo.getChannelHandlerContext());
            requestOriginalRequest.set(messageInfo.getOriginalRequest());
            requestIsHttps.set(messageInfo.isHttps());
            requestOriginalUrl.set(messageInfo.getOriginalUrl());
            return null;
        });

        final AtomicReference<ChannelHandlerContext> responseCtx = new AtomicReference<>();
        final AtomicReference<HttpRequest> responseOriginalRequest = new AtomicReference<>();
        final AtomicBoolean responseIsHttps = new AtomicBoolean(false);
        final AtomicReference<String> responseOriginalUrl = new AtomicReference<>();

        proxy.addResponseFilter((response, contents, messageInfo) -> {
            responseCtx.set(messageInfo.getChannelHandlerContext());
            responseOriginalRequest.set(messageInfo.getOriginalRequest());
            responseIsHttps.set(messageInfo.isHttps());
            responseOriginalUrl.set(messageInfo.getOriginalUrl());
        });

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String requestUrl = "https://localhost:" + mockServerHttpsPort + "/httpmessageinfopopulated?param1=value1";
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));

            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected server to return a 200");

            assertNotNull(requestCtx.get(), "Expected ChannelHandlerContext to be populated in request filter");
            assertNotNull(requestOriginalRequest.get(), "Expected originalRequest to be populated in request filter");
            assertTrue(requestIsHttps.get(), "Expected isHttps to return true in request filter");
            assertEquals(requestUrl, requestOriginalUrl.get(), "Expected originalUrl in request filter to match actual request URL");

            assertNotNull(responseCtx.get(), "Expected ChannelHandlerContext to be populated in response filter");
            assertNotNull(responseOriginalRequest.get(), "Expected originalRequest to be populated in response filter");
            assertTrue(responseIsHttps.get(), "Expected isHttps to return true in response filter");
            assertEquals(requestUrl, responseOriginalUrl.get(), "Expected originalUrl in response filter to match actual request URL");

            verify(1, getRequestedFor(urlMatching(urlPattern)));
        }
    }
}
