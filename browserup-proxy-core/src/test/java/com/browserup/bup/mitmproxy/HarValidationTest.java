package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarPage;
import de.sstoehr.harreader.model.HarCookie;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HarValidationTest extends MockServerTest {
    private MitmProxyServer proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testHarEntryContainsUrlField() throws Exception {
        String stubUrl = "/testUrl.*";
        stubFor(get(urlMatching(stubUrl)).willReturn(ok()));

        proxy = new MitmProxyServer();
        proxy.start();

        proxy.newHar();

        String requestUrl = "http://localhost:" + mockServerPort + "/testUrl";

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet(requestUrl)).getEntity().getContent());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertNotNull("Expected not null log creator name", har.getLog().getCreator().getName());
        assertNotNull("Expected not null log creator version", har.getLog().getCreator().getVersion());

        for (HarEntry entry : har.getLog().getEntries()) {
            assertTrue(entry.getAdditional().get("_url") != null && StringUtils.isNotEmpty((String) entry.getAdditional().get("_url")));
        }
        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testDefaultValuesOfMockedHarResponse() throws Exception {
        String stubUrl = "/testUrl.*";
        stubFor(get(urlMatching(stubUrl)).willReturn(ok()));

        proxy = new MitmProxyServer();
        proxy.start();

        proxy.newHar();

        String requestUrl = "http://localhost:" + mockServerPort + "/testUrl";

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            NewProxyServerTestUtil.toStringAndClose(client.execute(new HttpGet(requestUrl)).getEntity().getContent());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

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
        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }
}
