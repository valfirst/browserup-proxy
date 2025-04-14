package com.browserup.bup.proxy.assertion.field.header.mostrecent;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest;

import org.junit.Test;

public class HeaderContainsTest extends HeaderBaseTest {

    private static final Pattern URL_PATTERN = Pattern.compile(".*" + URL_PATH + ".*");

    @Test
    public void anyNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void anyNameIfEmptyNameProvidedAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, null, HEADER_VALUE);

        assertAssertionPassed(result);

        result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, "", HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void matchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_NAME, HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void matchingNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, HEADER_NAME,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    public void notMatchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, NOT_MATCHING_HEADER_NAME,
                HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    public void notMatchingNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderContains(URL_PATTERN, NOT_MATCHING_HEADER_NAME,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionFailed(result);
    }
}
