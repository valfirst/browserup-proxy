package com.browserup.bup.proxy.assertion.field.header.filtered;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.proxy.assertion.field.header.HeaderBaseTest;
import com.github.tomakehurst.wiremock.http.HttpHeader;

public class FilteredHeaderBaseTest extends HeaderBaseTest {
    protected static final String FIRST_URL_PATH = "first-url-path-with-" + URL_PATH + "-in-the-middle";
    protected static final String SECOND_URL_PATH = "second-url-path-with-" + URL_PATH + "-in-the-middle";
    protected static final Pattern URL_PATTERN_TO_MATCH_BOTH = Pattern.compile(".*" + URL_PATH + ".*");
    protected static final Pattern URL_PATTERN_TO_MATCH_FIRST = Pattern.compile(".*" + FIRST_URL_PATH + ".*");
    protected static final Pattern URL_PATTERN_TO_MATCH_NOTHING = Pattern.compile(".*match-nothing.*");
    protected static final String COMMON_HEADER_VALUE = "HeaderValue";
    protected static final String FIRST_HEADER_VALUE = "first" + COMMON_HEADER_VALUE;
    protected static final String SECOND_HEADER_VALUE = "second" + COMMON_HEADER_VALUE;
    protected static final String COMMON_HEADER_NAME = "HeaderName";
    protected static final String FIRST_HEADER_NAME = "first" + COMMON_HEADER_NAME;
    protected static final String SECOND_HEADER_NAME = "second" + COMMON_HEADER_NAME;
    protected static final Pattern HEADER_VALUE_PATTERN_TO_MATCH_FIRST = Pattern.compile(".*" + String.valueOf(FIRST_HEADER_VALUE) + ".*");
    protected static final Pattern HEADER_VALUE_PATTERN_TO_MATCH_SECOND = Pattern.compile(".*" + String.valueOf(SECOND_HEADER_VALUE) + ".*");
    protected static final Pattern HEADER_NAME_PATTERN_TO_MATCH_FIRST = Pattern.compile(".*" + String.valueOf(FIRST_HEADER_NAME) + ".*");
    protected static final Pattern HEADER_NAME_PATTERN_TO_MATCH_SECOND = Pattern.compile(".*" + String.valueOf(SECOND_HEADER_NAME) + ".*");
    protected static final Pattern HEADER_VALUE_PATTERN_TO_MATCH_BOTH = Pattern.compile(".*" + COMMON_HEADER_VALUE + ".*");
    protected static final Pattern HEADER_NAME_PATTERN_TO_MATCH_BOTH = Pattern.compile(".*" + COMMON_HEADER_NAME + ".*");
    protected static final Pattern HEADER_NAME_PATTERN_TO_MATCH_NOTHING = Pattern.compile(".*nothing.*");
    protected static final String ABSENT_HEADER_VALUE = "something";
    protected static final HttpHeader FIRST_HEADER = new HttpHeader("firstHeaderName", FIRST_HEADER_VALUE);
    protected static final HttpHeader SECOND_HEADER = new HttpHeader("secondHeaderName", SECOND_HEADER_VALUE);

    protected void mockAndSendRequestsToMockedServer(HttpHeader firstHeader, HttpHeader secondHeader) throws
            IOException {
        mockResponse(FIRST_URL_PATH, firstHeader);
        mockResponse(SECOND_URL_PATH, secondHeader);

        requestToMockedServer(FIRST_URL_PATH);
        requestToMockedServer(SECOND_URL_PATH);
    }
}
