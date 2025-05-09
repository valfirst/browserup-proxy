package com.browserup.bup.proxy.assertion.field.content;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContentSizeLessThanOrEqualTest extends ContentBaseTest {

    @Test
    public void contentSizeWithinAssertionPasses() throws IOException {
        int bodySize = 100;
        String body = StringUtils.repeat("a", bodySize);

        mockResponse(URL_PATH, body);

        requestToMockedServer(URL_PATH, body);

        AssertionResult result = proxy.assertMostRecentResponseContentLengthLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"),
                (long) bodySize);

        assertTrue("Expected assertion to pass", result.getPassed());
        assertFalse("Expected assertion to pass", result.getFailed());
    }

    @Test
    public void contentSizeWithinAssertionFails() throws IOException {
        int bodySize = 100;
        String body = StringUtils.repeat("a", bodySize);

        mockResponse(URL_PATH, body);

        requestToMockedServer(URL_PATH, body);

        AssertionResult result = proxy.assertMostRecentResponseContentLengthLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"),
                (long) (bodySize - 1));

        assertFalse("Expected assertion to fail", result.getPassed());
        assertTrue("Expected assertion to fail", result.getFailed());
    }

}
