package com.browserup.bup.assertion;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MostRecentUrlResponseTimeLessThanOrEqualTest extends BaseAssertionsTest {

    @Test
    public void mostRecentUrlResponseTimeExceeds() throws IOException {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY);

        requestToMockedServer(URL_PATH);

        int assertionTime = DEFAULT_RESPONSE_DELAY - TIME_DELTA_MILLISECONDS;

        AssertionResult result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"), assertionTime);

        assertTrue("Expected failed flag to be true", result.getFailed());
        assertFalse("Expected passed flag to be true", result.getPassed());

        verify(1, getRequestedFor(urlMatching(".*" + URL_PATH + ".*")));
    }

    @Test
    public void passesIfNoEntriesFound() throws IOException {
        mockResponseForPathWithDelay(URL_PATH, 0);

        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile("^does not match?"),
                0);

        assertTrue("Expected passed flag to be true", result.getPassed());
        assertFalse("Expected failed flag to be true", result.getFailed());
        MatcherAssert.assertThat("Expected to get one har entry result", result.getRequests(), Matchers.empty());

        verify(1, getRequestedFor(urlMatching(".*" + URL_PATH + ".*")));
    }

    @Test
    public void mostRecentUrlResponseTimeLessThanOrEqual() throws IOException {
        mockResponseForPathWithDelay(URL_PATH, DEFAULT_RESPONSE_DELAY);

        requestToMockedServer(URL_PATH);

        int assertionTime = DEFAULT_RESPONSE_DELAY + TIME_DELTA_MILLISECONDS;

        AssertionResult result = proxy.assertMostRecentResponseTimeLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"), assertionTime);

        assertTrue("Expected passed flag to be true", result.getPassed());
        assertFalse("Expected failed flag to be false", result.getFailed());
        MatcherAssert.assertThat("Expected to get one har entry result", result.getRequests(), Matchers.hasSize(1));

        verify(1, getRequestedFor(urlMatching(".*" + URL_PATH + ".*")));
    }
}
