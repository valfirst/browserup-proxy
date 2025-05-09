package com.browserup.bup.proxy.assertion.field.header.filtered;

import java.io.IOException;
import java.util.List;

import com.browserup.bup.assertion.model.AssertionEntryResult;
import com.browserup.bup.assertion.model.AssertionResult;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

public class HeaderContainsTest extends FilteredHeaderBaseTest {

    @Test
    public void filterMatchesBothRequestsAndFilteredHeadersContainValuePasses() throws IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_BOTH, COMMON_HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void filterMatchesFirstRequestAndFilteredHeaderContainsValuePasses() throws IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_FIRST, FIRST_HEADER_VALUE);

        assertAssertionPassed(result);
    }

    @Test
    public void filterMatchesFirstRequestAndFilteredHeaderDoesNotContainValueFails() throws IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_FIRST,
                SECOND_HEADER_VALUE);

        assertAssertionFailed(result);
    }

    @Test
    public void filterMatchesNoRequestsPasses() throws IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_NOTHING,
                SECOND_HEADER_VALUE);

        assertAssertionPassed(result);
        assertAssertionHasNoEntries(result);
    }

    @Test
    public void filterMatchesBothRequestsAndFilteredHeadersDoNotContainValueFails() throws IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_BOTH, ABSENT_HEADER_VALUE);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected to get both assertion entries", failedRequests, hasSize(2));
    }

    @Test
    public void filterMatchesBothRequestsAndSomeHeadersDoNotContainValueFails() throws IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderContains(URL_PATTERN_TO_MATCH_BOTH, SECOND_HEADER_VALUE);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected to get both assertion entries", failedRequests, hasSize(1));
        assertThat("Expected failed assertion entry to have proper url", failedRequests.get(0).getUrl(),
                containsString(FIRST_URL_PATH));
    }

}
