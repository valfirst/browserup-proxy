package com.browserup.bup.proxy.assertion.field.content.mostrecent;

import java.io.IOException;

import com.browserup.bup.assertion.model.AssertionResult;

import org.junit.jupiter.api.Test;

class ContentDoesNotContainTest extends MostRecentContentBaseTest {

    @Test
    void oldAndRecentDoNotContainTextAndRecentFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentDoesNotContain(RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    void oldAndRecentDoNotContainTextAndOldFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentDoesNotContain(OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    void onlyOldDoesNotContainTextAndOldFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentDoesNotContain(OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    void onlyOldDoesNotContainTextAndRecentFilterIsUsedFails() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentDoesNotContain(RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionFailed(result);
    }
}
