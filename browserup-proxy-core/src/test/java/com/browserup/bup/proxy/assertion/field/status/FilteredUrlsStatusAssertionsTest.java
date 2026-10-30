package com.browserup.bup.proxy.assertion.field.status;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.browserup.bup.assertion.model.AssertionEntryResult;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;
import com.browserup.bup.util.HttpStatusClass;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilteredUrlsStatusAssertionsTest extends BaseAssertionsTest {

    protected static final Pattern COMMON_URL_PATTERN = Pattern.compile(".*some-url.*");
    protected static final Pattern NOT_TO_MATCH_PATTERN = Pattern.compile(".*will_not_match.*");
    protected static final String RECENT_PATH = "recent-some-url";
    protected static final String OLD_PATH = "old-some-url";

    @Test
    void filteredUrlsStatusesCodeBelongToClassPasses() throws IOException {
        mockResponse(OLD_PATH, HttpStatus.SC_OK);
        mockResponse(RECENT_PATH, HttpStatus.SC_CREATED);

        requestToMockedServer(OLD_PATH);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(COMMON_URL_PATTERN, HttpStatusClass.SUCCESS);

        assertThat("Expected to get proper assertion entries number", result.getRequests(), hasSize(2));
        assertTrue(result.getPassed(), "Expected filtered urls statuses to belong to specified class");
        assertFalse(result.getFailed(), "Expected filtered urls statuses to belong to specified class");
    }

    @Test
    void filteredUrlsStatusesCodeBelongToClassFail() throws IOException {
        mockResponse(OLD_PATH, HttpStatus.SC_OK);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(COMMON_URL_PATTERN, HttpStatusClass.SUCCESS);

        assertThat("Expected to get proper assertion entries number", result.getRequests(), hasSize(2));

        List<AssertionEntryResult> failedRequests = result.getRequests().stream()
                .filter(AssertionEntryResult::getFailed)
                .collect(Collectors.toList());

        assertThat("Expected to get one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed assertion entry to have proper url", failedRequests.get(0).getUrl(),
                containsString(RECENT_PATH));
        assertFalse(result.getPassed(), "Expected some of filtered urls statuses not to belong to specified class");
        assertTrue(result.getFailed(), "Expected some of filtered urls statuses not to belong to specified class");
    }

    @Test
    void filteredUrlStatusCodeEqualsPasses() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, status);

        requestToMockedServer(OLD_PATH);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(COMMON_URL_PATTERN, status);

        assertThat("Expected to get proper assertion entries number", result.getRequests(), hasSize(2));
        assertTrue(result.getPassed(), "Expected filtered urls statuses to have specified status");
        assertFalse(result.getFailed(), "Expected filtered urls statuses to have specified status");
    }

    @Test
    void filteredUrlStatusCodeEqualsFails() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_CREATED);

        requestToMockedServer(OLD_PATH);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(COMMON_URL_PATTERN, status);

        assertThat("Expected to get proper assertion entries number", result.getRequests(), hasSize(2));

        List<AssertionEntryResult> failedRequests = result.getRequests().stream()
                .filter(AssertionEntryResult::getFailed)
                .collect(Collectors.toList());

        assertThat("Expected to get one failed assertion entry", failedRequests, hasSize(1));
        assertThat("Expected failed assertion entry to have proper url", failedRequests.get(0).getUrl(),
                containsString(RECENT_PATH));
        assertFalse(result.getPassed(), "Expected some of filtered urls statuses not be equal to specified class");
        assertTrue(result.getFailed(), "Expected some of filtered urls statuses not be equal to specified class");
    }

    @Test
    void noResponseFoundByUrlAndStatusBelongsToClassPasses() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, status);

        requestToMockedServer(OLD_PATH);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(NOT_TO_MATCH_PATTERN, HttpStatusClass.SUCCESS);

        assertTrue(result.getPassed(), "Expected to pass when no response found by url pattern");
        assertFalse(result.getFailed(), "Expected to pass when no response found by url pattern");
    }

    @Test
    void noResponseFoundByUrlAndStatusDoesNotBelongToClassPasses() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(NOT_TO_MATCH_PATTERN, HttpStatusClass.SUCCESS);

        assertTrue(result.getPassed(), "Expected to pass when no response found by url pattern");
        assertFalse(result.getFailed(), "Expected to pass when no response found by url pattern");
    }

    @Test
    void noResponseFoundByUrlAndStatusEqualsPasses() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, status);

        requestToMockedServer(OLD_PATH);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(NOT_TO_MATCH_PATTERN, status);

        assertTrue(result.getPassed(), "Expected to pass when no response found by url pattern");
        assertFalse(result.getFailed(), "Expected to pass when no response found by url pattern");
    }

    @Test
    void noResponseFoundByUrlAndStatusNotEqualsPasses() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(NOT_TO_MATCH_PATTERN, status);

        assertTrue(result.getPassed(), "Expected to pass when no response found by url pattern");
        assertFalse(result.getFailed(), "Expected to pass when no response found by url pattern");
    }

    protected StubMapping mockResponse(String path, Integer status) {
        return WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/" + path)).willReturn(WireMock.aResponse().withStatus(status).withBody(SUCCESSFUL_RESPONSE_BODY)));
    }
}
