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
import org.junit.After;
import org.junit.Test;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * HAR tests using the new interface. When the legacy interface is retired, these tests should be combined with the tests currently in HarTest.
 */
public class NewHarTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @After
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
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertNotNull("HAR should not be null", har);
        assertNotNull("HAR log should not be null", har.getLog());
        assertNotNull("HAR log entries should not be null", har.getLog().getEntries());
        assertFalse("HAR entries should exist", har.getLog().getEntries().isEmpty());

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
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));
        assertThat("Expected to find two cookies in the HAR", har.getLog().getEntries().get(0).getResponse().getCookies(), hasSize(2));

        HarCookie maxAgeCookie = har.getLog().getEntries().get(0).getResponse().getCookies().get(0);
        HarCookie expiresCookie = har.getLog().getEntries().get(0).getResponse().getCookies().get(1);

        assertEquals("Incorrect cookie name in HAR", "max-age-cookie", maxAgeCookie.getName());
        assertEquals("Incorrect cookie value in HAR", "mock-value", maxAgeCookie.getValue());
        assertThat("Incorrect expiration date in cookie with Max-Age", maxAgeCookie.getExpires(), greaterThan(maxAgeCookieNotBefore));

        assertEquals("Incorrect cookie name in HAR", "expires-cookie", expiresCookie.getName());
        assertEquals("Incorrect cookie value in HAR", "mock-value", expiresCookie.getValue());

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
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        List<HarHeader> headers = har.getLog().getEntries().get(0).getResponse().getHeaders();
        assertThat("Expected to find headers in the HAR", headers, not(empty()));

        HarHeader header = headers.stream().filter(h -> h.getName().equals("Mock-Header")).findFirst().orElse(null);
        assertNotNull("Expected to find header with name Mock-Header in HAR", header);
        assertEquals("Incorrect header value for Mock-Header", "mock value", header.getValue());

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
            assertEquals("Did not receive expected response from mock server", expectedResponseBody, responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarContent content = har.getLog().getEntries().get(0).getResponse().getContent();
        assertNotNull("Expected to find HAR content", content);

        assertEquals("Expected to capture response mimeType in HAR", responseContentType, content.getMimeType());

        assertEquals("Expected to capture body content in HAR", expectedResponseBody, content.getText());
        assertEquals("Unexpected response content length", (long) expectedResponseBody.getBytes("UTF-8").length, (long) content.getSize());

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
            assertEquals("Did not receive expected response from mock server", expectedResponseBody, responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarContent content = har.getLog().getEntries().get(0).getResponse().getContent();
        assertNotNull("Expected to find HAR content", content);

        assertEquals("Expected to capture response mimeType in HAR", responseContentType, content.getMimeType());

        assertEquals("Expected to not capture body content in HAR", "", content.getText());

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

            assertNull("Expected newHar() to return the old (null) har", newHar);
        }

        proxy.newHar();

        // regularHarCanCapture
        {
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "success", responseBody);
            }

            Thread.sleep(500);
            Har har = proxy.endHar();

            assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

            HarContent content = har.getLog().getEntries().get(0).getResponse().getContent();
            assertNotNull("Expected to find HAR content", content);

            assertEquals("Expected to capture body content in HAR", "success", content.getText());

            assertThat("Expected HAR page timing onLoad value to be populated", har.getLog().getPages().get(har.getLog().getPages().size() - 1).getPageTimings().getOnLoad(), greaterThan(0));
            assertNotNull(har.getLog().getEntries().get(0).getTime());
        }

        // harEmptyAfterEnd
        {
            Har emptyHar = proxy.getHar();

            assertNull("Expected getHar() to return null after calling endHar()", emptyHar);
        }

        // harNotEmptyAfterRequest
        {
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "success", responseBody);
            }

            Thread.sleep(500);
            Har nonEmptyHar = proxy.getHar();

            assertNotNull("Expected getHar() to return non-null Har after calling endHar() and sending request", nonEmptyHar);
        }

        // newHarCanCapture
        {
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "success", responseBody);
            }

            Thread.sleep(500);
            Har populatedHar = proxy.getHar();

            assertThat("Expected to find entries in the HAR", populatedHar.getLog().getEntries(), not(empty()));

            HarContent newContent = populatedHar.getLog().getEntries().get(0).getResponse().getContent();
            assertNotNull("Expected to find HAR content", newContent);

            assertEquals("Expected to capture body content in HAR", "success", newContent.getText());
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
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarContent content = har.getLog().getEntries().get(0).getResponse().getContent();
        assertNotNull("Expected to find HAR content", content);

        assertEquals("Expected to capture body content in HAR", "success", content.getText());

        assertEquals("Expected only one HAR page to be created", 1, har.getLog().getPages().size());
        assertEquals("Expected id of HAR page to be 'first-page'", "first-page", har.getLog().getPages().get(0).getId());

        Har harWithFirstPageOnly = proxy.newPage("second-page");

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("http://localhost:" + mockServerPort + "/testEndHar")).getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har harWithSecondPage = proxy.getHar();

        assertEquals("Expected HAR to contain first and second page page", 2, harWithSecondPage.getLog().getPages().size());
        assertEquals("Expected id of second HAR page to be 'second-page'", "second-page", harWithSecondPage.getLog().getPages().get(1).getId());

        assertEquals("Expected HAR returned from newPage() not to contain second page", 1, harWithFirstPageOnly.getLog().getPages().size());
        assertEquals("Expected id of HAR page to be 'first-page'", "first-page", harWithFirstPageOnly.getLog().getPages().get(0).getId());
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
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);

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
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);

        assertThat("Expected to find query parameters in the HAR", har.getLog().getEntries().get(0).getRequest().getQueryString(), not(empty()));

        assertEquals("Expected first query parameter name to be param1", "param1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getName());
        assertEquals("Expected first query parameter value to be value1", "value1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getValue());

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
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);

        assertThat("Expected to find query parameters in the HAR", har.getLog().getEntries().get(0).getRequest().getQueryString(), not(empty()));

        assertEquals("Expected first query parameter name to be param1", "param1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getName());
        assertEquals("Expected first query parameter value to be value1", "value1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getValue());

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
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "success", responseBody);
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", expectedRewrittenUrl, capturedUrl);

        assertThat("Expected to find query parameters in the HAR", har.getLog().getEntries().get(0).getRequest().getQueryString(), not(empty()));

        assertEquals("Expected first query parameter name to be param1", "param1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getName());
        assertEquals("Expected first query parameter value to be value1", "value1", har.getLog().getEntries().get(0).getRequest().getQueryString().get(0).getValue());

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
                assertEquals("Did not receive expected response from mock server", "Response over HTTPS", responseBody);
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
                assertEquals("Did not receive expected response from mock server", "Response over HTTP", responseBody);
            }

            Thread.sleep(500);
            Har har = proxy.getHar();

            assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

            String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
            assertEquals("URL captured in HAR did not match request URL", httpUrl, capturedUrl);
        }

        // secondHttpsRequest
        {
            String httpsUrl = "https://localhost:" + mockServerHttpsPort + "/httpsmitmdisabled";
            try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet(httpsUrl)).getEntity().getContent());
                assertEquals("Did not receive expected response from mock server", "Response over HTTPS", responseBody);
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
            assertEquals("Did not receive HTTP 502 from proxy", 502, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);

        assertEquals("Error in HAR response did not match expected DNS failure error message", HarCaptureUtil.getResolutionFailedErrorMessage("www.doesnotexist.address"), harResponse.getAdditional().get("_errorMessage"));
        assertEquals("Expected HTTP status code of 0 for failed request", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus());
        assertEquals("Expected unknown HTTP version for failed request", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion());
        assertEquals("Expected default value for headersSize for failed request", Long.valueOf(-1), harResponse.getHeadersSize());
        assertEquals("Expected default value for bodySize for failed request", Long.valueOf(-1), harResponse.getBodySize());

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull("No HAR timings found", harTimings);

        assertThat("Expected dns time to be populated after dns resolution failure", harTimings.getDns(), greaterThan(0));

        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(-1), harTimings.getConnect());
        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(-1), harTimings.getSsl());
        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(0), harTimings.getSend());
        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(0), harTimings.getWait());
        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(0), harTimings.getReceive());
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
            assertEquals("Did not receive HTTP 502 from proxy", 502, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match expected HTTP CONNECT URL", "https://www.doesnotexist.address", capturedUrl);

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);

        assertEquals("Error in HAR response did not match expected DNS failure error message", HarCaptureUtil.getResolutionFailedErrorMessage("www.doesnotexist.address:443"), harResponse.getAdditional().get("_errorMessage"));
        assertEquals("Expected HTTP status code of 0 for failed request", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus());
        assertEquals("Expected unknown HTTP version for failed request", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion());
        assertEquals("Expected default value for headersSize for failed request", Long.valueOf(-1), harResponse.getHeadersSize());
        assertEquals("Expected default value for bodySize for failed request", Long.valueOf(-1), harResponse.getBodySize());

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull("No HAR timings found", harTimings);

        assertThat("Expected dns time to be populated after dns resolution failure", harTimings.getDns(), greaterThan(0));

        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(-1), harTimings.getConnect());
        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(-1), harTimings.getSsl());
        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(0), harTimings.getSend());
        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(0), harTimings.getWait());
        assertEquals("Expected HAR timings to contain default values after DNS failure", Integer.valueOf(0), harTimings.getReceive());
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
            assertEquals("Did not receive HTTP 502 from proxy", 502, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);

        assertEquals("Expected IP address to be populated", "127.0.0.1", har.getLog().getEntries().get(0).getServerIPAddress());

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);

        assertEquals("Error in HAR response did not match expected connection failure error message", HarCaptureUtil.getConnectionFailedErrorMessage(), harResponse.getAdditional().get("_errorMessage"));
        assertEquals("Expected HTTP status code of 0 for failed request", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus());
        assertEquals("Expected unknown HTTP version for failed request", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion());
        assertEquals("Expected default value for headersSize for failed request", Long.valueOf(-1), harResponse.getHeadersSize());
        assertEquals("Expected default value for bodySize for failed request", Long.valueOf(-1), harResponse.getBodySize());

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull("No HAR timings found", harTimings);

        assertThat("Expected dns time to be populated after connection failure", harTimings.getDns(), greaterThan(0));
        assertThat("Expected connect time to be populated after connection failure", harTimings.getConnect(), greaterThan(0));
        assertEquals("Expected HAR timings to contain default values after connection failure", Integer.valueOf(-1), harTimings.getSsl());
        assertEquals("Expected HAR timings to contain default values after connection failure", Integer.valueOf(0), harTimings.getSend());
        assertEquals("Expected HAR timings to contain default values after connection failure", Integer.valueOf(0), harTimings.getWait());
        assertEquals("Expected HAR timings to contain default values after connection failure", Integer.valueOf(0), harTimings.getReceive());
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
            assertEquals("Did not receive HTTP 502 from proxy", 502, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", "https://localhost:2", capturedUrl);

        assertEquals("Expected IP address to be populated", "127.0.0.1", har.getLog().getEntries().get(0).getServerIPAddress());

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);

        assertEquals("Error in HAR response did not match expected connection failure error message", HarCaptureUtil.getConnectionFailedErrorMessage(), harResponse.getAdditional().get("_errorMessage"));
        assertEquals("Expected HTTP status code of 0 for failed request", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus());
        assertEquals("Expected unknown HTTP version for failed request", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion());
        assertEquals("Expected default value for headersSize for failed request", Long.valueOf(-1), harResponse.getHeadersSize());
        assertEquals("Expected default value for bodySize for failed request", Long.valueOf(-1), harResponse.getBodySize());

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull("No HAR timings found", harTimings);

        assertThat("Expected dns time to be populated after connection failure", harTimings.getDns(), greaterThan(0));
        assertThat("Expected connect time to be populated after connection failure", harTimings.getConnect(), greaterThan(0));
        assertEquals("Expected HAR timings to contain default values after connection failure", Integer.valueOf(-1), harTimings.getSsl());
        assertEquals("Expected HAR timings to contain default values after connection failure", Integer.valueOf(0), harTimings.getSend());
        assertEquals("Expected HAR timings to contain default values after connection failure", Integer.valueOf(0), harTimings.getWait());
        assertEquals("Expected HAR timings to contain default values after connection failure", Integer.valueOf(0), harTimings.getReceive());
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
            assertEquals("Did not receive HTTP 504 from proxy", 504, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);

        assertEquals("Expected IP address to be populated", "127.0.0.1", har.getLog().getEntries().get(0).getServerIPAddress());

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);

        assertEquals("Error in HAR response did not match expected response timeout error message", HarCaptureUtil.getResponseTimedOutErrorMessage(), harResponse.getAdditional().get("_errorMessage"));
        assertEquals("Expected HTTP status code of 0 for response timeout", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus());
        assertEquals("Expected unknown HTTP version for response timeout", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion());
        assertEquals("Expected default value for headersSize for response timeout", Long.valueOf(-1), harResponse.getHeadersSize());
        assertEquals("Expected default value for bodySize for response timeout", Long.valueOf(-1), harResponse.getBodySize());

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull("No HAR timings found", harTimings);

        assertEquals("Expected ssl timing to contain default value", Integer.valueOf(-1), harTimings.getSsl());

        // this timeout was caused by a failure of the server to respond, so dns, connect, send, and wait should all be populated,
        // but receive should not be populated since no response was received.
        assertThat("Expected dns time to be populated", harTimings.getDns(), greaterThan(0));
        assertThat("Expected connect time to be populated", harTimings.getConnect(), greaterThan(0));
        assertThat("Expected send time to be populated", harTimings.getSend(), greaterThan(0));
        assertThat("Expected wait time to be populated", harTimings.getWait(), greaterThan(0));

        assertEquals("Expected receive time to not be populated", Integer.valueOf(0), harTimings.getReceive());
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
            assertEquals("Did not receive HTTP 504 from proxy", 504, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);

        assertEquals("Expected IP address to be populated", "127.0.0.1", har.getLog().getEntries().get(0).getServerIPAddress());

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);

        assertEquals("Error in HAR response did not match expected response timeout error message", HarCaptureUtil.getResponseTimedOutErrorMessage(), harResponse.getAdditional().get("_errorMessage"));
        assertEquals("Expected HTTP status code of 0 for response timeout", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.getStatus());
        assertEquals("Expected unknown HTTP version for response timeout", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.getHttpVersion());
        assertEquals("Expected default value for headersSize for response timeout", Long.valueOf(-1), harResponse.getHeadersSize());
        assertEquals("Expected default value for bodySize for response timeout", Long.valueOf(-1), harResponse.getBodySize());

        HarTiming harTimings = har.getLog().getEntries().get(0).getTimings();
        assertNotNull("No HAR timings found", harTimings);

        assertThat("Expected ssl timing to be populated", harTimings.getSsl(), greaterThan(0));

        // this timeout was caused by a failure of the server to respond, so dns, connect, send, and wait should all be populated,
        // but receive should not be populated since no response was received.
        assertThat("Expected dns time to be populated", harTimings.getDns(), greaterThan(0));
        assertThat("Expected connect time to be populated", harTimings.getConnect(), greaterThan(0));
        assertThat("Expected send time to be populated", harTimings.getSend(), greaterThan(0));
        assertThat("Expected wait time to be populated", harTimings.getWait(), greaterThan(0));

        assertEquals("Expected receive time to not be populated", Integer.valueOf(0), harTimings.getReceive());
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
            assertEquals("Did not receive blacklisted status code in response", 405, response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blacklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);
        assertEquals("Expected blacklisted status code for the request", 405, harResponse.getStatus());
        assertEquals("Expected default value for bodySize for response timeout", Long.valueOf(-1), harResponse.getBodySize());
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
            assertEquals("Did not receive blacklisted status code in response", 405, response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blacklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);
        assertEquals("Expected blacklisted status code for the request", 405, harResponse.getStatus());
        assertEquals("Expected default value for bodySize for response timeout", Long.valueOf(-1), harResponse.getBodySize());
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
            assertEquals("HTTP response code did not match expected response code", expectedStatusCode, response.getStatusLine().getStatusCode());
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        // make sure request data is still captured despite the failure
        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);

        HarResponse harResponse = har.getLog().getEntries().get(0).getResponse();
        assertNotNull("No HAR response found", harResponse);

        assertEquals("Expected redirect location to be populated in redirectURL field", expectedLocationValue, harResponse.getRedirectURL());
    }

    //TODO: Add Request Capture Type tests
}
