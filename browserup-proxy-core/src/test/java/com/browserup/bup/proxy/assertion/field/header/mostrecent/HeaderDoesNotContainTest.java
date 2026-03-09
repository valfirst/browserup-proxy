package com.browserup.bup.proxy.assertion.field.header.mostrecent;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest;

import org.junit.jupiter.api.Test;

class HeaderDoesNotContainTest extends HeaderBaseTest {

    private static final Pattern URL_PATTERN = Pattern.compile(".*" + URL_PATH + ".*");

    @Test
    void anyNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    void nameNotProvidedAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, null,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionPassed(result);

        result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, "", NOT_MATCHING_HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    void nameNotProvidedAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, null, HEADER_VALUE);

        assertAssertionFailed(result);

        result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, "", HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    void anyNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    void matchingNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_NAME,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    void notMatchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN,
                NOT_MATCHING_HEADER_NAME, HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    void matchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_NAME,
                HEADER_VALUE);

        assertAssertionFailed(result);
    }
}
