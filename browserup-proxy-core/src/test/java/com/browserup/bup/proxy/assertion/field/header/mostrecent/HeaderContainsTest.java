package com.browserup.bup.proxy.assertion.field.header.mostrecent;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest;

import org.junit.jupiter.api.Test;

class HeaderContainsTest extends HeaderBaseTest {

    private static final Pattern URL_PATTERN = Pattern.compile(".*" + URL_PATH + ".*");

    @Test
    void anyNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    void anyNameIfEmptyNameProvidedAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, null, HEADER_VALUE);

        assertAssertionPassed(result);

        result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, "", HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    void matchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_NAME, HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    void matchingNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_NAME,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    void notMatchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, NOT_MATCHING_HEADER_NAME,
                HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    void notMatchingNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, NOT_MATCHING_HEADER_NAME,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionFailed(result);
    }
}
