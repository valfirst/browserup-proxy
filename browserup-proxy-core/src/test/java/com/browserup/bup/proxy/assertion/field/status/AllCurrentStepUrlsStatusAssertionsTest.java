package com.browserup.bup.proxy.assertion.field.status;

import java.io.IOException;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;
import com.browserup.bup.util.HttpStatusClass;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllCurrentStepUrlsStatusAssertionsTest extends BaseAssertionsTest {

    @Test
    public void currentUrlsStatusCodesBelongToClassPass() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(URL_PATH, status);
        mockResponse(URL_PATH, status);

        requestToMockedServer(URL_PATH);
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(HttpStatusClass.SUCCESS);

        assertTrue(result.getPassed(), "Expected statuses of all responses to have the same class");
        assertFalse(result.getFailed(), "Expected statuses of all responses to have the same class");
    }

    @Test
    public void currentUrlsStatusCodesBelongToClassFail() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(URL_PATH, status);
        mockResponse(URL_PATH, HttpStatus.SC_NOT_FOUND);

        requestToMockedServer(URL_PATH);
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(HttpStatusClass.SUCCESS);

        assertFalse(result.getPassed(), "Expected some response statuses to belong to other classes");
        assertTrue(result.getFailed(), "Expected some response statuses to belong to other classes");
    }

    @Test
    public void currentUrlsStatusCodesEqualPass() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(URL_PATH, status);
        mockResponse(URL_PATH, status);

        requestToMockedServer(URL_PATH);
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(status);

        assertTrue(result.getPassed(), "Expected statuses of all responses to match");
        assertFalse(result.getFailed(), "Expected statuses of all responses to match");
    }

    @Test
    public void currentUrlsStatusCodesEqualFail() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(URL_PATH, status);
        mockResponse(URL_PATH, HttpStatus.SC_NOT_FOUND);

        requestToMockedServer(URL_PATH);
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(status);

        assertFalse(result.getPassed(), "Expected some responses statuses not to match");
        assertTrue(result.getFailed(), "Expected some responses statuses not to match");
    }

    protected StubMapping mockResponse(String path, Integer status) {
        return stubFor(get(urlEqualTo("/" + path)).willReturn(aResponse().withStatus(status).withBody(SUCCESSFUL_RESPONSE_BODY)));
    }

}
