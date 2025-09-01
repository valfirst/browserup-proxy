package com.browserup.bup.proxy.assertion.field.content.filtered;

import java.io.IOException;
import java.util.List;

import com.browserup.bup.assertion.model.AssertionEntryResult;
import com.browserup.bup.assertion.model.AssertionResult;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

public class ContentContainsTest extends FilteredContentBaseTest {

    @Test
    public void filterMatchesBothRequestsAndBothContentContainTextPasses() throws IOException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertAnyUrlContentContains(URL_PATTERN_TO_MATCH_BOTH, BODY_PART);

        assertAssertionPassed(result);
    }

    @Test
    public void filterMatchesFirstRequestAndOnlySecondContentContainTextFails() throws IOException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertAnyUrlContentContains(URL_PATTERN_TO_MATCH_FIRST, BODY_PART);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected there's one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed entry has proper url", failedRequests.get(0).getUrl(), containsString(FIRST_URL_PATH));
    }

    @Test
    public void filterMatchesBothRequestsAndSomeContentDoesNotContainTextFails_1() throws IOException {
        mockAndSendRequestsToMockedServer(BODY_CONTAINING_BODY_PART, BODY_NOT_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertAnyUrlContentContains(URL_PATTERN_TO_MATCH_BOTH, BODY_PART);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected there's one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed entry has proper url", failedRequests.get(0).getUrl(), containsString(SECOND_URL_PATH));
    }

    @Test
    public void filterMatchesBothRequestsAndSomeContentDoesNotContainTextFails_2() throws IOException {
        mockAndSendRequestsToMockedServer(BODY_NOT_CONTAINING_BODY_PART, BODY_CONTAINING_BODY_PART);

        AssertionResult result = proxy.assertAnyUrlContentContains(URL_PATTERN_TO_MATCH_BOTH, BODY_PART);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected there's one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed entry has proper url", failedRequests.get(0).getUrl(), containsString(FIRST_URL_PATH));
    }

}
