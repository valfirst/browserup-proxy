package com.browserup.bup.proxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.MitmProxyManager;
import com.browserup.bup.filters.RequestFilter;
import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.proxy.bricks.ProxyResource;
import com.browserup.bup.proxy.test.util.ProxyResourceTest;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.sitebricks.headless.Request;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Ignore
public class FilterTest extends ProxyResourceTest {

    private String readBody(HttpURLConnection conn) throws Exception {
        InputStream is;
        try { is = conn.getInputStream(); } catch (Exception e) { is = conn.getErrorStream(); }
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) { sb.append(buffer, 0, len); }
            return sb.toString();
        }
    }

    @Test
    public void testCanModifyRequestHeadersWithJavascript() throws Exception {
        final String requestFilterJavaScript =
                "request.headers().remove('User-Agent');\n" +
                "request.headers().add('User-Agent', 'My-Custom-User-Agent-String 1.0');\n";

        Request mockRestRequest = createMockRestRequestWithEntity(requestFilterJavaScript);
        proxyResource.addRequestFilter(proxyPort, mockRestRequest);

        String stubUrl = "/modifyuseragent";
        stubFor(get(urlEqualTo(stubUrl))
                .withHeader("User-Agent", WireMock.equalTo("My-Custom-User-Agent-String 1.0"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("success")
                        .withHeader("Content-Type", "text/plain")));

        HttpURLConnection conn = getHttpConnection("/modifyuseragent");
        String body = readBody(conn);
        assertEquals("Javascript interceptor did not modify the user agent string", "success", body);
        conn.disconnect();
    }

    @Test
    public void testCanModifyRequestContentsWithJavascript() throws Exception {
        final String requestFilterJavaScript =
                "if (request.getUri().endsWith('/modifyrequest') && contents.isText()) {\n" +
                "    if (contents.getTextContents() == 'original request text') {\n" +
                "        contents.setTextContents('modified request text');\n" +
                "    }\n" +
                "}\n";

        Request mockRestRequest = createMockRestRequestWithEntity(requestFilterJavaScript);
        proxyResource.addRequestFilter(proxyPort, mockRestRequest);

        String stubUrl = "/modifyrequest";
        stubFor(put(urlEqualTo(stubUrl))
                .withRequestBody(WireMock.equalTo("modified request text"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("success")
                        .withHeader("Content-Type", "text/plain; charset=UTF-8")));

        HttpURLConnection conn = getHttpConnection("/modifyrequest");
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.getOutputStream().write("original request text".getBytes(StandardCharsets.UTF_8));
        String body = readBody(conn);
        assertEquals("Javascript interceptor did not modify request body", "success", body);
        conn.disconnect();
    }

    @Test
    public void testCanModifyResponseWithJavascript() throws Exception {
        final String responseFilterJavaScript =
                "if (contents.isText()) {\n" +
                "    if (contents.getTextContents() == 'original response text') {\n" +
                "        contents.setTextContents('modified response text');\n" +
                "    }\n" +
                "}\n";

        Request mockRestRequest = createMockRestRequestWithEntity(responseFilterJavaScript);
        proxyResource.addResponseFilter(proxyPort, mockRestRequest);

        String stubUrl = "/modifyresponse";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(aResponse().withStatus(200)
                        .withBody("original response text")
                        .withHeader("Content-Type", "text/plain; charset=UTF-8")));

        HttpURLConnection conn = getHttpConnection("/modifyresponse");
        String body = readBody(conn);
        assertEquals("Javascript interceptor did not modify response text", "modified response text", body);
        conn.disconnect();
    }

    @Test
    public void testCanAccessOriginalRequestWithJavascript() throws Exception {
        final String requestFilterJavaScript =
                "if (request.getUri().endsWith('/originalrequest')) {\n" +
                "    request.setUri(request.getUri().replaceAll('originalrequest', 'modifiedrequest'));\n" +
                "}\n";

        Request mockRestAddReqFilterRequest = createMockRestRequestWithEntity(requestFilterJavaScript);
        proxyResource.addRequestFilter(proxyPort, mockRestAddReqFilterRequest);

        final String responseFilterJavaScript =
                "contents.setTextContents(messageInfo.getOriginalRequest().getUri());\n";
        Request mockRestAddRespFilterRequest = createMockRestRequestWithEntity(responseFilterJavaScript);
        proxyResource.addResponseFilter(proxyPort, mockRestAddRespFilterRequest);

        String stubUrl = "/modifiedrequest";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(aResponse().withStatus(200)
                        .withBody("should-be-replaced")
                        .withHeader("Content-Type", "text/plain; charset=UTF-8")));

        HttpURLConnection conn = getHttpConnection("/originalrequest");
        String body = readBody(conn);
        assertThat("Javascript interceptor did not read messageData.originalRequest variable successfully", body, endsWith("originalrequest"));
        conn.disconnect();
    }

    @Test
    public void testRequestFilterNotAddedIfJavascriptDoesNotCompile() throws Exception {
        final String requestFilterJavaScript = "this javascript won\'t compile!";

        Request mockRestAddReqFilterRequest = createMockRestRequestWithEntity(requestFilterJavaScript);

        MitmProxyServer mockProxy = mock(MitmProxyServer.class);

        MitmProxyManager mockProxyManager = mock(MitmProxyManager.class);
        when(mockProxyManager.get(proxyPort)).thenReturn(mockProxy);

        ProxyResource proxyResource = new ProxyResource(mockProxyManager);

        boolean javascriptExceptionOccurred = false;
        try {
            proxyResource.addRequestFilter(proxyPort, mockRestAddReqFilterRequest);
        } catch (Exception ignored) {
            javascriptExceptionOccurred = true;
        }

        assertTrue("Expected javascript to fail to compile", javascriptExceptionOccurred);
        verify(mockProxy, never()).addRequestFilter(any(RequestFilter.class));
    }

    @Test
    public void testResponseFilterNotAddedIfJavascriptDoesNotCompile() throws Exception {
        final String responseFilterJavaScript = "this javascript won\'t compile!";

        Request mockRestAddRespFilterRequest = createMockRestRequestWithEntity(responseFilterJavaScript);

        MitmProxyServer mockProxy = mock(MitmProxyServer.class);

        MitmProxyManager mockProxyManager = mock(MitmProxyManager.class);
        when(mockProxyManager.get(proxyPort)).thenReturn(mockProxy);

        ProxyResource proxyResource = new ProxyResource(mockProxyManager);

        boolean javascriptExceptionOccurred = false;
        try {
            proxyResource.addResponseFilter(proxyPort, mockRestAddRespFilterRequest);
        } catch (Exception ignored) {
            javascriptExceptionOccurred = true;
        }

        assertTrue("Expected javascript to fail to compile", javascriptExceptionOccurred);
        verify(mockProxy, never()).addResponseFilter(any(ResponseFilter.class));
    }

    @Test
    public void testCanShortCircuitRequestWithJavascript() throws Exception {
        double javaVersion = Double.parseDouble(System.getProperty("java.specification.version"));
        assumeThat("Skipping Nashorn-dependent test on Java 1.7", javaVersion, greaterThanOrEqualTo(1.8d));

        final String requestFilterJavaScript =
                "var DefaultFullHttpResponse = Java.type('io.netty.handler.codec.http.DefaultFullHttpResponse');\n" +
                "var HttpResponseStatus = Java.type('io.netty.handler.codec.http.HttpResponseStatus');\n" +
                "var HttpObjectUtil = Java.type('com.browserup.bup.util.HttpObjectUtil');\n" +
                "var shortCircuitRequest = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.PAYMENT_REQUIRED);\n" +
                "var responseBody = 'You have to pay the troll toll to get into this Proxy\\\'s soul';\n" +
                "HttpObjectUtil.replaceTextHttpEntityBody(shortCircuitRequest, responseBody);\n" +
                "shortCircuitRequest;\n";

        Request mockRestRequest = createMockRestRequestWithEntity(requestFilterJavaScript);
        proxyResource.addRequestFilter(proxyPort, mockRestRequest);

        HttpURLConnection conn = getHttpConnection("/testShortCircuit");
        int status = conn.getResponseCode();
        String body = readBody(conn);
        assertEquals("Expected short-circuit response to return an HTTP 402 Payment Required", 402, status);
        assertEquals("Expected short-circuit response to contain body text set in Javascript",
                "You have to pay the troll toll to get into this Proxy's soul", body);
        conn.disconnect();
    }

    @Override
    public String[] getArgs() {
        return new String[]{"--use-littleproxy", "true"};
    }
}
