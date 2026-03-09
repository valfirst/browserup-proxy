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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HarValidationTest extends MockServerTest {
    private MitmProxyServer proxy;

    @AfterEach
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

        assertNotNull(har.getLog().getCreator().getName(), "Expected not null log creator name");
        assertNotNull(har.getLog().getCreator().getVersion(), "Expected not null log creator version");

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
        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }
}
