package com.browserup.bup.proxy.assertion.field.content.filtered;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.proxy.assertion.field.content.ContentBaseTest;

public class FilteredContentBaseTest extends ContentBaseTest {

    protected void mockAndSendRequestsToMockedServer(String firstBody, String secondBody) throws IOException {
        mockResponse(FIRST_URL_PATH, firstBody);
        mockResponse(SECOND_URL_PATH, secondBody);

        requestToMockedServer(FIRST_URL_PATH, firstBody);
        requestToMockedServer(SECOND_URL_PATH, secondBody);
    }

    protected static final String FIRST_URL_PATH = "first-url-path-with-" + URL_PATH + "-in-the-middle";
    protected static final String SECOND_URL_PATH = "second-url-path-with-" + URL_PATH + "-in-the-middle";
    protected static final Pattern URL_PATTERN_TO_MATCH_BOTH = Pattern.compile(".*" + URL_PATH + ".*");
    protected static final Pattern URL_PATTERN_TO_MATCH_FIRST = Pattern.compile(".*" + FIRST_URL_PATH + ".*");
}
