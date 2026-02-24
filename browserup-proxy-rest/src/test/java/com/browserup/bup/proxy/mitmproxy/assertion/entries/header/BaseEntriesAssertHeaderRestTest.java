package com.browserup.bup.proxy.mitmproxy.assertion.entries.header;

import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public abstract class BaseEntriesAssertHeaderRestTest extends BaseRestTest {
    protected static final String COMMON_URL_PART = "url";
    protected static final String URL_OF_FIRST_REQUEST = COMMON_URL_PART + "-first";
    protected static final String URL_OF_SECOND_REQUEST = COMMON_URL_PART + "-second";
    protected static final String URL_PATTERN_TO_MATCH_BOTH = ".*" + COMMON_URL_PART + "-.*";
    protected static final String URL_PATTERN_TO_MATCH_FIRST = ".*" + URL_OF_FIRST_REQUEST + ".*";
    protected static final String URL_PATTERN_TO_MATCH_NOTHING = ".*does_not_match-.*";
    protected static final String COMMON_RESPONSE_BODY = "success";
    protected static final String COMMON_HEADER_VALUE = "header-value";
    protected static final String COMMON_HEADER_NAME = "header-name";
    protected static final String FIRST_HEADER_NAME = "first-" + COMMON_HEADER_NAME;
    protected static final String SECOND_HEADER_NAME = "some-" + COMMON_HEADER_NAME;
    protected static final String FIRST_HEADER_VALUE = "first-value-" + COMMON_HEADER_VALUE;
    protected static final String SECOND_HEADER_VALUE = "second-value-" + COMMON_HEADER_VALUE;
    protected static final String MISSING_HEADER_VALUE = "missing value";

    protected static final HttpHeader FIRST_HEADER = new HttpHeader(FIRST_HEADER_NAME, FIRST_HEADER_VALUE);
    protected static final HttpHeader SECOND_HEADER = new HttpHeader(SECOND_HEADER_NAME, SECOND_HEADER_VALUE);

    protected void mockTargetServerResponse(String url, String responseBody, HttpHeader[] headers) {
        HttpHeader[] allHeaders = new HttpHeader[headers.length + 1];
        System.arraycopy(headers, 0, allHeaders, 0, headers.length);
        allHeaders[headers.length] = new HttpHeader("Content-Type", "text/plain");
        stubFor(get(urlEqualTo("/" + url)).willReturn(
                ok().withBody(responseBody)
                        .withHeaders(new HttpHeaders(allHeaders))));
    }

    protected void sendRequestsToTargetServer(HttpHeader firstResponseHeader, HttpHeader secondResponseHeader) throws Exception {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_HEADERS);

        mockTargetServerResponse(URL_OF_FIRST_REQUEST, COMMON_RESPONSE_BODY, new HttpHeader[]{firstResponseHeader});
        mockTargetServerResponse(URL_OF_SECOND_REQUEST, COMMON_RESPONSE_BODY, new HttpHeader[]{secondResponseHeader});

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(URL_OF_FIRST_REQUEST, COMMON_RESPONSE_BODY);
        requestToTargetServer(URL_OF_SECOND_REQUEST, COMMON_RESPONSE_BODY);
    }
}
