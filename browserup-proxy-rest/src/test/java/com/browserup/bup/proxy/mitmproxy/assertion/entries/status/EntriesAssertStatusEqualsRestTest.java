package com.browserup.bup.proxy.mitmproxy.assertion.entries.status;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class EntriesAssertStatusEqualsRestTest extends BaseRestTest {
    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";
    private int status = HttpURLConnection.HTTP_OK;
    private int statusNotToMatch = HttpURLConnection.HTTP_NOT_FOUND;
    private String responseBody = "success";

    @Override
    protected String getUrlPath() { return "har/mostRecentEntry/assertStatusEquals"; }

    @Test
    public void getBadRequestIfStatusValidButUrlPatternIsInvalid() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", "[", "status", HttpURLConnection.HTTP_OK));
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfStatusNotProvided() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath());
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfUrlPatternIsInvalid() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", "[", "status", status));
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void statusEqualsPasses() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "status", status));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionPassed(r);
        assertFalse("Expected assertion entry result to have \"false\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void statusEqualsFails() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "status", statusNotToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionFailed(r);
        assertTrue("Expected assertion entry result to have \"true\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void getEmptyResultIfNoEntryFoundByUrlPattern() throws Exception {
        sendRequestsToTargetServer();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPatternNotToMatchUrl, "status", status));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get no assertion result entries", r.getRequests(), Matchers.hasSize(0));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    private void sendRequestsToTargetServer() throws Exception {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, status);
        mockTargetServerResponse(urlOfOldRequest, responseBody, status);
        proxyManager.get().iterator().next().newHar();
        requestToTargetServer(urlOfOldRequest, responseBody);
        Thread.sleep(MILLISECONDS_BETWEEN_REQUESTS);
        requestToTargetServer(urlOfMostRecentRequest, responseBody);
    }

    protected void mockTargetServerResponse(String url, String responseBody, int status) {
        stubFor(get(urlEqualTo("/" + url)).willReturn(
                aResponse().withStatus(status).withBody(responseBody).withHeader("Content-Type", "text/plain")));
    }
}
