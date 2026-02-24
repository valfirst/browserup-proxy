package com.browserup.bup.proxy.mitmproxy.assertion.entries.status;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class EntriesAssertStatusServerErrorRestTest extends BaseRestTest {
    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String urlOfNotToMatchRequest = "not-to-match";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";
    private int serverErrorStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
    private int nonServerErrorStatus = HttpStatus.SC_OK;
    private int statusOfNotToMatchUrl = HttpStatus.SC_BAD_REQUEST;
    private String responseBody = "success";

    @Override
    protected String getUrlPath() { return "har/entries/assertStatusServerError"; }

    @Test
    public void getBadRequestIfUrlPatternIsInvalid() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", "["));
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpStatus.SC_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void statusServerErrorForFilteredResponsesPasses() throws Exception {
        sendRequestsToTargetServer(serverErrorStatus, serverErrorStatus, statusOfNotToMatchUrl);
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get all entries found by url pattern", r.getRequests(), Matchers.hasSize(2));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void statusServerErrorForAllResponsesPasses() throws Exception {
        sendRequestsToTargetServer(serverErrorStatus, serverErrorStatus, serverErrorStatus);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath());
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get all assertion entries", r.getRequests(), Matchers.hasSize(3));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void statusServerErrorForAllResponsesFails() throws Exception {
        sendRequestsToTargetServer(serverErrorStatus, serverErrorStatus, statusOfNotToMatchUrl);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath());
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get all assertion entries", r.getRequests(), Matchers.hasSize(3));
        assertAssertionFailed(r);
        conn.disconnect();
    }

    @Test
    public void statusServerErrorForFilteredResponsesFails() throws Exception {
        sendRequestsToTargetServer(serverErrorStatus, nonServerErrorStatus, statusOfNotToMatchUrl);
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
        sendRequestsToTargetServer(serverErrorStatus, nonServerErrorStatus, statusOfNotToMatchUrl);
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
