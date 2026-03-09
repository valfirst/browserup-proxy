package com.browserup.bup.proxy.mitmproxy.assertion.entries.header;

import com.browserup.bup.assertion.model.AssertionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EntriesAssertHeaderMatchesRestTest extends BaseEntriesAssertHeaderRestTest {
    protected static final String HEADER_NAME_PATTERN_TO_MATCH_FIRST = ".*" + FIRST_HEADER_NAME + ".*";
    protected static final String HEADER_NAME_PATTERN_TO_MATCH_BOTH = ".*" + COMMON_HEADER_NAME + ".*";
    protected static final String HEADER_VALUE_PATTERN_TO_MATCH_FIRST = ".*" + FIRST_HEADER_VALUE + ".*";
    protected static final String HEADER_VALUE_PATTERN_TO_MATCH_SECOND = ".*" + SECOND_HEADER_VALUE + ".*";
    protected static final String HEADER_VALUE_PATTERN_TO_MATCH_ALL = ".*";

    @Override
    protected String getUrlPath() { return "har/entries/assertResponseHeaderMatches"; }

    @Test
    void urlFilterMatchesBothAndAnyHeaderNameAndHeaderValueMatchesPasses() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "headerValuePattern", HEADER_VALUE_PATTERN_TO_MATCH_ALL));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(2)); assertAssertionPassed(r); conn.disconnect();
    }

    @Test
    void urlFilterMatchesBothAndAnyHeaderNameAndHeaderValueDoesNotMatchesFails() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "headerValuePattern", HEADER_VALUE_PATTERN_TO_MATCH_FIRST));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(2)); assertAssertionFailed(r); conn.disconnect();
    }

    @Test
    void urlFilterMatchesBothAndHeaderNameMatchesFirstAndHeaderValueMatchesFirstPasses() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "headerNamePattern", HEADER_NAME_PATTERN_TO_MATCH_FIRST, "headerValuePattern", HEADER_VALUE_PATTERN_TO_MATCH_FIRST));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(2)); assertAssertionPassed(r); conn.disconnect();
    }

    @Test
    void urlFilterMatchesBothAndHeaderNameMatchesFirstAndHeaderValueMatchesSecondFails() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "headerNamePattern", HEADER_NAME_PATTERN_TO_MATCH_FIRST, "headerValuePattern", HEADER_VALUE_PATTERN_TO_MATCH_SECOND));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(2)); assertAssertionFailed(r);
        assertThat("Expected to get one failed assertion entry", r.getFailedRequests(), Matchers.hasSize(1));
        assertThat("Expected assertion entry to have proper url", r.getFailedRequests().get(0).getUrl(), Matchers.containsString(URL_OF_FIRST_REQUEST));
        conn.disconnect();
    }

    @Test
    void urlFilterMatchesBothAndHeaderNameMatchesBothAndHeaderValueMatchesSecondFails() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "headerNamePattern", HEADER_NAME_PATTERN_TO_MATCH_BOTH, "headerValuePattern", HEADER_VALUE_PATTERN_TO_MATCH_SECOND));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(2)); assertAssertionFailed(r);
        assertThat("Expected to get one failed assertion entry", r.getFailedRequests(), Matchers.hasSize(1));
        assertThat("Expected assertion entry to have proper url", r.getFailedRequests().get(0).getUrl(), Matchers.containsString(URL_OF_FIRST_REQUEST));
        conn.disconnect();
    }

    @Test
    void urlFilterMatchesNonePasses() throws Exception {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "headerNamePattern", HEADER_NAME_PATTERN_TO_MATCH_BOTH, "headerValuePattern", HEADER_VALUE_PATTERN_TO_MATCH_SECOND));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r); assertAssertionPassed(r); conn.disconnect();
    }

    @Test
    void getBadRequestIfHeaderValuePatternNotProvided() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "headerNamePattern", HEADER_NAME_PATTERN_TO_MATCH_BOTH));
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, conn.getResponseCode(), "Expected to get bad request"); conn.disconnect();
    }

    @Test
    void getBadRequestIfHeaderValuePatternNotValid() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "headerNamePattern", HEADER_NAME_PATTERN_TO_MATCH_BOTH, "headerValuePattern", "["));
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, conn.getResponseCode(), "Expected to get bad request"); conn.disconnect();
    }

    @Test
    void getBadRequestIfHeaderNamePatternNotValid() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "headerNamePattern", "[", "headerValuePattern", HEADER_VALUE_PATTERN_TO_MATCH_SECOND));
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, conn.getResponseCode(), "Expected to get bad request"); conn.disconnect();
    }
}
