package com.browserup.bup.proxy.assertion.field.content.mostrecent;

import java.io.IOException;

import com.browserup.bup.assertion.model.AssertionResult;

import org.junit.Test;

public class ContentContainsTest extends MostRecentContentBaseTest {

    @Test
    public void oldAndRecentContainsTextAndRecentFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentContains(RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void oldAndRecentContainsTextAndOldFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentContains(OLD_REQUEST_URL_PATH_PATTERN, BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void onlyOldContainsTextAndOldFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentContains(OLD_REQUEST_URL_PATH_PATTERN, BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void onlyOldContainsTextAndRecentFilterIsUsedFails() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentContains(RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PART);

        assertAssertionFailed(result);
    }
}
