package com.browserup.bup.proxy.mitmproxy.assertion.entries.header;

import com.browserup.bup.assertion.model.AssertionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class EntriesAssertHeaderContainsRestTest extends BaseEntriesAssertHeaderRestTest {

    @Override
    protected String getUrlPath() {
        return "har/entries/assertResponseHeaderContains";
    }

    @Test
    public void urlFilterMatchesBothAndHeaderValueContainsInBothPasses() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "headerValue", COMMON_HEADER_VALUE));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(2));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesBothAndAnyHeaderNameAndHeaderValueMissedFails() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "headerValue", MISSING_HEADER_VALUE));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(2));
        assertAssertionFailed(r);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesFirstAndAnyHeaderNameAndHeaderValueMissedFails() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_FIRST, "headerValue", SECOND_HEADER_VALUE));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionFailed(r);
        assertThat("Expected to get one assertion entry", r.getFailedRequests(), Matchers.hasSize(1));
        assertThat("Expected assertion entry to have proper url",
                r.getFailedRequests().get(0).getUrl(), Matchers.containsString(URL_OF_FIRST_REQUEST));
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesFirstAndAnyHeaderNameAndHeaderValueContainsPassed() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_FIRST, "headerValue", FIRST_HEADER_VALUE));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesFirstAndFirstHeaderNameAndFirstHeaderValuePassed() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_FIRST, "headerName", FIRST_HEADER_NAME, "headerValue", FIRST_HEADER_VALUE));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesFirstAndSecondHeaderNameAndSecondHeaderValueContainsFails() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_FIRST, "headerName", SECOND_HEADER_NAME, "headerValue", SECOND_HEADER_VALUE));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionFailed(r);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesFirstAndFirstHeaderNameAndSecondHeaderValueContainsFails() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_FIRST, "headerName", FIRST_HEADER_NAME, "headerValue", SECOND_HEADER_VALUE));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionFailed(r);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesNonePasses() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "headerName", FIRST_HEADER_NAME, "headerValue", SECOND_HEADER_VALUE));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfHeaderValueNotProvided() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "headerName", FIRST_HEADER_NAME));
        int statusCode = conn.getResponseCode();
        assertEquals("Expected to get bad request", HttpStatus.SC_BAD_REQUEST, statusCode);
        conn.disconnect();
    }
}
