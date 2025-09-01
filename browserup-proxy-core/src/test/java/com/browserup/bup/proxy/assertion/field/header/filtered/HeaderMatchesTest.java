package com.browserup.bup.proxy.assertion.field.header.filtered;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionEntryResult;
import com.browserup.bup.assertion.model.AssertionResult;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;

public class HeaderMatchesTest extends FilteredHeaderBaseTest {

    @Test
    public void urlFilterMatchesBothAndHeaderNameFilterMatchesBothAndHeaderValueFilterMatchesBothPasses() throws
            IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderMatches(URL_PATTERN_TO_MATCH_BOTH,
                HEADER_NAME_PATTERN_TO_MATCH_BOTH, HEADER_VALUE_PATTERN_TO_MATCH_BOTH);

        assertAssertionPassed(result);
    }

    @Test
    public void urlFilterMatchesBothAndAnyHeaderNameIsUsedAndHeaderValueFilterMatchesFirstFails() throws IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderMatches(URL_PATTERN_TO_MATCH_BOTH,
                HEADER_VALUE_PATTERN_TO_MATCH_FIRST);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected to get both assertion entries", failedRequests, hasSize(2));

        Optional<AssertionEntryResult> entryCorrespondingSecondRequest = failedRequests.stream()
                .filter(r -> r.getUrl().contains(SECOND_URL_PATH))
                .findFirst();

        assertTrue(entryCorrespondingSecondRequest.isPresent());
        assertThat(
                "Expected assertion entry corresponding second request to have second header name mentioned in message",
                entryCorrespondingSecondRequest.get().getMessage(), containsString(SECOND_HEADER_NAME));
    }

    @Test
    public void urlFilterMatchesFirstAndAnyHeaderNameIsUsedAndHeaderValueFilterMatchesFirstFails() throws IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderMatches(URL_PATTERN_TO_MATCH_FIRST,
                HEADER_VALUE_PATTERN_TO_MATCH_FIRST);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected to get one assertion entry", failedRequests, hasSize(1));

        assertThat(
                "Expected assertion entry corresponding first request not to have first header name mentioned in message",
                failedRequests.get(0).getMessage(), Matchers.not(containsString(FIRST_HEADER_NAME)));
    }

    @Test
    public void urlFilterMatchesFirstAndAnyHeaderNameIsUsedAndHeaderValueFilterMatchesAllPasses() throws IOException {
        Pattern headerValuePattern = Pattern.compile(".*");

        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderMatches(URL_PATTERN_TO_MATCH_FIRST, headerValuePattern);

        assertAssertionPassed(result);
    }

    @Test
    public void urlFilterMatchesNothingAndHeaderNameFilterMatchesFirstAndHeaderValueFilterMatchesFirstPasses() throws
            IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderMatches(URL_PATTERN_TO_MATCH_NOTHING,
                HEADER_NAME_PATTERN_TO_MATCH_FIRST, HEADER_VALUE_PATTERN_TO_MATCH_FIRST);

        assertAssertionPassed(result);
    }

    @Test
    public void urlFilterMatchesFirstAndHeaderNameFilterMatchesFirstAndHeaderValueFilterMatchesFirstPasses() throws
            IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderMatches(URL_PATTERN_TO_MATCH_FIRST,
                HEADER_NAME_PATTERN_TO_MATCH_FIRST, HEADER_VALUE_PATTERN_TO_MATCH_FIRST);

        assertAssertionPassed(result);
    }

    @Test
    public void urlFilterMatchesFirstAndHeaderNameFilterMatchesFirstAndHeaderValueFilterMatchesSecondFails() throws
            IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderMatches(URL_PATTERN_TO_MATCH_FIRST,
                HEADER_NAME_PATTERN_TO_MATCH_FIRST, HEADER_VALUE_PATTERN_TO_MATCH_SECOND);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected one assertion entry", failedRequests, hasSize(1));
        assertThat("Expected assertion entry to have proper url", failedRequests.get(0).getUrl(), containsString(FIRST_URL_PATH));
    }

    @Test
    public void urlFilterMatchesFirstAndHeaderNameFilterMatchesNothingAndHeaderValueFilterMatchesSecondPasses() throws
            IOException {
        mockAndSendRequestsToMockedServer(FIRST_HEADER, SECOND_HEADER);

        AssertionResult result = proxy.assertAnyUrlResponseHeaderMatches(URL_PATTERN_TO_MATCH_FIRST,
                HEADER_NAME_PATTERN_TO_MATCH_NOTHING, HEADER_VALUE_PATTERN_TO_MATCH_SECOND);

        assertAssertionPassed(result);
    }

}
