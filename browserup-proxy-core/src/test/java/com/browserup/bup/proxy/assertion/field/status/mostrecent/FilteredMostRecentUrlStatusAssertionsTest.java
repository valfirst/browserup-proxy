package com.browserup.bup.proxy.assertion.field.status.mostrecent;

import java.io.IOException;
import java.util.regex.Pattern;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;
import com.browserup.bup.util.HttpStatusClass;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilteredMostRecentUrlStatusAssertionsTest extends BaseAssertionsTest {

    protected static final Pattern COMMON_URL_PATTERN = Pattern.compile(".*some-url.*");
    protected static final Pattern NOT_TO_MATCH_PATTERN = Pattern.compile(".*will_not_match.*");
    protected static final String RECENT_PATH = "recent-some-url";
    protected static final String OLD_PATH = "old-some-url";

    @Test
    public void mostRecentUrlStatusCodeBelongsToClassPasses() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, HttpStatus.SC_CONFLICT);
        mockResponse(RECENT_PATH, status);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(COMMON_URL_PATTERN, HttpStatusClass.SUCCESS);

        assertThat("Expected to get one assertion entry", result.getRequests(), hasSize(1));
        assertThat("Expected assertion entry to have most recent url",
                result.getRequests().get(0).getUrl(), containsString(RECENT_PATH));
        assertTrue("Expected most recent response status to belong to the class", result.getPassed());
        assertFalse("Expected most recent response status to belong to the class", result.getFailed());
    }

    @Test
    public void mostRecentUrlStatusCodeBelongsToClassFails() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(COMMON_URL_PATTERN, HttpStatusClass.SUCCESS);

        assertThat("Expected to get one assertion entry", result.getRequests(), hasSize(1));
        assertThat("Expected assertion entry to have most recent url",
                result.getRequests().get(0).getUrl(), containsString(RECENT_PATH));
        assertFalse("Expected most recent response status not to belong to specified class", result.getPassed());
        assertTrue("Expected most recent response status not to belong to specified class", result.getFailed());
    }

    @Test
    public void mostRecentUrlStatusCodeEqualsPasses() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, HttpStatus.SC_BAD_REQUEST);
        mockResponse(RECENT_PATH, status);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(COMMON_URL_PATTERN, status);

        assertThat("Expected to get one assertion entry", result.getRequests(), hasSize(1));
        assertThat("Expected assertion entry to have most recent url",
                result.getRequests().get(0).getUrl(), containsString(RECENT_PATH));
        assertTrue("Expected status to pass assertion", result.getPassed());
        assertFalse("Expected status to pass assertion", result.getFailed());
    }

    @Test
    public void mostRecentUrlStatusCodeEqualsFails() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(COMMON_URL_PATTERN, status);

        assertThat("Expected to get one assertion entry", result.getRequests(), hasSize(1));
        assertThat("Expected assertion entry to have most recent url",
                result.getRequests().get(0).getUrl(), containsString(RECENT_PATH));
        assertFalse("Expected status to fail assertion", result.getPassed());
        assertTrue("Expected status to fail assertion", result.getFailed());
    }

    @Test
    public void noResponseFoundByUrlAndStatusBelongsToClassPasses() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, status);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(NOT_TO_MATCH_PATTERN, HttpStatusClass.SUCCESS);

        assertTrue("Expected to pass when no response found by url pattern", result.getPassed());
        assertFalse("Expected to pass when no response found by url pattern", result.getFailed());
    }

    @Test
    public void noResponseFoundByUrlAndStatusDoesNotBelongToClassPasses() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(NOT_TO_MATCH_PATTERN, HttpStatusClass.SUCCESS);

        assertTrue("Expected to pass when no response found by url pattern", result.getPassed());
        assertFalse("Expected to pass when no response found by url pattern", result.getFailed());
    }

    @Test
    public void noResponseFoundByUrlAndStatusEqualsPasses() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, status);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(NOT_TO_MATCH_PATTERN, status);

        assertTrue("Expected to pass when no response found by url pattern", result.getPassed());
        assertFalse("Expected to pass when no response found by url pattern", result.getFailed());
    }

    @Test
    public void noResponseFoundByUrlAndStatusNotEqualsPasses() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(NOT_TO_MATCH_PATTERN, status);

        assertTrue("Expected to pass when no response found by url pattern", result.getPassed());
        assertFalse("Expected to pass when no response found by url pattern", result.getFailed());
    }

    protected StubMapping mockResponse(String path, Integer status) {
        return stubFor(get(urlEqualTo("/" + path)).willReturn(aResponse().withStatus(status).withBody(SUCCESSFUL_RESPONSE_BODY)));
    }
}
