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

public class EntriesAssertStatusClientErrorRestTest extends BaseRestTest {
    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String urlOfNotToMatchRequest = "not-to-match";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";
    private int clientErrorStatus = HttpURLConnection.HTTP_BAD_REQUEST;
    private int nonClientErrorStatus = HttpURLConnection.HTTP_OK;
    private int statusOfNotToMatchUrl = HttpURLConnection.HTTP_INTERNAL_ERROR;
    private String responseBody = "success";

    @Override
    protected String getUrlPath() { return "har/entries/assertStatusClientError"; }

    @Test
    public void getBadRequestIfUrlPatternIsInvalid() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", "["));
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void statusClientErrorForFilteredResponsesPasses() throws Exception {
        sendRequestsToTargetServer(clientErrorStatus, clientErrorStatus, statusOfNotToMatchUrl);
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get all entries found by url pattern", r.getRequests(), Matchers.hasSize(2));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void statusClientErrorForAllResponsesPasses() throws Exception {
        sendRequestsToTargetServer(clientErrorStatus, clientErrorStatus, clientErrorStatus);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath());
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get all assertion entries", r.getRequests(), Matchers.hasSize(3));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void statusClientErrorForAllResponsesFails() throws Exception {
        sendRequestsToTargetServer(clientErrorStatus, clientErrorStatus, statusOfNotToMatchUrl);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath());
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get all assertion entries", r.getRequests(), Matchers.hasSize(3));
        assertAssertionFailed(r);
        conn.disconnect();
    }

    @Test
    public void statusClientErrorForFilteredResponsesFails() throws Exception {
        sendRequestsToTargetServer(clientErrorStatus, nonClientErrorStatus, statusOfNotToMatchUrl);
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get all entries found by url pattern", r.getRequests(), Matchers.hasSize(2));
        assertAssertionFailed(r);
        AssertionResult.class.getName(); // keep import
        assertTrue("Expected failed assertion entry result has \"true\" failed flag",
                r.getRequests().stream().filter(e -> e.getFailed()).findFirst().get().getFailed());
        conn.disconnect();
    }

    @Test
    public void getEmptyResultIfNoEntryFoundByUrlPattern() throws Exception {
        sendRequestsToTargetServer(clientErrorStatus, nonClientErrorStatus, statusOfNotToMatchUrl);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPatternNotToMatchUrl));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get no assertion result entries", r.getRequests(), Matchers.hasSize(0));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    private void sendRequestsToTargetServer(int oldStatus, int recentStatus, int statusOfNotToMatchUrl) throws Exception {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, recentStatus);
        mockTargetServerResponse(urlOfOldRequest, responseBody, oldStatus);
        mockTargetServerResponse(urlOfNotToMatchRequest, responseBody, statusOfNotToMatchUrl);
        proxyManager.get().iterator().next().newHar();
        requestToTargetServer(urlOfOldRequest, responseBody);
        requestToTargetServer(urlOfMostRecentRequest, responseBody);
        requestToTargetServer(urlOfNotToMatchRequest, responseBody);
    }

    protected void mockTargetServerResponse(String url, String responseBody, int status) {
        stubFor(get(urlEqualTo("/" + url)).willReturn(
                aResponse().withStatus(status).withBody(responseBody).withHeader("Content-Type", "text/plain").withHeader("Location", "test.com")));
    }
}
