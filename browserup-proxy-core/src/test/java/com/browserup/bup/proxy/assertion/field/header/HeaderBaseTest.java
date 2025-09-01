package com.browserup.bup.proxy.assertion.field.header;

import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import org.junit.Before;

public class HeaderBaseTest extends BaseAssertionsTest {

    protected StubMapping mockResponse(String path, HttpHeader header) {
        return WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/" + path))
                .willReturn(WireMock.ok().withBody(SUCCESSFUL_RESPONSE_BODY).withHeaders(new HttpHeaders(header))));
//        mockServer.when(request()
//                .withMethod("GET")
//                .withPath("/${path}"),
//                Times.once())
//                .respond(response()
//                .withStatusCode(HttpStatus.SC_OK)
//                .withBody(SUCCESSFUL_RESPONSE_BODY)
//                .withHeader(header))
    }

    protected static final String HEADER_NAME = "headerName";

    protected static final String NOT_MATCHING_HEADER_NAME = "headerName not to match";
    protected static final String HEADER_VALUE = "headerValue";
    protected static final String NOT_MATCHING_HEADER_VALUE = "headerValue not to match";
    protected static final HttpHeader HEADER = new HttpHeader(HEADER_NAME, HEADER_VALUE);

    @Before
    public void setUp() {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_HEADERS);
        mockResponse(URL_PATH, HEADER);
    }
}
