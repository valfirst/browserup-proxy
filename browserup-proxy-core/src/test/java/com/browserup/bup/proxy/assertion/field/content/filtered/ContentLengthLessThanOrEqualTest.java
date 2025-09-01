package com.browserup.bup.proxy.assertion.field.content.filtered;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.browserup.bup.assertion.model.AssertionEntryResult;
import com.browserup.bup.assertion.model.AssertionResult;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

public class ContentLengthLessThanOrEqualTest extends FilteredContentBaseTest {

    private static final String BIG_BODY = IntStream.range(0, 10).mapToObj(i -> "big body").collect(Collectors.joining(" "));
    private static final String SMALL_BODY = "small body";
    private static final long BIG_BODY_SIZE = BIG_BODY.getBytes().length;
    private static final long SMALL_BODY_SIZE = SMALL_BODY.getBytes().length;

    @Test
    public void filterMatchesBothRequestsAndBothContentLengthAreUnderLimitPasses() throws IOException {
        mockAndSendRequestsToMockedServer(BIG_BODY, SMALL_BODY);

        AssertionResult result = proxy.assertAnyUrlContentLengthLessThanOrEquals(URL_PATTERN_TO_MATCH_BOTH,
                BIG_BODY_SIZE);

        assertAssertionPassed(result);
    }

    @Test
    public void filterMatchesFirstRequestAndOnlySecondContentLengthIsUnderLimitFails() throws IOException {
        mockAndSendRequestsToMockedServer(BIG_BODY, SMALL_BODY);

        AssertionResult result = proxy.assertAnyUrlContentLengthLessThanOrEquals(URL_PATTERN_TO_MATCH_FIRST,
                SMALL_BODY_SIZE);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected there's one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed entry has proper url", failedRequests.get(0).getUrl(), containsString(FIRST_URL_PATH));
    }

    @Test
    public void filterMatchesBothRequestsAndSomeContentIsNotUnderLimitFails() throws IOException {
        mockAndSendRequestsToMockedServer(BIG_BODY, SMALL_BODY);

        AssertionResult result = proxy.assertAnyUrlContentLengthLessThanOrEquals(URL_PATTERN_TO_MATCH_BOTH,
                SMALL_BODY_SIZE);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected there's one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed entry has proper url", failedRequests.get(0).getUrl(), containsString(FIRST_URL_PATH));
    }

    @Test
    public void filterMatchesBothRequestsAndAllContentIsNotUnderLimitFails() throws IOException {
        mockAndSendRequestsToMockedServer(BIG_BODY, SMALL_BODY);

        AssertionResult result = proxy.assertAnyUrlContentLengthLessThanOrEquals(URL_PATTERN_TO_MATCH_BOTH,
                SMALL_BODY_SIZE - 1);

        assertAssertionFailed(result);

        List<AssertionEntryResult> failedRequests = result.getFailedRequests();

        assertThat("Expected to find both assertion entries", failedRequests, hasSize(2));
    }
}
