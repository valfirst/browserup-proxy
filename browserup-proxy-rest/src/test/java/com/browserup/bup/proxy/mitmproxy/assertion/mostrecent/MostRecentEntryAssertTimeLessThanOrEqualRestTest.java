package com.browserup.bup.proxy.mitmproxy.assertion.mostrecent;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MostRecentEntryAssertTimeLessThanOrEqualRestTest extends BaseRestTest {
    private int successfulAssertionMilliseconds = SUCCESSFUL_ASSERTION_TIME_WITHIN;
    private int failedAssertionMilliseconds = FAILED_ASSERTION_TIME_WITHIN;
    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String commonUrlPattern = ".*url-.*";
    private String responseBody = "success";
    private String urlPattern = ".*does-not-match.*";
    private int assertionMilliseconds = SUCCESSFUL_ASSERTION_TIME_WITHIN;

    @Override
    protected String getUrlPath() { return "har/mostRecentEntry/assertResponseTimeLessThanOrEqual"; }

    @Test
    void passAndFailTimeWithinAssertion() throws Exception {
        sendRequestsToTargetServer();
        String urlPat = ".*" + commonUrlPattern;

        HttpURLConnection conn1 = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPat, "milliseconds", successfulAssertionMilliseconds));
        AssertionResult r1 = new ObjectMapper().readValue(readResponseBody(conn1), AssertionResult.class);
        assertAssertionNotNull(r1);
        assertThat("Expected to get one assertion result", r1.getRequests(), Matchers.hasSize(1));
        assertAssertionPassed(r1);
        assertFalse(r1.getRequests().get(0).getFailed(), "Expected assertion entry result to have \"false\" failed flag");
        conn1.disconnect();

        HttpURLConnection conn2 = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPat, "milliseconds", failedAssertionMilliseconds));
        AssertionResult r2 = new ObjectMapper().readValue(readResponseBody(conn2), AssertionResult.class);
        assertAssertionNotNull(r2);
        assertThat("Expected to get one assertion result", r2.getRequests(), Matchers.hasSize(1));
        assertAssertionFailed(r2);
        assertTrue(r2.getRequests().get(0).getFailed(), "Expected assertion entry result to have \"true\" failed flag");
        conn2.disconnect();
    }

    @Test
    void getEmptyResultIfNoEntryFoundByUrlPattern() throws Exception {
        sendRequestsToTargetServer();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "milliseconds", assertionMilliseconds));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get no assertion result entries", r.getRequests(), Matchers.hasSize(0));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    void getBadRequestIfMillisecondsNotValid() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", ".*", "milliseconds", "abcd"));
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, conn.getResponseCode(), "Expected to get bad request");
        conn.disconnect();
    }

    private void sendRequestsToTargetServer() throws Exception {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody, TARGET_SERVER_RESPONSE_DELAY);
        mockTargetServerResponse(urlOfOldRequest, responseBody);
        proxyManager.get().iterator().next().newHar();
        requestToTargetServer(urlOfOldRequest, responseBody);
        Thread.sleep(MILLISECONDS_BETWEEN_REQUESTS);
        requestToTargetServer(urlOfMostRecentRequest, responseBody);
    }
}
