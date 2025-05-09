package com.browserup.bup.proxy.assertion.field.content;

import java.util.regex.Pattern;

import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.assertion.BaseAssertionsTest;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import org.apache.http.HttpHeaders;
import org.junit.Before;

public class ContentBaseTest extends BaseAssertionsTest {

    protected static final String BODY_PART = "body part";
    protected static final String BODY_CONTAINING_BODY_PART = "body example with " + BODY_PART + " in the middle".toString();
    protected static final String BODY_NOT_CONTAINING_BODY_PART = "body example";
    protected static final Pattern BODY_PATTERN_TO_MATCH_BODY_PART = Pattern.compile(".*" + BODY_PART + ".*");
    protected static final Pattern BODY_PATTERN_NOT_TO_MATCH_BODY_PART = Pattern.compile(".*NOT-TO-MATCH.*");

    @Before
    public void setUp() {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_CONTENT, CaptureType.REQUEST_BINARY_CONTENT, CaptureType.REQUEST_CONTENT);
    }

    protected StubMapping mockResponse(String path, String body) {
        return WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/" + path))
                .willReturn(WireMock.ok().withHeader(HttpHeaders.CONTENT_TYPE, "text/plain").withBody(body)));
    }
}
