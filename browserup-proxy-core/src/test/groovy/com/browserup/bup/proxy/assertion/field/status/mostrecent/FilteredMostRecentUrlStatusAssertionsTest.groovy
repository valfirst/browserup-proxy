package com.browserup.bup.proxy.assertion.field.status.mostrecent

import com.browserup.bup.proxy.assertion.BaseAssertionsTest
import com.browserup.bup.util.HttpStatusClass
import org.apache.http.HttpStatus
import org.hamcrest.Matchers
import org.junit.Test

import java.util.regex.Pattern

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.*

class FilteredMostRecentUrlStatusAssertionsTest extends BaseAssertionsTest {
    protected static final Pattern COMMON_URL_PATTERN = Pattern.compile(".*some-url.*")
    protected static final Pattern NOT_TO_MATCH_PATTERN = Pattern.compile(".*will_not_match.*")
    protected static final String RECENT_PATH = "recent-some-url"
    protected static final String OLD_PATH = "old-some-url"

    @Test
    void mostRecentUrlStatusCodeBelongsToClassPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, HttpStatus.SC_CONFLICT)
        mockResponse(RECENT_PATH, status)

        requestToMockedServer(OLD_PATH)
        sleep(100)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertMostRecentResponseStatusCode(COMMON_URL_PATTERN, HttpStatusClass.SUCCESS)

        assertThat("Expected to get one assertion entry", result.requests, Matchers.hasSize(1))
        assertThat("Expected assertion entry to have most recent url", result.requests.get(0).url, Matchers.containsString(RECENT_PATH))
        assertTrue("Expected most recent response status to belong to the class", result.passed)
        assertFalse("Expected most recent response status to belong to the class", result.failed)
    }

    @Test
    void mostRecentUrlStatusCodeBelongsToClassFails() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST)

        requestToMockedServer(OLD_PATH)
        sleep(100)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertMostRecentResponseStatusCode(COMMON_URL_PATTERN, HttpStatusClass.SUCCESS)

        assertThat("Expected to get one assertion entry", result.requests, Matchers.hasSize(1))
        assertThat("Expected assertion entry to have most recent url", result.requests.get(0).url, Matchers.containsString(RECENT_PATH))
        assertFalse("Expected most recent response status not to belong to specified class", result.passed)
        assertTrue("Expected most recent response status not to belong to specified class", result.failed)
    }

    @Test
    void mostRecentUrlStatusCodeEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, HttpStatus.SC_BAD_REQUEST)
        mockResponse(RECENT_PATH, status)

        requestToMockedServer(OLD_PATH)
        sleep(100)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertMostRecentResponseStatusCode(COMMON_URL_PATTERN, status)

        assertThat("Expected to get one assertion entry", result.requests, Matchers.hasSize(1))
        assertThat("Expected assertion entry to have most recent url", result.requests.get(0).url, Matchers.containsString(RECENT_PATH))
        assertTrue("Expected status to pass assertion", result.passed)
        assertFalse("Expected status to pass assertion", result.failed)
    }

    @Test
    void mostRecentUrlStatusCodeEqualsFails() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST)

        requestToMockedServer(OLD_PATH)
        sleep(100)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertMostRecentResponseStatusCode(COMMON_URL_PATTERN, status)

        assertThat("Expected to get one assertion entry", result.requests, Matchers.hasSize(1))
        assertThat("Expected assertion entry to have most recent url", result.requests.get(0).url, Matchers.containsString(RECENT_PATH))
        assertFalse("Expected status to fail assertion", result.passed)
        assertTrue("Expected status to fail assertion", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusBelongsToClassPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, status)

        requestToMockedServer(OLD_PATH)
        sleep(100)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertMostRecentResponseStatusCode(NOT_TO_MATCH_PATTERN, HttpStatusClass.SUCCESS)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusDoesNotBelongToClassPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST)

        requestToMockedServer(OLD_PATH)
        sleep(100)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertMostRecentResponseStatusCode(NOT_TO_MATCH_PATTERN, HttpStatusClass.SUCCESS)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, status)

        requestToMockedServer(OLD_PATH)
        sleep(100)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertMostRecentResponseStatusCode(NOT_TO_MATCH_PATTERN, status)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    @Test
    void noResponseFoundByUrlAndStatusNotEqualsPasses() {
        def status = HttpStatus.SC_OK

        mockResponse(OLD_PATH, status)
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST)

        requestToMockedServer(OLD_PATH)
        sleep(100)
        requestToMockedServer(RECENT_PATH)

        def result = proxy.assertMostRecentResponseStatusCode(NOT_TO_MATCH_PATTERN, status)

        assertTrue("Expected to pass when no response found by url pattern", result.passed)
        assertFalse("Expected to pass when no response found by url pattern", result.failed)
    }

    protected mockResponse(String path, Integer status) {
        stubFor(get(urlEqualTo("/" + path))
                .willReturn(aResponse().withStatus(status).withBody(SUCCESSFUL_RESPONSE_BODY)))
    }
}
