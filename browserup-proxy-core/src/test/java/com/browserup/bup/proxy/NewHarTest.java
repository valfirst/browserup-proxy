package com.browserup.bup.proxy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Iterables;
import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.util.HarCaptureUtil;
import com.browserup.bup.proxy.dns.AdvancedHostResolver;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarContent;
import de.sstoehr.harreader.model.HarCookie;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarHeader;
import de.sstoehr.harreader.model.HarResponse;
import de.sstoehr.harreader.model.HarTiming;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * HAR tests using the new interface. When the legacy interface is retired, these tests should be combined with the tests currently in HarTest.
 */
@Disabled
public class NewHarTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @AfterEach
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testDnsTimingPopulated() throws Exception {
        // mock up a resolver with a DNS resolution delay
        AdvancedHostResolver mockResolver = mock(AdvancedHostResolver.class);
        when(mockResolver.resolve("localhost")).then(new Answer<java.util.Collection<InetAddress>>() {
            @Override
            public java.util.Collection<InetAddress> answer(InvocationOnMock invocationOnMock) throws Throwable {
                TimeUnit.SECONDS.sleep(1);
                return Collections.singleton(InetAddress.getByName("localhost"));
            }
        });

        String stubUrl = "/testDnsTimingPopulated";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setHostNameResolver(mockResolver);

        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.newHar();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testDnsTimingPopulated")).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertNotNull(har, "HAR should not be null");
        assertNotNull(har.getLog(), "HAR log should not be null");
        assertNotNull(har.getLog().getEntries(), "HAR log entries should not be null");
        assertFalse(har.getLog().getEntries().isEmpty(), "HAR entries should exist");

        HarEntry entry = Iterables.get(har.getLog().getEntries(), 0);
        assertThat("Expected at least 1 second DNS delay", entry.getTimings().getDns(), greaterThanOrEqualTo(1000));
        assertNotNull(har.getLog().getEntries().get(0).getTime());

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testCaptureResponseCookiesInHar() throws Exception {
        String stubUrl = "/testCaptureResponseCookiesInHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Set-Cookie",
                "max-age-cookie=mock-value; Max-Age=3153600000",
                "expires-cookie=mock-value; Expires=Wed, 15 Mar 2022 12:00:00 GMT"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setHarCaptureTypes(EnumSet.of(CaptureType.RESPONSE_COOKIES));
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.newHar();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX", Locale.US);
        Date expiresDate = df.parse("2022-03-15 12:00:00Z");

        // expiration of the cookie won't be before this date, since the request hasn't yet been issued
        Date maxAgeCookieNotBefore = new Date(System.currentTimeMillis() + 3153600000L);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/testCaptureResponseCookiesInHar")).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));
        assertThat("Expected to find two cookies in the HAR", har.getLog().getEntries().get(0).getResponse().getCookies(), hasSize(2));

        HarCookie maxAgeCookie = har.getLog().getEntries().get(0).getResponse().getCookies().get(0);
        HarCookie expiresCookie = har.getLog().getEntries().get(0).getResponse().getCookies().get(1);

        assertEquals("max-age-cookie", maxAgeCookie.getName(), "Incorrect cookie name in HAR");
        assertEquals("mock-value", maxAgeCookie.getValue(), "Incorrect cookie value in HAR");
        assertThat("Incorrect expiration date in cookie with Max-Age", maxAgeCookie.getExpires(), greaterThan(maxAgeCookieNotBefore));

        assertEquals("expires-cookie", expiresCookie.getName(), "Incorrect cookie name in HAR");
        assertEquals("mock-value", expiresCookie.getValue(), "Incorrect cookie value in HAR");

        assertThat("Incorrect expiration date in cookie with Expires", expiresCookie.getExpires(), equalTo(expiresDate));

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testCaptureResponseHeaderInHar() throws Exception {
        String stubUrl = "/testCaptureResponseHeaderInHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Mock-Header", "mock value"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setHarCaptureTypes(EnumSet.of(CaptureType.RESPONSE_HEADERS));
        proxy.start();

        proxy.newHar();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testCaptureResponseHeaderInHar")).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        List<HarHeader> headers = har.getLog().getEntries().get(0).getResponse().getHeaders();
        assertThat("Expected to find headers in the HAR", headers, not(empty()));

        HarHeader header = headers.stream().filter(h -> h.getName().equals("Mock-Header")).findFirst().orElse(null);
        assertNotNull(header, "Expected to find header with name Mock-Header in HAR");
        assertEquals("mock value", header.getValue(), "Incorrect header value for Mock-Header");

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testCaptureResponseContentInHar() throws Exception {
        String expectedResponseBody = "success";
        String responseContentType = "text/plain;charset=utf-8";

        String stubUrl = "/testCaptureResponseContentInHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Content-Type", responseContentType))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setHarCaptureTypes(CaptureType.RESPONSE_CONTENT);
        proxy.start();

        proxy.newHar();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testCaptureResponseContentInHar")).getEntity().getContent());
            assertEquals(expectedResponseBody, responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarContent content = har.getLog().getEntries().get(0).getResponse().getContent();
        assertNotNull(content, "Expected to find HAR content");

        assertEquals(responseContentType, content.getMimeType(), "Expected to capture response mimeType in HAR");

        assertEquals(expectedResponseBody, content.getText(), "Expected to capture body content in HAR");
        assertEquals((long) expectedResponseBody.getBytes("UTF-8").length, (long) content.getSize(), "Unexpected response content length");

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testCaptureResponseInfoWhenResponseCaptureDisabled() throws Exception {
        String expectedResponseBody = "success";
        String responseContentType = "text/plain;charset=utf-8";

        String stubUrl = "/testCaptureResponseContentInHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Content-Type", responseContentType))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setHarCaptureTypes(Collections.emptySet());
        proxy.start();

        proxy.newHar();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testCaptureResponseContentInHar")).getEntity().getContent());
            assertEquals(expectedResponseBody, responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarContent content = har.getLog().getEntries().get(0).getResponse().getContent();
        assertNotNull(content, "Expected to find HAR content");

        assertEquals(responseContentType, content.getMimeType(), "Expected to capture response mimeType in HAR");

        assertEquals("", content.getText(), "Expected to not capture body content in HAR");

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testEndHar() throws Exception {
        String stubUrl = "/testEndHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Content-Type", "text/plain;charset=utf-8"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setHarCaptureTypes(EnumSet.of(CaptureType.RESPONSE_CONTENT));
        proxy.start();

        // newHarInitiallyEmpty
        {
            Har newHar = proxy.newHar();

            assertNull(newHar, "Expected newHar() to return the old (null) har");
        }

        proxy.newHar();

        // regularHarCanCapture
        {
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
                assertEquals("success", responseBody, "Did not receive expected response from mock server");
            }

            Thread.sleep(500);
            Har har = proxy.endHar();

            assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

            HarContent content = har.getLog().getEntries().get(0).getResponse().getContent();
            assertNotNull(content, "Expected to find HAR content");

            assertEquals("success", content.getText(), "Expected to capture body content in HAR");

            assertThat("Expected HAR page timing onLoad value to be populated", har.getLog().getPages().get(har.getLog().getPages().size() - 1).getPageTimings().getOnLoad(), greaterThan(0));
            assertNotNull(har.getLog().getEntries().get(0).getTime());
        }

        // harEmptyAfterEnd
        {
            Har emptyHar = proxy.getHar();

            assertNull(emptyHar, "Expected getHar() to return null after calling endHar()");
        }

        // harNotEmptyAfterRequest
        {
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
                assertEquals("success", responseBody, "Did not receive expected response from mock server");
            }

            Thread.sleep(500);
            Har nonEmptyHar = proxy.getHar();

            assertNotNull(nonEmptyHar, "Expected getHar() to return non-null Har after calling endHar() and sending request");
        }

        // newHarCanCapture
        {
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
                assertEquals("success", responseBody, "Did not receive expected response from mock server");
            }

            Thread.sleep(500);
            Har populatedHar = proxy.getHar();

            assertThat("Expected to find entries in the HAR", populatedHar.getLog().getEntries(), not(empty()));

            HarContent newContent = populatedHar.getLog().getEntries().get(0).getResponse().getContent();
            assertNotNull(newContent, "Expected to find HAR content");

            assertEquals("success", newContent.getText(), "Expected to capture body content in HAR");
        }
    }

    @Test
    public void testNewPageReturnsHarInPreviousState() throws Exception {
        String stubUrl = "/testEndHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Content-Type", "text/plain;charset=utf-8"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setHarCaptureTypes(EnumSet.of(CaptureType.RESPONSE_CONTENT));
        proxy.start();

        proxy.newHar("first-page");

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarContent content = har.getLog().getEntries().get(0).getResponse().getContent();
        assertNotNull(content, "Expected to find HAR content");

        assertEquals("success", content.getText(), "Expected to capture body content in HAR");

        assertEquals(1, har.getLog().getPages().size(), "Expected only one HAR page to be created");
        assertEquals("first-page", har.getLog().getPages().get(0).getId(), "Expected id of HAR page to be 'first-page'");

        Har harWithFirstPageOnly = proxy.newPage("second-page");

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har harWithSecondPage = proxy.getHar();

        assertEquals(2, harWithSecondPage.getLog().getPages().size(), "Expected HAR to contain first and second page page");
        assertEquals("second-page", harWithSecondPage.getLog().getPages().get(1).getId(), "Expected id of second HAR page to be 'second-page'");

        assertEquals(1, harWithFirstPageOnly.getLog().getPages().size(), "Expected HAR returned from newPage() not to contain second page");
        assertEquals("first-page", harWithFirstPageOnly.getLog().getPages().get(0).getId(), "Expected id of HAR page to be 'first-page'");
    }

    @Test
    public void testCaptureHttpRequestUrlInHar() throws Exception {
        String stubUrl = "/httprequesturlcaptured";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.newHar();

        String requestUrl = "http://localhost:" + mockServerPort + "/httprequesturlcaptured";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(requestUrl)).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(requestUrl, capturedUrl, "URL captured in HAR did not match request URL");

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }

    @Test
    public void testCaptureHttpRequestUrlWithQueryParamInHar() throws Exception {
        String stubUrl = "/httprequesturlcaptured.*";
        stubFor(get(urlMatching(stubUrl)).withQueryParam("param1", WireMock.equalTo("value1"))
                .willReturn(ok()
                .withBody("success"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.newHar();

        String requestUrl = "http://localhost:" + mockServerPort + "/httprequesturlcaptured?param1=value1";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(requestUrl)).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(requestUrl, capturedUrl, "URL captured in HAR did not match request URL");

        assertThat("Expected to find query parameters in the HAR", har.getLog().getEntries().get(0).getRequest().getQueryString(), not(empty()));

        assertEquals("param1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getName(), "Expected first query parameter name to be param1");
        assertEquals("value1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getValue(), "Expected first query parameter value to be value1");

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testCaptureHttpsRequestUrlInHar() throws Exception {
        String stubUrl = "/httpsrequesturlcaptured.*";
        stubFor(get(urlMatching(stubUrl)).withQueryParam("param1", WireMock.equalTo("value1"))
                .willReturn(ok()
                .withBody("success"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.newHar();

        // use HTTPS to force a CONNECT. subsequent requests through the tunnel will only contain the resource path, not the full hostname.
        String requestUrl = "https://localhost:" + mockServerHttpsPort + "/httpsrequesturlcaptured?param1=value1";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(requestUrl)).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(requestUrl, capturedUrl, "URL captured in HAR did not match request URL");

        assertThat("Expected to find query parameters in the HAR", har.getLog().getEntries().get(0).getRequest().getQueryString(), not(empty()));

        assertEquals("param1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getName(), "Expected first query parameter name to be param1");
        assertEquals("value1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getValue(), "Expected first query parameter value to be value1");

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testCaptureHttpsRewrittenUrlInHar() throws Exception {
        String stubUrl = "/httpsrewrittenurlcaptured.*";
        stubFor(get(urlMatching(stubUrl)).withQueryParam("param1", WireMock.equalTo("value1"))
                .willReturn(ok()
                .withBody("success"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.rewriteUrl("https://localhost:" + mockServerHttpsPort + "/originalurl(.*)", "https://localhost:" + mockServerHttpsPort + "/httpsrewrittenurlcaptured$1");
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.newHar();

        String requestUrl = "https://localhost:" + mockServerHttpsPort + "/originalurl?param1=value1";
        String expectedRewrittenUrl = "https://localhost:" + mockServerHttpsPort + "/httpsrewrittenurlcaptured?param1=value1";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(200, response.getStatusLine().getStatusCode(), "Did not receive HTTP 200 from mock server");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(expectedRewrittenUrl, capturedUrl, "URL captured in HAR did not match request URL");

        assertThat("Expected to find query parameters in the HAR", har.getLog().getEntries().get(0).getRequest().getQueryString(), not(empty()));

        assertEquals("param1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getName(), "Expected first query parameter name to be param1");
        assertEquals("value1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getValue(), "Expected first query parameter value to be value1");

        verify(1, getRequestedFor(urlMatching(stubUrl)));
    }

    @Test
    public void testMitmDisabledStopsHTTPCapture() throws Exception {
        String stubUrl1 = "/httpmitmdisabled";
        stubFor(get(urlEqualTo(stubUrl1))
                .willReturn(ok()
                .withBody("Response over HTTP"))
        );

        String stubUrl2 = "/httpsmitmdisabled";
        stubFor(get(urlEqualTo(stubUrl2))
                .willReturn(ok()
                .withBody("Response over HTTPS"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setMitmDisabled(true);
        proxy.start();

        proxy.newHar();

        // httpsRequest
        {
            String httpsUrl = "https://localhost:" + mockServerHttpsPort + "/httpsmitmdisabled";
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(httpsUrl)).getEntity().getContent());
                assertEquals("Response over HTTPS", responseBody, "Did not receive expected response from mock server");
            }

            Thread.sleep(500);
            Har har = proxy.getHar();

            assertThat("Expected to find no entries in the HAR because MITM is disabled", har.getLog().getEntries(), empty());
        }

        // httpRequest
        {
            String httpUrl = "http://localhost:" + mockServerPort + "/httpmitmdisabled";
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(httpUrl)).getEntity().getContent());
                assertEquals("Response over HTTP", responseBody, "Did not receive expected response from mock server");
            }

            Thread.sleep(500);
            Har har = proxy.getHar();

            assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

            String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
            assertEquals(httpUrl, capturedUrl, "URL captured in HAR did not match request URL");
        }

        // secondHttpsRequest
        {
            String httpsUrl = "https://localhost:" + mockServerHttpsPort + "/httpsmitmdisabled";
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(httpsUrl)).getEntity().getContent());
                assertEquals("Response over HTTPS", responseBody, "Did not receive expected response from mock server");
            }

            Thread.sleep(500);
            Har har = proxy.getHar();

            assertThat("Expected to find only the HTTP entry in the HAR", har.getLog().getEntries(), hasSize(1));
        }

    }

    @Test
    public void testHttpDnsFailureCapturedInHar() throws Exception {
        AdvancedHostResolver mockFailingResolver = mock(AdvancedHostResolver.class);
        when(mockFailingResolver.resolve("www.doesnotexist.address")).thenReturn(Collections.emptyList());

        proxy = new BrowserUpProxyServer();
        proxy.setHostNameResolver(mockFailingResolver);
        proxy.start();

        proxy.newHar();

        String requestUrl = "http://www.doesnotexist.address/some-resource";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(502, response.getStatusLine().getStatusCode(), "Did not receive HTTP 502 from proxy");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(requestUrl, capturedUrl, "URL captured in HAR did not match request URL");

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");

        assertEquals(HarCaptureUtil.getResolutionFailedErrorMessage("www.doesnotexist.address"), harResponse.getAdditional().get("_errorMessage"), "Error in HAR response did not match expected DNS failure error message");
        assertEquals(HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus(), "Expected HTTP status code of 0 for failed request");
        assertEquals(HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion(), "Expected unknown HTTP version for failed request");
        assertEquals(Long.valueOf(-1), harResponse.getHeadersSize(), "Expected default value for headersSize for failed request");
        assertEquals(Long.valueOf(-1), harResponse.getBodySize(), "Expected default value for bodySize for failed request");

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull(harTimings, "No HAR timings found");

        assertThat("Expected dns time to be populated after dns resolution failure", harTimings.getDns(), greaterThan(0));

        assertEquals(Integer.valueOf(-1), harTimings.getConnect(), "Expected HAR timings to contain default values after DNS failure");
        assertEquals(Integer.valueOf(-1), harTimings.getSsl(), "Expected HAR timings to contain default values after DNS failure");
        assertEquals(Integer.valueOf(0), harTimings.getSend(), "Expected HAR timings to contain default values after DNS failure");
        assertEquals(Integer.valueOf(0), harTimings.getWait(), "Expected HAR timings to contain default values after DNS failure");
        assertEquals(Integer.valueOf(0), harTimings.getReceive(), "Expected HAR timings to contain default values after DNS failure");
        assertNotNull(har.getLog().getEntries().get(0).getTime());
    }

    @Test
    public void testHttpsDnsFailureCapturedInHar() throws Exception {
        AdvancedHostResolver mockFailingResolver = mock(AdvancedHostResolver.class);
        when(mockFailingResolver.resolve("www.doesnotexist.address")).thenReturn(Collections.emptyList());

        proxy = new BrowserUpProxyServer();
        proxy.setHostNameResolver(mockFailingResolver);
        proxy.start();

        proxy.newHar();

        String requestUrl = "https://www.doesnotexist.address/some-resource";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(502, response.getStatusLine().getStatusCode(), "Did not receive HTTP 502 from proxy");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("https://www.doesnotexist.address", capturedUrl, "URL captured in HAR did not match expected HTTP CONNECT URL");

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");

        assertEquals(HarCaptureUtil.getResolutionFailedErrorMessage("www.doesnotexist.address:443"), harResponse.getAdditional().get("_errorMessage"), "Error in HAR response did not match expected DNS failure error message");
        assertEquals(HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus(), "Expected HTTP status code of 0 for failed request");
        assertEquals(HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion(), "Expected unknown HTTP version for failed request");
        assertEquals(Long.valueOf(-1), harResponse.getHeadersSize(), "Expected default value for headersSize for failed request");
        assertEquals(Long.valueOf(-1), harResponse.getBodySize(), "Expected default value for bodySize for failed request");

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull(harTimings, "No HAR timings found");

        assertThat("Expected dns time to be populated after dns resolution failure", harTimings.getDns(), greaterThan(0));

        assertEquals(Integer.valueOf(-1), harTimings.getConnect(), "Expected HAR timings to contain default values after DNS failure");
        assertEquals(Integer.valueOf(-1), harTimings.getSsl(), "Expected HAR timings to contain default values after DNS failure");
        assertEquals(Integer.valueOf(0), harTimings.getSend(), "Expected HAR timings to contain default values after DNS failure");
        assertEquals(Integer.valueOf(0), harTimings.getWait(), "Expected HAR timings to contain default values after DNS failure");
        assertEquals(Integer.valueOf(0), harTimings.getReceive(), "Expected HAR timings to contain default values after DNS failure");
        assertNotNull(har.getLog().getEntries().get(0).getTime());
    }

    @Test
    public void testHttpConnectTimeoutCapturedInHar() throws Exception {
        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.newHar();

        // TCP port 2 is reserved for "CompressNET Management Utility". since it's almost certainly not in use, connections
        // to port 2 will fail.
        String requestUrl = "http://localhost:2/some-resource";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(502, response.getStatusLine().getStatusCode(), "Did not receive HTTP 502 from proxy");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(requestUrl, capturedUrl, "URL captured in HAR did not match request URL");

        assertEquals("127.0.0.1", har.getLog().getEntries().get(0).getServerIPAddress(), "Expected IP address to be populated");

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");

        assertEquals(HarCaptureUtil.getConnectionFailedErrorMessage(), harResponse.getAdditional().get("_errorMessage"), "Error in HAR response did not match expected connection failure error message");
        assertEquals(HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus(), "Expected HTTP status code of 0 for failed request");
        assertEquals(HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion(), "Expected unknown HTTP version for failed request");
        assertEquals(Long.valueOf(-1), harResponse.getHeadersSize(), "Expected default value for headersSize for failed request");
        assertEquals(Long.valueOf(-1), harResponse.getBodySize(), "Expected default value for bodySize for failed request");

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull(harTimings, "No HAR timings found");

        assertThat("Expected dns time to be populated after connection failure", harTimings.getDns(), greaterThan(0));
        assertThat("Expected connect time to be populated after connection failure", harTimings.getConnect(), greaterThan(0));
        assertEquals(Integer.valueOf(-1), harTimings.getSsl(), "Expected HAR timings to contain default values after connection failure");
        assertEquals(Integer.valueOf(0), harTimings.getSend(), "Expected HAR timings to contain default values after connection failure");
        assertEquals(Integer.valueOf(0), harTimings.getWait(), "Expected HAR timings to contain default values after connection failure");
        assertEquals(Integer.valueOf(0), harTimings.getReceive(), "Expected HAR timings to contain default values after connection failure");
        assertNotNull(har.getLog().getEntries().get(0).getTime());
    }

    @Test
    public void testHttpsConnectTimeoutCapturedInHar() throws Exception {
        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.newHar();

        // TCP port 2 is reserved for "CompressNET Management Utility". since it's almost certainly not in use, connections
        // to port 2 will fail.
        String requestUrl = "https://localhost:2/some-resource";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(502, response.getStatusLine().getStatusCode(), "Did not receive HTTP 502 from proxy");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("https://localhost:2", capturedUrl, "URL captured in HAR did not match request URL");

        assertEquals("127.0.0.1", har.getLog().getEntries().get(0).getServerIPAddress(), "Expected IP address to be populated");

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");

        assertEquals(HarCaptureUtil.getConnectionFailedErrorMessage(), harResponse.getAdditional().get("_errorMessage"), "Error in HAR response did not match expected connection failure error message");
        assertEquals(HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus(), "Expected HTTP status code of 0 for failed request");
        assertEquals(HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion(), "Expected unknown HTTP version for failed request");
        assertEquals(Long.valueOf(-1), harResponse.getHeadersSize(), "Expected default value for headersSize for failed request");
        assertEquals(Long.valueOf(-1), harResponse.getBodySize(), "Expected default value for bodySize for failed request");

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull(harTimings, "No HAR timings found");

        assertThat("Expected dns time to be populated after connection failure", harTimings.getDns(), greaterThan(0));
        assertThat("Expected connect time to be populated after connection failure", harTimings.getConnect(), greaterThan(0));
        assertEquals(Integer.valueOf(-1), harTimings.getSsl(), "Expected HAR timings to contain default values after connection failure");
        assertEquals(Integer.valueOf(0), harTimings.getSend(), "Expected HAR timings to contain default values after connection failure");
        assertEquals(Integer.valueOf(0), harTimings.getWait(), "Expected HAR timings to contain default values after connection failure");
        assertEquals(Integer.valueOf(0), harTimings.getReceive(), "Expected HAR timings to contain default values after connection failure");
        assertTrue(har.getLog().getEntries().get(0).getTime() > 0);
    }

    @Test
    public void testHttpResponseTimeoutCapturedInHar() throws Exception {
        String stubUrl = "/testResponseTimeoutCapturedInHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withFixedDelay((int) TimeUnit.SECONDS.toMillis(10))
                .withBody("success"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setIdleConnectionTimeout(3, TimeUnit.SECONDS);
        proxy.start();

        proxy.newHar();

        String requestUrl = "http://localhost:" + mockServerPort + "/testResponseTimeoutCapturedInHar";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(504, response.getStatusLine().getStatusCode(), "Did not receive HTTP 504 from proxy");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(requestUrl, capturedUrl, "URL captured in HAR did not match request URL");

        assertEquals("127.0.0.1", har.getLog().getEntries().get(0).getServerIPAddress(), "Expected IP address to be populated");

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");

        assertEquals(HarCaptureUtil.getResponseTimedOutErrorMessage(), harResponse.getAdditional().get("_errorMessage"), "Error in HAR response did not match expected response timeout error message");
        assertEquals(HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus(), "Expected HTTP status code of 0 for response timeout");
        assertEquals(HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion(), "Expected unknown HTTP version for response timeout");
        assertEquals(Long.valueOf(-1), harResponse.getHeadersSize(), "Expected default value for headersSize for response timeout");
        assertEquals(Long.valueOf(-1), harResponse.getBodySize(), "Expected default value for bodySize for response timeout");

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull(harTimings, "No HAR timings found");

        assertEquals(Integer.valueOf(-1), harTimings.getSsl(), "Expected ssl timing to contain default value");

        // this timeout was caused by a failure of the server to respond, so dns, connect, send, and wait should all be populated,
        // but receive should not be populated since no response was received.
        assertThat("Expected dns time to be populated", harTimings.getDns(), greaterThan(0));
        assertThat("Expected connect time to be populated", harTimings.getConnect(), greaterThan(0));
        assertThat("Expected send time to be populated", harTimings.getSend(), greaterThan(0));
        assertThat("Expected wait time to be populated", harTimings.getWait(), greaterThan(0));

        assertEquals(Integer.valueOf(0), harTimings.getReceive(), "Expected receive time to not be populated");
        assertTrue(har.getLog().getEntries().get(0).getTime() > 0);
    }

    @Test
    public void testHttpsResponseTimeoutCapturedInHar() throws Exception {
        String stubUrl = "/testResponseTimeoutCapturedInHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withFixedDelay((int) TimeUnit.SECONDS.toMillis(10))
                .withBody("success"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.setIdleConnectionTimeout(3, TimeUnit.SECONDS);
        proxy.start();

        proxy.newHar();

        String requestUrl = "https://localhost:" + mockServerHttpsPort + "/testResponseTimeoutCapturedInHar";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(504, response.getStatusLine().getStatusCode(), "Did not receive HTTP 504 from proxy");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(requestUrl, capturedUrl, "URL captured in HAR did not match request URL");

        assertEquals("127.0.0.1", har.getLog().getEntries().get(0).getServerIPAddress(), "Expected IP address to be populated");

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");

        assertEquals(HarCaptureUtil.getResponseTimedOutErrorMessage(), harResponse.getAdditional().get("_errorMessage"), "Error in HAR response did not match expected response timeout error message");
        assertEquals(HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus(), "Expected HTTP status code of 0 for response timeout");
        assertEquals(HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion(), "Expected unknown HTTP version for response timeout");
        assertEquals(Long.valueOf(-1), harResponse.getHeadersSize(), "Expected default value for headersSize for response timeout");
        assertEquals(Long.valueOf(-1), harResponse.getBodySize(), "Expected default value for bodySize for response timeout");

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull(harTimings, "No HAR timings found");

        assertThat("Expected ssl timing to be populated", harTimings.getSsl(), greaterThan(0));

        // this timeout was caused by a failure of the server to respond, so dns, connect, send, and wait should all be populated,
        // but receive should not be populated since no response was received.
        assertThat("Expected dns time to be populated", harTimings.getDns(), greaterThan(0));
        assertThat("Expected connect time to be populated", harTimings.getConnect(), greaterThan(0));
        assertThat("Expected send time to be populated", harTimings.getSend(), greaterThan(0));
        assertThat("Expected wait time to be populated", harTimings.getWait(), greaterThan(0));

        assertEquals(Integer.valueOf(0), harTimings.getReceive(), "Expected receive time to not be populated");
        assertTrue(har.getLog().getEntries().get(0).getTime() > 0);
    }

    @Test
    public void testHttpsBlacklistedUrlInHar() throws Exception {
        String stubUrl = "/httpsblacklistedurl";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok())
        );
        proxy = new BrowserUpProxyServer();
        proxy.blocklistRequests(".*", 405);
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.newHar();

        String requestUrl = "https://localhost:" + mockServerHttpsPort + "/httpsblacklistedurl";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blacklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blacklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");
        assertEquals(405, harResponse.getStatus(), "Expected blacklisted status code for the request");
        assertEquals(Long.valueOf(-1), harResponse.getBodySize(), "Expected default value for bodySize for response timeout");
    }

    @Test
    public void testHttpsWhitelistedUrlInHar() throws Exception {
        String stubUrl = "/httpsblacklistedurl";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok())
        );
        proxy = new BrowserUpProxyServer();
        proxy.allowlistRequests(null, 405);
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.newHar();

        String requestUrl = "https://localhost:" + mockServerHttpsPort + "/httpsblacklistedurl";

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blacklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blacklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");
        assertEquals(405, harResponse.getStatus(), "Expected blacklisted status code for the request");
        assertEquals(Long.valueOf(-1), harResponse.getBodySize(), "Expected default value for bodySize for response timeout");
    }

    @Test
    public void testRedirectUrlCapturedForRedirects() throws Exception {
        String stubUrl = "/test300";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(aResponse().withStatus(300)
                .withHeader("Location", "/redirected-location"))
        );

        String stubUrl2 = "/test301";
        stubFor(get(urlEqualTo(stubUrl2))
                .willReturn(aResponse().withStatus(301)
                .withHeader("Location", "/redirected-location"))
        );

        String stubUrl3 = "/test302";
        stubFor(get(urlEqualTo(stubUrl3))
                .willReturn(aResponse().withStatus(302)
                .withHeader("Location", "/redirected-location"))
        );

        String stubUrl4 = "/test303";
        stubFor(get(urlEqualTo(stubUrl4))
                .willReturn(aResponse().withStatus(303)
                .withHeader("Location", "/redirected-location"))
        );

        String stubUrl5 = "/test307";
        stubFor(get(urlEqualTo(stubUrl5))
                .willReturn(aResponse().withStatus(307)
                .withHeader("Location", "/redirected-location"))
        );

        String stubUrl6 = "/test301-no-location-header";
        stubFor(get(urlEqualTo(stubUrl6))
                .willReturn(aResponse().withStatus(301))
        );

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.newHar();

        verifyRedirect("http://localhost:" + mockServerPort + "/test300", 300, "/redirected-location");

        // clear the HAR between every request, to make the verification step easier
        proxy.newHar();
        verifyRedirect("http://localhost:" + mockServerPort + "/test301", 301, "/redirected-location");

        proxy.newHar();
        verifyRedirect("http://localhost:" + mockServerPort + "/test302", 302, "/redirected-location");

        proxy.newHar();
        verifyRedirect("http://localhost:" + mockServerPort + "/test303", 303, "/redirected-location");

        proxy.newHar();
        verifyRedirect("http://localhost:" + mockServerPort + "/test307", 307, "/redirected-location");

        proxy.newHar();
        // redirectURL should always be populated or an empty string, never null
        verifyRedirect("http://localhost:" + mockServerPort + "/test301-no-location-header", 301, "");
    }

    private void verifyRedirect(String requestUrl, int expectedStatusCode, String expectedLocationValue) throws Exception {
        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            // for some reason, even when the HTTP client is built with .disableRedirectHandling(), it still tries to follow
            // the 301. so explicitly disable following redirects at the request level.
            HttpGet request = new HttpGet(requestUrl);
            request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());

            CloseableHttpResponse response = httpClient.execute(request);
            assertEquals(expectedStatusCode, response.getStatusLine().getStatusCode(), "HTTP response code did not match expected response code");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals(requestUrl, capturedUrl, "URL captured in HAR did not match request URL");

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull(harResponse, "No HAR response found");

        assertEquals(expectedLocationValue, harResponse.getRedirectURL(), "Expected redirect location to be populated in redirectURL field");
    }

    //TODO: Add Request Capture Type tests
}
