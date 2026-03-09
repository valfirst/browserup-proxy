package com.browserup.bup.proxy.assertion.field.content;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentSizeLessThanOrEqualTest extends ContentBaseTest {

    @Test
    public void contentSizeWithinAssertionPasses() throws IOException {
        int bodySize = 100;
        String body = StringUtils.repeat("a", bodySize);

        mockResponse(URL_PATH, body);

        requestToMockedServer(URL_PATH, body);

        AssertionResult result = proxy.assertMostRecentResponseContentLengthLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"),
                (long) bodySize);

        assertTrue(result.getPassed(), "Expected assertion to pass");
        assertFalse(result.getFailed(), "Expected assertion to pass");
    }

    @Test
    public void contentSizeWithinAssertionFails() throws IOException {
        int bodySize = 100;
        String body = StringUtils.repeat("a", bodySize);

        mockResponse(URL_PATH, body);

        requestToMockedServer(URL_PATH, body);

        AssertionResult result = proxy.assertMostRecentResponseContentLengthLessThanOrEqual(Pattern.compile(".*" + URL_PATH + ".*"),
                (long) (bodySize - 1));

        assertFalse(result.getPassed(), "Expected assertion to fail");
        assertTrue(result.getFailed(), "Expected assertion to fail");
    }

}
