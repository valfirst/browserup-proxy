package com.browserup.bup.proxy.assertion.field.status.mostrecent;

import java.io.IOException;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;
import com.browserup.bup.util.HttpStatusClass;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import org.apache.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class MostRecentUrlStatusAssertionsTest extends BaseAssertionsTest {

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

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS);

        MatcherAssert.assertThat("Expected to get one assertion entry", result.getRequests(), Matchers.hasSize(1));
        MatcherAssert.assertThat("Expected assertion entry to have most recent url",
                result.getRequests().get(0).getUrl(), Matchers.containsString(RECENT_PATH));
        Assertions.assertTrue(result.getPassed(), "Expected most recent response status to belong to the class");
        Assertions.assertFalse(result.getFailed(), "Expected most recent response status to belong to the class");
    }

    @Test
    public void mostRecentUrlStatusCodeBelongsToClassFails() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS);

        MatcherAssert.assertThat("Expected to get one assertion entry", result.getRequests(), Matchers.hasSize(1));
        MatcherAssert.assertThat("Expected assertion entry to have most recent url",
                result.getRequests().get(0).getUrl(), Matchers.containsString(RECENT_PATH));
        Assertions.assertFalse(result.getPassed(), "Expected most recent response status not to belong to specified class");
        Assertions.assertTrue(result.getFailed(), "Expected most recent response status not to belong to specified class");
    }

    @Test
    public void mostRecentUrlStatusCodeEqualsPasses() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, HttpStatus.SC_BAD_REQUEST);
        mockResponse(RECENT_PATH, status);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(status);

        MatcherAssert.assertThat("Expected to get one assertion entry", result.getRequests(), Matchers.hasSize(1));
        MatcherAssert.assertThat("Expected assertion entry to have most recent url",
                result.getRequests().get(0).getUrl(), Matchers.containsString(RECENT_PATH));
        Assertions.assertTrue(result.getPassed(), "Expected status to pass assertion");
        Assertions.assertFalse(result.getFailed(), "Expected status to pass assertion");
    }

    @Test
    public void mostRecentUrlStatusCodeEqualsFails() throws IOException, InterruptedException {
        int status = HttpStatus.SC_OK;

        mockResponse(OLD_PATH, status);
        mockResponse(RECENT_PATH, HttpStatus.SC_BAD_REQUEST);

        requestToMockedServer(OLD_PATH);
        Thread.sleep(100);
        requestToMockedServer(RECENT_PATH);

        AssertionResult result = proxy.assertMostRecentResponseStatusCode(status);

        MatcherAssert.assertThat("Expected to get one assertion entry", result.getRequests(), Matchers.hasSize(1));
        MatcherAssert.assertThat("Expected assertion entry to have most recent url",
                result.getRequests().get(0).getUrl(), Matchers.containsString(RECENT_PATH));
        Assertions.assertFalse(result.getPassed(), "Expected status to fail assertion");
        Assertions.assertTrue(result.getFailed(), "Expected status to fail assertion");
    }

    @Test
    public void noResponseFoundByUrlAndStatusBelongsToClassPasses() {
        AssertionResult result = proxy.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS);

        Assertions.assertTrue(result.getPassed(), "Expected to pass when no response found by url pattern");
        Assertions.assertFalse(result.getFailed(), "Expected to pass when no response found by url pattern");
    }

    @Test
    public void noResponseFoundByUrlAndStatusDoesNotBelongToClassPasses() {
        AssertionResult result = proxy.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS);

        Assertions.assertTrue(result.getPassed(), "Expected to pass when no response found by url pattern");
        Assertions.assertFalse(result.getFailed(), "Expected to pass when no response found by url pattern");
    }

    @Test
    public void noResponseFoundByUrlAndStatusEqualsPasses() {
        AssertionResult result = proxy.assertMostRecentResponseStatusCode(HttpStatus.SC_OK);

        Assertions.assertTrue(result.getPassed(), "Expected to pass when no response found by url pattern");
        Assertions.assertFalse(result.getFailed(), "Expected to pass when no response found by url pattern");
    }

    @Test
    public void noResponseFoundByUrlAndStatusNotEqualsPasses() {
        AssertionResult result = proxy.assertMostRecentResponseStatusCode(HttpStatus.SC_OK);

        Assertions.assertTrue(result.getPassed(), "Expected to pass when no response found by url pattern");
        Assertions.assertFalse(result.getFailed(), "Expected to pass when no response found by url pattern");
    }

    protected StubMapping mockResponse(String path, Integer status) {
        return stubFor(get(urlEqualTo("/" + path)).willReturn(aResponse().withStatus(status).withBody(SUCCESSFUL_RESPONSE_BODY)));
    }
}
