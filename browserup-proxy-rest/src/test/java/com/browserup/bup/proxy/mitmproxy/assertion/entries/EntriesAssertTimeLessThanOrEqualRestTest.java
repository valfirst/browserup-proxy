package com.browserup.bup.proxy.mitmproxy.assertion.entries;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.assertion.model.AssertionEntryResult;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class EntriesAssertTimeLessThanOrEqualRestTest extends BaseRestTest {
    private String responseBody = "success";
    private String url = "some-url";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";

    @Override
    protected String getUrlPath() {
        return "har/entries/assertResponseTimeLessThanOrEqual";
    }

    @Test
    public void someEntriesFailTimeWithinAssertion() throws Exception {
        List<String> fastUrls = IntStream.rangeClosed(1, 2).mapToObj(i -> url + "-" + i).collect(Collectors.toList());
        List<String> slowUrls = IntStream.rangeClosed(3, 4).mapToObj(i -> url + "-" + i).collect(Collectors.toList());
        List<String> allUrls = new ArrayList<>(fastUrls);
        allUrls.addAll(slowUrls);

        for (String u : fastUrls) { mockTargetServerResponse(u, responseBody); }
        for (String u : slowUrls) { mockTargetServerResponse(u, responseBody, TARGET_SERVER_SLOW_RESPONSE_DELAY); }

        proxyManager.get().iterator().next().newHar();

        for (String u : allUrls) { requestToTargetServer(u, responseBody); }

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", urlPatternToMatchUrl, "milliseconds", SUCCESSFUL_ASSERTION_TIME_WITHIN));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertThat("Expected to get all assertion entries filtered by url pattern",
                assertionResult.getRequests(), Matchers.hasSize(allUrls.size()));
        assertAssertionFailed(assertionResult);

        for (AssertionEntryResult e : assertionResult.getRequests()) {
            if (fastUrls.stream().anyMatch(u -> e.getUrl().contains(u))) {
                assertFalse("Expected entry result for fast response to have failed flag = false", e.getFailed());
            }
            if (slowUrls.stream().anyMatch(u -> e.getUrl().contains(u))) {
                assertTrue("Expected entry result for slow response to have failed flag = true", e.getFailed());
            }
        }
        conn.disconnect();
    }

    @Test
    public void emptyResultIfNoEntriesFoundForTimeWithinAssertion() throws Exception {
        proxyManager.get().iterator().next().newHar();

        mockTargetServerResponse(url, responseBody);

        requestToTargetServer(url, responseBody);

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", urlPatternNotToMatchUrl, "milliseconds", SUCCESSFUL_ASSERTION_TIME_WITHIN));
        AssertionResult assertionResult = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(assertionResult);
        assertThat("Expected to get empty assertion entries found by url pattern",
                assertionResult.getRequests(), Matchers.hasSize(0));
        assertAssertionPassed(assertionResult);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfMillisecondsNotValid() throws Exception {
        proxyManager.get().iterator().next().newHar();

        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(),
                toStringMap("urlPattern", ".*", "milliseconds", "abcd"));
        int statusCode = conn.getResponseCode();
        assertEquals("Expected to get bad request", statusCode, HttpStatus.SC_BAD_REQUEST);
        conn.disconnect();
    }
}
