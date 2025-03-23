package com.browserup.bup.proxy.assertion.field.content.mostrecent;

import java.io.IOException;
import java.util.List;

import com.browserup.bup.assertion.model.AssertionEntryResult;
import com.browserup.bup.assertion.model.AssertionResult;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

public class ContentMatchesTest extends MostRecentContentBaseTest {

    @Test
    public void oldAndRecentMatchesAndRecentFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentMatches(RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PATTERN_TO_MATCH_BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void oldAndRecentMatchesAndOldFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentMatches(OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PATTERN_TO_MATCH_BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void onlyOldMatchesAndOldFilterIsUsedPasses() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentMatches(OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PATTERN_TO_MATCH_BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void onlyOldMatchesAndRecentFilterIsUsedFails() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentMatches(RECENT_REQUEST_URL_PATH_PATTERN,
                BODY_PATTERN_TO_MATCH_BODY_PART);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected there's one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed entry has proper url", failedRequests.get(0).getUrl(), containsString(RECENT_REQUEST_URL_PATH));
    }

    @Test
    public void onlyRecentMatchesAndOldFilterIsUsedFails() throws IOException, InterruptedException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertMostRecentResponseContentMatches(OLD_REQUEST_URL_PATH_PATTERN,
                BODY_PATTERN_TO_MATCH_BODY_PART);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected there's one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed entry has proper url", failedRequests.get(0).getUrl(), containsString(OLD_REQUEST_URL_PATH));
    }

}
