package com.browserup.bup.assertion;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MostRecentUrlResponseTimeLessThanOrEqualTest extends BaseAssertionsTest {

    @Test
    void mostRecentUrlResponseTimeExceeds() throws IOException {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY);

        requestToMockedServer(URL_PATH);

        int assertionTime = DEFAULT_RESPONSE_DELAY - TIME_DELTA_MILLISECONDS;

        AssertionResult result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"), assertionTime);

        assertTrue(result.getFailed(), "Expected failed flag to be true");
        assertFalse(result.getPassed(), "Expected passed flag to be true");

        verify(1, getRequestedFor(urlMatching(".*" + URL_PATH + ".*")));
    }

    @Test
    void passesIfNoEntriesFound() throws IOException {
        mockResponseForPathWithDelay(URL_PATH, 0);

        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile("^does not match?"),
                0);

        assertTrue(result.getPassed(), "Expected passed flag to be true");
        assertFalse(result.getFailed(), "Expected failed flag to be true");
        MatcherAssert.assertThat("Expected to get one har entry result", result.getRequests(), Matchers.empty());

        verify(1, getRequestedFor(urlMatching(".*" + URL_PATH + ".*")));
    }

    @Test
    void mostRecentUrlResponseTimeLessThanOrEqual() throws IOException {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY);

        requestToMockedServer(URL_PATH);

        int assertionTime = DEFAULT_RESPONSE_DELAY + TIME_DELTA_MILLISECONDS;

        AssertionResult result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"), assertionTime);

        assertTrue(result.getPassed(), "Expected passed flag to be true");
        assertFalse(result.getFailed(), "Expected failed flag to be false");
        MatcherAssert.assertThat("Expected to get one har entry result", result.getRequests(), Matchers.hasSize(1));

        verify(1, getRequestedFor(urlMatching(".*" + URL_PATH + ".*")));
    }
}
