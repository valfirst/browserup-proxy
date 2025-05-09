package com.browserup.bup.assertion;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AllUrlResponsesTimeWithinTest extends BaseAssertionsTest {

    @Test
    public void allUrlResponsesTimeExceeds() throws IOException {
        int numberOfCalls = 3;
        mockResponsesWithDelay(1, numberOfCalls, DEFAULT_RESPONSE_DELAY);

        AssertionResult result = runTest(numberOfCalls);

        assertTrue("Expected failed flag to be true", result.getFailed());
        assertFalse("Expected passed flag to be false", result.getPassed());

        result.getRequests().forEach(request ->
                assertTrue("Expected entry result to have failed flag = true", request.getFailed()));

        verify(numberOfCalls, getRequestedFor(urlMatching(".*")));
    }

    @Test
    public void someUrlResponsesTimeExceeds() throws IOException
    {
        mockResponsesWithDelay(1, 2, FAST_RESPONSE_DELAY);
        mockResponsesWithDelay(3, 4, DEFAULT_RESPONSE_DELAY);

        AssertionResult result = runTest(4);

        assertTrue("Expected failed flag to be true", result.getFailed());
        assertFalse("Expected passed flag to be false", result.getPassed());

        result.getRequests().stream()
                .filter(r -> r.getUrl().endsWith(URL_PATH + "1") || r.getUrl().endsWith(URL_PATH + "2"))
                .forEach(request ->
                        assertFalse("Expected entry result for fast response to have failed flag = false",
                                request.getFailed()));
        result.getRequests().stream()
                .filter(r -> r.getUrl().endsWith(URL_PATH + "3") || r.getUrl().endsWith(URL_PATH + "4"))
                .forEach(request ->
                        assertTrue("Expected entry result for slow response to have failed flag = true",
                                request.getFailed()));

        verify(4, getRequestedFor(urlMatching(".*")));
    }

    private AssertionResult runTest(int numberOfCalls) throws IOException {
        for (int i = 1; i <= numberOfCalls; i++) {
            HttpGet request = new HttpGet("http://localhost:" + mockServerPort + "/" + URL_PATH + i);

            String responseBody = NewProxyServerTestUtil.toStringAndClose(clientToProxy.execute(request).getEntity()
                    .getContent());
            assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY,
                    responseBody);
        }

        return proxy.assertResponseTimeLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"),
                DEFAULT_RESPONSE_DELAY - TIME_DELTA_MILLISECONDS);
    }

    private void mockResponsesWithDelay(int startInclusive, int endInclusive, int responseDelay) {
        IntStream.range(startInclusive, endInclusive + 1).forEach(
                i -> mockResponseForPathWithDelay(URL_PATH + i, responseDelay));
    }
}
