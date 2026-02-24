package com.browserup.bup.proxy.mitmproxy.assertion.entries.content;

import com.browserup.bup.assertion.model.AssertionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class EntriesAssertContentLengthLessThanOrEqualRestTest extends BaseEntriesAssertContentRestTest {
    protected static final int FIRST_CONTENT_SIZE = FIRST_RESPONSE.getBytes().length;
    protected static final int SECOND_CONTENT_SIZE = SECOND_RESPONSE.getBytes().length;

    @Override
    protected String getUrlPath() {
        return "har/entries/assertContentLengthLessThanOrEqual";
    }

    @Test
    public void urlFilterMatchesBothAndContentLengthLessOrEqualPasses() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "length", SECOND_CONTENT_SIZE));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertAssertionPassed(assertionResult);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesBothAndSomeContentLengthExceedsForSomeFails() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "length", FIRST_CONTENT_SIZE));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertThat("Expected to get two assertion entries", assertionResult.getRequests(), Matchers.hasSize(2));
        assertAssertionFailed(assertionResult);

        assertThat("Expected to get one failed assertion entry", assertionResult.getFailedRequests(), Matchers.hasSize(1));
        assertThat("Expected failed entry to have proper request url",
                assertionResult.getFailedRequests().get(0).getUrl(),
                Matchers.containsString(URL_OF_SECOND_REQUEST));
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesFirstAndFirstContentLengthExceedsFails() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_FIRST, "length", FIRST_CONTENT_SIZE - 1));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertAssertionFailed(assertionResult);

        assertThat("Expected to get one assertion entry", assertionResult.getFailedRequests(), Matchers.hasSize(1));
        assertThat("Expected assertion entry to have proper url",
                assertionResult.getFailedRequests().get(0).getUrl(),
                Matchers.containsString(URL_OF_FIRST_REQUEST));
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesNonePasses() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "length", 1));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertAssertionPassed(assertionResult);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfLengthNotProvided() throws Exception {
        proxyManager.get().iterator().next().newHar();

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING));
        int statusCode = conn.getResponseCode();
        assertEquals("Expected to get bad request", HttpStatus.SC_BAD_REQUEST, statusCode);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfLengthNotValid() throws Exception {
        proxyManager.get().iterator().next().newHar();

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "length", "invalid"));
        int statusCode = conn.getResponseCode();
        assertEquals("Expected to get bad request", HttpStatus.SC_BAD_REQUEST, statusCode);
        conn.disconnect();
    }
}
