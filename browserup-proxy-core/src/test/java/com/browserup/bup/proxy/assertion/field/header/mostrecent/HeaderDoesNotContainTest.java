package com.browserup.bup.proxy.assertion.field.header.mostrecent;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest;

import org.junit.Test;

public class HeaderDoesNotContainTest extends HeaderBaseTest {

    private static final Pattern URL_PATTERN = Pattern.compile(".*" + URL_PATH + ".*");

    @Test
    public void anyNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void nameNotProvidedAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, null,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionPassed(result);

        result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, "", NOT_MATCHING_HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void nameNotProvidedAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, null, HEADER_VALUE);

        assertAssertionFailed(result);

        result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, "", HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    public void anyNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    public void matchingNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_NAME,
                NOT_MATCHING_HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void notMatchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN,
                NOT_MATCHING_HEADER_NAME, HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void matchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderDoesNotContain(URL_PATTERN, HEADER_NAME,
                HEADER_VALUE);

        assertAssertionFailed(result);
    }
}
