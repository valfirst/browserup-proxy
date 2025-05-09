package com.browserup.bup.proxy.assertion.field.content.mostrecent;

import java.io.IOException;

import com.browserup.bup.assertion.model.AssertionResult;

import org.junit.Test;

public class ContentDoesNotContainTest extends MostRecentContentBaseTest {

    @Test
    public void oldAndRecentDoNotContainTextAndRecentFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentDoesNotContain(RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void oldAndRecentDoNotContainTextAndOldFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentDoesNotContain(OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void onlyOldDoesNotContainTextAndOldFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentDoesNotContain(OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void onlyOldDoesNotContainTextAndRecentFilterIsUsedFails() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentDoesNotContain(RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionFailed(result);
    }
}
