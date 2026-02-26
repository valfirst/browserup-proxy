package com.browserup.bup.proxy.mitmproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarPage;
import de.sstoehr.harreader.model.HarCookie;
import org.junit.Test;

import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

public class ValidateHarRestTest extends BaseRestTest {

    @Override
    protected String getUrlPath() {
        return "har";
    }

    @Test
    public void cleanHarFalseTest() throws Exception {
        String urlToCatch = "test";
        String responseBody = "";

        mockTargetServerResponse(urlToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);

        HttpURLConnection conn1 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har1 = new ObjectMapper().readValue(readResponseBody(conn1), Har.class);
        assertTrue("Expected captured queries in har", har1.getLog().getEntries().size() > 0);
        conn1.disconnect();

        HttpURLConnection conn2 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath(),
                toStringMap("cleanHar", "false"));
        Har har2 = new ObjectMapper().readValue(readResponseBody(conn2), Har.class);
        assertTrue("Expected captured queries in har", har2.getLog().getEntries().size() > 0);
        conn2.disconnect();

        HttpURLConnection conn3 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har3 = new ObjectMapper().readValue(readResponseBody(conn3), Har.class);
        assertTrue("Expected captured queries in har", har3.getLog().getEntries().size() > 0);
        conn3.disconnect();

        verify(1, getRequestedFor(urlEqualTo("/" + urlToCatch)));
    }

    @Test
    public void cleanHarTest() throws Exception {
        String urlToCatch = "test";
        String responseBody = "";

        mockTargetServerResponse(urlToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);

        HttpURLConnection conn1 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har1 = new ObjectMapper().readValue(readResponseBody(conn1), Har.class);
        assertTrue("Expected captured queries in har", har1.getLog().getEntries().size() > 0);
        conn1.disconnect();

        HttpURLConnection conn2 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath(),
                toStringMap("cleanHar", "true"));
        Har har2 = new ObjectMapper().readValue(readResponseBody(conn2), Har.class);
        assertTrue("Expected captured queries in old har", har2.getLog().getEntries().size() > 0);
        conn2.disconnect();

        HttpURLConnection conn3 = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har3 = new ObjectMapper().readValue(readResponseBody(conn3), Har.class);
        assertTrue("Expected to get Har without entries", har3.getLog().getEntries().size() == 0);
        conn3.disconnect();

        verify(1, getRequestedFor(urlEqualTo("/" + urlToCatch)));
    }

    @Test
    public void validateHarForRequestWithEmptyContentAndMimeType() throws Exception {
        String urlToCatch = "test";
        String responseBody = "";

        mockTargetServerResponse(urlToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);

        HttpURLConnection conn = sendGetToProxyServer("/proxy/" + proxy.getPort() + "/" + getUrlPath());
        Har har = new ObjectMapper().readValue(readResponseBody(conn), Har.class);
        assertNull("Expected null browser", har.getLog().getBrowser());
        assertNotNull("Expected not null log creator name", har.getLog().getCreator().getName());
        assertNotNull("Expected not null log creator version", har.getLog().getCreator().getVersion());

        for (HarPage page : har.getLog().getPages()) {
            assertNotNull("Expected not null har log pages id", page.getId());
            assertNotNull("Expected not null har log pages title", page.getTitle());
            assertNotNull("Expected not null har log pages startedDateTime", page.getStartedDateTime());
            assertNotNull("Expected not null har log pages pageTimings", page.getPageTimings());
        }

        for (HarEntry entry : har.getLog().getEntries()) {
            assertNotNull("Expected not null har entries startedDateTime", entry.getStartedDateTime());
            assertNotNull("Expected not null har entries time", entry.getTime());
            assertNotNull("Expected not null har entries request", entry.getRequest());
            assertNotNull("Expected not null har entries response", entry.getResponse());
            assertNotNull("Expected not null har entries cache", entry.getCache());
            assertNotNull("Expected not null har entries timings", entry.getTimings());

            assertNotNull("Expected not null har entries requests method", entry.getRequest().getMethod());
            assertNotNull("Expected not null har entries requests url", entry.getRequest().getUrl());
            assertNotNull("Expected not null har entries requests httpVersion", entry.getRequest().getHttpVersion());
            assertNotNull("Expected not null har entries requests cookies", entry.getRequest().getCookies());
            assertNotNull("Expected not null har entries requests headers", entry.getRequest().getHeaders());
            assertNotNull("Expected not null har entries requests queryString", entry.getRequest().getQueryString());
            assertNotNull("Expected not null har entries requests headersSize", entry.getRequest().getHeadersSize());
            assertNotNull("Expected not null har entries requests bodySize", entry.getRequest().getBodySize());

            assertNotNull("Expected not null har entries responses status", entry.getResponse().getStatus());
            assertNotNull("Expected not null har entries responses statusText", entry.getResponse().getStatusText());
            assertNotNull("Expected not null har entries responses httpVersion", entry.getResponse().getHttpVersion());
            assertNotNull("Expected not null har entries responses cookies", entry.getResponse().getCookies());
            assertNotNull("Expected not null har entries responses content", entry.getResponse().getContent());
            assertNotNull("Expected not null har entries responses redirectURL", entry.getResponse().getRedirectURL());
            assertNotNull("Expected not null har entries responses headersSize", entry.getResponse().getHeadersSize());
            assertNotNull("Expected not null har entries responses bodySize", entry.getResponse().getBodySize());

            for (HarCookie cookie : entry.getResponse().getCookies()) {
                assertNotNull("Expected not null har entries responses cookies name", cookie.getName());
                assertNotNull("Expected not null har entries responses cookies value", cookie.getValue());
            }

            assertNotNull("Expected not null har entries responses content size", entry.getResponse().getContent().getSize());
            assertNotNull("Expected not null har entries responses content mimeType", entry.getResponse().getContent().getMimeType());
            assertNotNull("Expected not null har entries responses content text", entry.getResponse().getContent().getText());

            assertNotNull("Expected not null har entries timings send", entry.getTimings().getSend());
            assertNotNull("Expected not null har entries timings wait", entry.getTimings().getWait());
            assertNotNull("Expected not null har entries timings receive", entry.getTimings().getReceive());
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
