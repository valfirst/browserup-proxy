package com.browserup.bup.proxy.mitmproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarPage;
import de.sstoehr.harreader.model.HarCookie;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateHarRestTest extends BaseRestTest {

    @Override
    protected String getUrlPath() {
        return "har";
    }

    @Test
    void cleanHarFalseTest() throws Exception {
        String urlToCatch = "test";
        String responseBody = "";

        mockTargetServerResponse(urlToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);

        HttpURLConnection conn1 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har1 = new ObjectMapper().readValue(readResponseBody(conn1), Har.class);
        assertTrue(har1.getLog().getEntries().size() > 0, "Expected captured queries in har");
        conn1.disconnect();

        HttpURLConnection conn2 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath(),
                toStringMap("cleanHar", "false"));
        Har har2 = new ObjectMapper().readValue(readResponseBody(conn2), Har.class);
        assertTrue(har2.getLog().getEntries().size() > 0, "Expected captured queries in har");
        conn2.disconnect();

        HttpURLConnection conn3 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har3 = new ObjectMapper().readValue(readResponseBody(conn3), Har.class);
        assertTrue(har3.getLog().getEntries().size() > 0, "Expected captured queries in har");
        conn3.disconnect();

        verify(1, getRequestedFor(urlEqualTo("/" + urlToCatch)));
    }

    @Test
    void cleanHarTest() throws Exception {
        String urlToCatch = "test";
        String responseBody = "";

        mockTargetServerResponse(urlToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);

        HttpURLConnection conn1 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har1 = new ObjectMapper().readValue(readResponseBody(conn1), Har.class);
        assertTrue(har1.getLog().getEntries().size() > 0, "Expected captured queries in har");
        conn1.disconnect();

        HttpURLConnection conn2 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath(),
                toStringMap("cleanHar", "true"));
        Har har2 = new ObjectMapper().readValue(readResponseBody(conn2), Har.class);
        assertTrue(har2.getLog().getEntries().size() > 0, "Expected captured queries in old har");
        conn2.disconnect();

        HttpURLConnection conn3 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har3 = new ObjectMapper().readValue(readResponseBody(conn3), Har.class);
        assertTrue(har3.getLog().getEntries().size() == 0, "Expected to get Har without entries");
        conn3.disconnect();

        verify(1, getRequestedFor(urlEqualTo("/" + urlToCatch)));
    }

    @Test
    void validateHarForRequestWithEmptyContentAndMimeType() throws Exception {
        String urlToCatch = "test";
        String responseBody = "";

        mockTargetServerResponse(urlToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);

        HttpURLConnection conn = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har = new ObjectMapper().readValue(readResponseBody(conn), Har.class);
        assertNull(har.getLog().getBrowser(), "Expected null browser");
        assertNotNull(har.getLog().getCreator().getName(), "Expected not null log creator name");
        assertNotNull(har.getLog().getCreator().getVersion(), "Expected not null log creator version");

        for (HarPage page : har.getLog().getPages()) {
            assertNotNull(page.getId(), "Expected not null har log pages id");
            assertNotNull(page.getTitle(), "Expected not null har log pages title");
            assertNotNull(page.getStartedDateTime(), "Expected not null har log pages startedDateTime");
            assertNotNull(page.getPageTimings(), "Expected not null har log pages pageTimings");
        }

        for (HarEntry entry : har.getLog().getEntries()) {
            assertNotNull(entry.getStartedDateTime(), "Expected not null har entries startedDateTime");
            assertNotNull(entry.getTime(), "Expected not null har entries time");
            assertNotNull(entry.getRequest(), "Expected not null har entries request");
            assertNotNull(entry.getResponse(), "Expected not null har entries response");
            assertNotNull(entry.getCache(), "Expected not null har entries cache");
            assertNotNull(entry.getTimings(), "Expected not null har entries timings");

            assertNotNull(entry.getRequest().getMethod(), "Expected not null har entries requests method");
            assertNotNull(entry.getRequest().getUrl(), "Expected not null har entries requests url");
            assertNotNull(entry.getRequest().getHttpVersion(), "Expected not null har entries requests httpVersion");
            assertNotNull(entry.getRequest().getCookies(), "Expected not null har entries requests cookies");
            assertNotNull(entry.getRequest().getHeaders(), "Expected not null har entries requests headers");
            assertNotNull(entry.getRequest().getQueryString(), "Expected not null har entries requests queryString");
            assertNotNull(entry.getRequest().getHeadersSize(), "Expected not null har entries requests headersSize");
            assertNotNull(entry.getRequest().getBodySize(), "Expected not null har entries requests bodySize");

            assertNotNull(entry.getResponse().getStatus(), "Expected not null har entries responses status");
            assertNotNull(entry.getResponse().getStatusText(), "Expected not null har entries responses statusText");
            assertNotNull(entry.getResponse().getHttpVersion(), "Expected not null har entries responses httpVersion");
            assertNotNull(entry.getResponse().getCookies(), "Expected not null har entries responses cookies");
            assertNotNull(entry.getResponse().getContent(), "Expected not null har entries responses content");
            assertNotNull(entry.getResponse().getRedirectURL(), "Expected not null har entries responses redirectURL");
            assertNotNull(entry.getResponse().getHeadersSize(), "Expected not null har entries responses headersSize");
            assertNotNull(entry.getResponse().getBodySize(), "Expected not null har entries responses bodySize");

            for (HarCookie cookie : entry.getResponse().getCookies()) {
                assertNotNull(cookie.getName(), "Expected not null har entries responses cookies name");
                assertNotNull(cookie.getValue(), "Expected not null har entries responses cookies value");
            }

            assertNotNull(entry.getResponse().getContent().getSize(), "Expected not null har entries responses content size");
            assertNotNull(entry.getResponse().getContent().getMimeType(), "Expected not null har entries responses content mimeType");
            assertNotNull(entry.getResponse().getContent().getText(), "Expected not null har entries responses content text");

            assertNotNull(entry.getTimings().getSend(), "Expected not null har entries timings send");
            assertNotNull(entry.getTimings().getWait(), "Expected not null har entries timings wait");
            assertNotNull(entry.getTimings().getReceive(), "Expected not null har entries timings receive");
        }
        conn.disconnect();

        verify(1, getRequestedFor(urlEqualTo("/" + urlToCatch)));
    }

    @Override
    protected void mockTargetServerResponse(String url, String responseBody) {
        stubFor(get(urlEqualTo("/" + url)).willReturn(
                aResponse().withStatus(200)
                        .withBody(responseBody)
                        .withHeader("Content-Type", "")));
    }
}
