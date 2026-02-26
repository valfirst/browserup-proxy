package com.browserup.bup.proxy.mitmproxy.assertion.entries.content;

import com.browserup.bup.assertion.model.AssertionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.hamcrest.MatcherAssert.assertThat;

public class EntriesAssertContentContainsRestTest extends BaseEntriesAssertContentRestTest {

    @Override
    protected String getUrlPath() {
        return "har/entries/assertContentContains";
    }

    @Test
    public void urlFilterMatchesBothAndContentContainsInBothPasses() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "contentText", RESPONSE_COMMON_PART));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertAssertionPassed(assertionResult);
        conn.disconnect();
    }

    @Test
    public void urlFilterMatchesBothAndContentDoesNotContainInSomeFails() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_BOTH, "contentText", FIRST_RESPONSE));
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
    public void urlFilterMatchesFirstAndContentDoesNotContainInFirstFails() throws Exception {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_FIRST, "contentText", SECOND_RESPONSE));
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
                toStringMap("urlPattern", URL_PATTERN_TO_MATCH_NOTHING, "contentText", RESPONSE_NOT_TO_FIND));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertAssertionPassed(assertionResult);
        conn.disconnect();
    }
}
