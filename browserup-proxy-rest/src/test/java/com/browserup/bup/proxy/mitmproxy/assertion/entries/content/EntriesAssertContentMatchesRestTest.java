package com.browserup.bup.proxy.mitmproxy.assertion.entries.content;

import com.browserup.bup.assertion.model.AssertionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class EntriesAssertContentMatchesRestTest extends BaseEntriesAssertContentRestTest {
    protected static final String CONTENT_PATTERN_TO_MATCH_BOTH = ".*" + RESPONSE_COMMON_PART + ".*";
    protected static final String CONTENT_PATTERN_TO_MATCH_FIRST = ".*" + FIRST_RESPONSE + ".*";
    protected static final String CONTENT_PATTERN_TO_MATCH_SECOND = ".*" + SECOND_RESPONSE + ".*";

    @Override
    protected String getUrlPath() {
        return "har/entries/assertContentMatches";
    }

    @Test
    public void urlFilterMatchesBothAndContentFilterMatchesBothPasses() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "contentPattern", CONTENT_PATTERN_TO_MATCH_BOTH));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertAssertionPassed(assertionResult);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesBothAndContentFilterDoesNotMatchSomeFails() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "contentPattern", CONTENT_PATTERN_TO_MATCH_FIRST));
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
    public void urlFilterMatchesFirstAndContentFilterDoesNotMatchFirstFails() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_FIRST, "contentPattern", CONTENT_PATTERN_TO_MATCH_SECOND));
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
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "contentPattern", CONTENT_PATTERN_TO_MATCH_SECOND));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertAssertionPassed(assertionResult);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfContentPatternNotProvided() throws Exception {
        proxyManager.get().iterator().next().newHar();

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING));
        int statusCode = conn.getResponseCode();
        assertEquals("Expected to get bad request", HttpURLConnection.HTTP_BAD_REQUEST, statusCode);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfUrlPatternNotValid() throws Exception {
        proxyManager.get().iterator().next().newHar();

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "contentPattern", "["));
        int statusCode = conn.getResponseCode();
        assertEquals("Expected to get bad request", HttpURLConnection.HTTP_BAD_REQUEST, statusCode);
        conn.disconnect();
    }
}
