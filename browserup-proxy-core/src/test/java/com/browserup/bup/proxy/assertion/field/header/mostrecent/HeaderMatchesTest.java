package com.browserup.bup.proxy.assertion.field.header.mostrecent;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderMatchesTest extends HeaderBaseTest {

    @Test
    void anyNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderMatches(Pattern.compile(".*" + URL_PATH + ".*"),
                Pattern.compile(".*"));

        assertAssertionPassed(result);
    }

    @Test
    void anyNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderMatches(Pattern.compile(".*" + URL_PATH + ".*"),
                Pattern.compile(".*" + NOT_MATCHING_HEADER_VALUE + ".*"));

        assertFalse(result.getPassed(), "Expected headers not to match value pattern");
        assertTrue(result.getFailed(), "Expected headers not to match value pattern");
    }

    @Test
    void emptyNameProvidedAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderMatches(Pattern.compile(".*" + URL_PATH + ".*"),
                null, Pattern.compile(".*"));

        assertAssertionPassed(result);
    }

    @Test
    void matchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderMatches(Pattern.compile(".*" + URL_PATH + ".*"),
                Pattern.compile(".*" + HEADER_NAME + ".*"), Pattern.compile(".*" + HEADER_VALUE + ".*"));

        assertAssertionPassed(result);
    }

    @Test
    void matchingNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderMatches(Pattern.compile(".*" + URL_PATH + ".*"),
                Pattern.compile(".*" + HEADER_NAME + ".*"), Pattern.compile(".*" + NOT_MATCHING_HEADER_VALUE + ".*"));

        assertAssertionFailed(result);
    }

    @Test
    void notMatchingNameAndMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderMatches(Pattern.compile(".*" + URL_PATH + ".*"),
                Pattern.compile(".*" + NOT_MATCHING_HEADER_NAME + ".*"), Pattern.compile(".*" + HEADER_VALUE + ".*"));

        assertAssertionPassed(result);
    }

    @Test
    void notMatchingNameAndNotMatchingValue() throws IOException {
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertMostRecentResponseHeaderMatches(Pattern.compile(".*" + URL_PATH + ".*"),
                Pattern.compile(".*" + NOT_MATCHING_HEADER_NAME + ".*"), Pattern.compile(".*" + NOT_MATCHING_HEADER_VALUE + ".*"));

        assertAssertionPassed(result);
    }
}
