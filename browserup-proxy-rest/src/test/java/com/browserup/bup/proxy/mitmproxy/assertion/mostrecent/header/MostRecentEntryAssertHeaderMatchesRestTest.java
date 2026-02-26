package com.browserup.bup.proxy.mitmproxy.assertion.mostrecent.header;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class MostRecentEntryAssertHeaderMatchesRestTest extends BaseRestTest {
    private String responseBody = "success";
    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";
    private String headerValueToFind = "some-header-part";
    private String headerValueNotToFind = "will not find";
    private String headerNameToFind = "some-header-name";
    private String headerNameNotToFind = "some-header-name-not-to-find";
    private String headerValuePatternToMatch = ".*";
    private String headerValuePatternNotToMatch = ".*" + headerNameNotToFind + ".*";
    private String headerNamePatternToMatch = ".*" + headerNameToFind + ".*";
    private String headerNamePatternNotToMatch = ".*" + headerNameNotToFind + ".*";
    private HttpHeader[] headers = new HttpHeader[]{new HttpHeader(headerNameToFind, "header value before " + headerValueToFind + " header value after")};

    @Override
    protected String getUrlPath() { return "har/mostRecentEntry/assertResponseHeaderMatches"; }

    @Test
    public void anyNameAndMatchingValuePatternPass() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "headerValuePattern", headerValuePatternToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1)); assertAssertionPassed(r);
        assertFalse("Expected assertion entry result to have \"false\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void anyNameAndNotMatchingValuePatternFail() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "headerValuePattern", headerValuePatternNotToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1)); assertAssertionFailed(r);
        assertTrue("Expected assertion entry result to have \"true\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void matchingNameAndMatchingValuePass() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "headerNamePattern", headerNamePatternToMatch, "headerValuePattern", headerValuePatternToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1)); assertAssertionPassed(r);
        assertFalse("Expected assertion entry result to have \"false\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void notMatchingNameAndMatchingValuePass() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "headerNamePattern", headerNamePatternNotToMatch, "headerValuePattern", headerValuePatternToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1)); assertAssertionPassed(r);
        assertFalse("Expected assertion entry result to have \"false\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void matchingNameAndNotMatchingValueFail() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "headerNamePattern", headerNamePatternToMatch, "headerValuePattern", headerValuePatternNotToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1)); assertAssertionFailed(r);
        assertTrue("Expected assertion entry result to have \"true\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void notMatchingNameAndNotMatchingValuePass() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "headerNamePattern", headerNamePatternNotToMatch, "headerValuePattern", headerValuePatternNotToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1)); assertAssertionPassed(r);
        assertFalse("Expected assertion entry result to have \"false\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void emptyResultIfNoEntryFoundByUrlPattern() throws Exception {
        sendRequestsToTargetServer();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPatternNotToMatchUrl, "headerValuePattern", headerValueNotToFind));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get no assertion result entries", r.getRequests(), Matchers.hasSize(0)); assertAssertionPassed(r);
        conn.disconnect();
    }

    private void sendRequestsToTargetServer() throws Exception {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_HEADERS);
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, headers);
        mockTargetServerResponse(urlOfOldRequest, responseBody, headers);
        proxyManager.get().iterator().next().newHar();
        requestToTargetServer(urlOfOldRequest, responseBody);
        Thread.sleep(MILLISECONDS_BETWEEN_REQUESTS);
        requestToTargetServer(urlOfMostRecentRequest, responseBody);
    }

    protected void mockTargetServerResponse(String url, String responseBody, HttpHeader[] headers) {
        HttpHeader[] allHeaders = new HttpHeader[headers.length + 1];
        System.arraycopy(headers, 0, allHeaders, 0, headers.length);
        allHeaders[headers.length] = new HttpHeader("Content-Type", "text/plain");
        stubFor(get(urlEqualTo("/" + url)).willReturn(
                ok().withBody(responseBody).withHeaders(new HttpHeaders(allHeaders))));
    }
}
