package com.browserup.bup.proxy.mitmproxy.assertion.mostrecent.content;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import org.hamcrest.Matchers;
import org.junit.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class MostRecentEntryAssertContentLengthLessThanOrEqualRestTest extends BaseRestTest {
    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";
    private String responseBody = "success";
    private int lengthToMatch = responseBody.getBytes().length;
    private int lengthNotToMatch = lengthToMatch - 1;

    @Override
    protected String getUrlPath() { return "har/mostRecentEntry/assertContentLengthLessThanOrEqual"; }

    @Test
    public void getBadRequestIfLengthNotProvided() throws Exception {
        proxyManager.get().iterator().next().newHar();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern));
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfUrlPatternNotProvided() throws Exception {
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath());
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfLengthNotValid() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "length", "invalidlength"));
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void contentLengthLessThanOrEqualPasses() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "length", lengthToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionPassed(r);
        assertFalse("Expected assertion entry result to have \"false\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void contentLengthLessThanOrEqualFails() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "length", lengthNotToMatch));
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
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPatternNotToMatchUrl, "length", lengthNotToMatch));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get no assertion result entries", r.getRequests(), Matchers.hasSize(0));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    private void sendRequestsToTargetServer() throws Exception {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_CONTENT);
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody);
        mockTargetServerResponse(urlOfOldRequest, responseBody);
        proxyManager.get().iterator().next().newHar();
        requestToTargetServer(urlOfOldRequest, responseBody);
        Thread.sleep(MILLISECONDS_BETWEEN_REQUESTS);
        requestToTargetServer(urlOfMostRecentRequest, responseBody);
    }
}
