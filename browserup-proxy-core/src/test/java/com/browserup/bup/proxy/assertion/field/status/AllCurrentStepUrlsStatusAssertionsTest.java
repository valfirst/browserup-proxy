package com.browserup.bup.proxy.assertion.field.status;

import java.io.IOException;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AllCurrentStepUrlsStatusAssertionsTest extends BaseAssertionsTest {

    @Test
    public void currentUrlsStatusCodesBelongToClassPass() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(URL_PATH, status);
        mockResponse(URL_PATH, status);

        requestToMockedServer(URL_PATH);
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(HttpStatusClass.SUCCESS);

        assertTrue("Expected statuses of all responses to have the same class", result.getPassed());
        assertFalse("Expected statuses of all responses to have the same class", result.getFailed());
    }

    @Test
    public void currentUrlsStatusCodesBelongToClassFail() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(URL_PATH, status);
        mockResponse(URL_PATH, HttpStatus.SC_NOT_FOUND);

        requestToMockedServer(URL_PATH);
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(HttpStatusClass.SUCCESS);

        assertFalse("Expected some response statuses to belong to other classes", result.getPassed());
        assertTrue("Expected some response statuses to belong to other classes", result.getFailed());
    }

    @Test
    public void currentUrlsStatusCodesEqualPass() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(URL_PATH, status);
        mockResponse(URL_PATH, status);

        requestToMockedServer(URL_PATH);
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(status);

        assertTrue("Expected statuses of all responses to match", result.getPassed());
        assertFalse("Expected statuses of all responses to match", result.getFailed());
    }

    @Test
    public void currentUrlsStatusCodesEqualFail() throws IOException {
        int status = HttpStatus.SC_OK;

        mockResponse(URL_PATH, status);
        mockResponse(URL_PATH, HttpStatus.SC_NOT_FOUND);

        requestToMockedServer(URL_PATH);
        requestToMockedServer(URL_PATH);

        AssertionResult result = proxy.assertResponseStatusCode(status);

        assertFalse("Expected some responses statuses not to match", result.getPassed());
        assertTrue("Expected some responses statuses not to match", result.getFailed());
    }

    protected StubMapping mockResponse(String path, Integer status) {
        return stubFor(get(urlEqualTo("/" + path)).willReturn(aResponse().withStatus(status).withBody(SUCCESSFUL_RESPONSE_BODY)));
    }

}
