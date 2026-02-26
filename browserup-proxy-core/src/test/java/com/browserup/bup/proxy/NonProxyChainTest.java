package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;

@Ignore
public class NonProxyChainTest extends MockServerTest {

    private BrowserUpProxy proxy;

    public HttpProxyServer upstreamProxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }

        if (upstreamProxy != null) {
            upstreamProxy.abort();
        }
    }

    /**
     * This testcase will set up a upstream proxy that is blocking all requests containing "localhost:"
     * Then it will setup a proxy with that upstream proxy
     * Then it will call an address containing "localhost:mockport/"
     * This will end up in a 502, because the request is processed to the upstream proxy, which will deny the request.
     */
    @Test
    public void testUpStreamProxyWithoutNonProxy() throws Exception {

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withFiltersSource(getFiltersSource())
                .withPort(0)
                .start();

        String stub = "/";
        stubFor(get(urlEqualTo(stub)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress());
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/"));
            assertEquals("Did not receive HTTP 502 from mock server", 502, response.getStatusLine().getStatusCode());
        }

        verify(0, getRequestedFor(urlEqualTo("/")));
    }

    /**
     * This testcase will set up a upstream proxy that is blocking all requests containing "localhost:"
     * Then it will setup a proxy with that upstream proxy and configure a nonProxyHost "localhost"
     * Then it will call an address containing "localhost:mockport"
     * This will end up in a 200, because the request is NOT processed to the upstream proxy due the nonProxySetting
     */
    @Test
    public void testUpStreamProxyWithNonProxy() throws Exception {

        List<String> objects = new ArrayList<>();
        objects.add("localhost");

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withFiltersSource(getFiltersSource())
                .withPort(0)
                .start();

        String stub = "/";
        stubFor(get(urlEqualTo(stub)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress());
        proxy.setChainedProxyNonProxyHosts(objects);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/"));
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlEqualTo("/")));
    }

    /**
     * This testcase will set up a upstream proxy that is blocking all requests containing "localhost:"
     * Then it will setup a proxy with that upstream proxy and configure a nonProxyHost "*"
     * Then it will call an address containing "localhost:mockport"
     * This will end up in a 200, because the request is NOT processed to the upstream proxy due the nonProxySetting
     */
    @Test
    public void testUpStreamProxyWithNonProxyWildcard() throws Exception {

        List<String> objects = new ArrayList<>();
        objects.add("*");

        upstreamProxy = DefaultHttpProxyServer.bootstrap()
                .withFiltersSource(getFiltersSource())
                .withPort(0)
                .start();

        String stub = "/";
        stubFor(get(urlEqualTo(stub)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setChainedProxy(upstreamProxy.getListenAddress());
        proxy.setChainedProxyNonProxyHosts(objects);
        proxy.setTrustAllServers(true);
        proxy.start();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/"));
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        verify(1, getRequestedFor(urlEqualTo("/")));
    }

    private HttpFiltersSource getFiltersSource() {

        return new HttpFiltersSourceAdapter() {

            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {

                return new HttpFiltersAdapter(originalRequest) {

                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {

                        if (httpObject instanceof HttpRequest) {
                            HttpRequest request = (HttpRequest) httpObject;

                            System.out.println("Method URI : " + request.method() + " " + request.uri());

                            if (request.uri().contains("localhost:")) {
                                return getBadGatewayResponse();
                            }
                        }
                        return null;
                    }

                    private HttpResponse getBadGatewayResponse() {
                        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                                .concat("<html><head>\n")
                                .concat("<title>Bad Gateway</title>\n")
                                .concat("</head><body>\n")
                                .concat("An error occurred")
                                .concat("</body></html>\n");
                        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
                        ByteBuf content = Unpooled.copiedBuffer(bytes);
                        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content);
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
                        response.headers().set("Content-Type", "text/html; charset=UTF-8");
                        response.headers().set("Date", formatDateForHttp(new Date()));
                        response.headers().set(HttpHeaderNames.CONNECTION, "close");
                        return response;
                    }
                };
            }
        };
    }

    private static String formatDateForHttp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }
}
