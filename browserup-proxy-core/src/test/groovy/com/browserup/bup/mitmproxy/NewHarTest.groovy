package com.browserup.bup.mitmproxy

import com.browserup.bup.MitmProxyServer
import com.browserup.bup.filters.util.HarCaptureUtil
import com.browserup.bup.proxy.CaptureType
import com.browserup.bup.proxy.dns.AdvancedHostResolver
import com.browserup.bup.proxy.test.util.MockServerTest
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil
import com.browserup.harreader.model.*
import com.github.tomakehurst.wiremock.client.WireMock
import com.google.common.collect.Iterables
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.After
import org.junit.Ignore
import org.junit.Test

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * HAR tests using the new interface. When the legacy interface is retired, these tests should be combined with the tests currently in HarTest.
 */
@Ignore
class NewHarTest extends MockServerTest {
    private MitmProxyServer proxy

    @After
    void tearDown() {
        if (proxy?.started) {
            proxy.abort()
        }
    }

    @Test
    void testDnsTimingPopulatedIfNoDnsResolutionDelaySpecified() {
        def stubUrl = "/testDnsTimingPopulated"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.setDnsResolvingDelayMs(0)

        proxy.start()
        int proxyPort = proxy.getPort()

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testDnsTimingPopulated")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertNotNull("HAR should not be null", har)
        assertNotNull("HAR log should not be null", har.getLog())
        assertNotNull("HAR log entries should not be null", har.getLog().getEntries())
        assertFalse("HAR entries should exist", har.getLog().getEntries().isEmpty())

        HarEntry entry = Iterables.get(har.getLog().getEntries(), 0)
        assertThat("Expected at least 1 second DNS delay", entry.getTimings().getDns(), lessThanOrEqualTo(1000))
        assertNotNull(har.log.entries[0].time)

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testDnsTimingPopulatedIfDnsResolutionDelaySpecified() {
        def stubUrl = "/testDnsTimingPopulated"
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")))

        proxy = new MitmProxyServer()
        proxy.setDnsResolvingDelayMs(1000)

        proxy.start()

        int proxyPort = proxy.getPort()

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxyPort).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testDnsTimingPopulated")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertNotNull("HAR should not be null", har)
        assertNotNull("HAR log should not be null", har.getLog())
        assertNotNull("HAR log entries should not be null", har.getLog().getEntries())
        assertFalse("HAR entries should exist", har.getLog().getEntries().isEmpty())

        HarEntry entry = Iterables.get(har.getLog().getEntries(), 0)
        assertThat("Expected at least 1 second DNS delay", entry.getTimings().getDns(), greaterThanOrEqualTo(1000))
        assertNotNull(har.log.entries[0].time)

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testCaptureResponseCookiesInHar() {
        def stubUrl = "/testCaptureResponseCookiesInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Set-Cookie",
                "max-age-cookie=mock-value; Max-Age=3153600000",
                "expires-cookie=mock-value; Expires=Wed, 15 Mar 2022 12:00:00 GMT"))
        )

        proxy = new MitmProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_COOKIES] as Set)

        proxy.newHar()

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX", Locale.US)
        Date expiresDate = df.parse("2022-03-15 12:00:00Z")

        // expiration of the cookie won't be before this date, since the request hasn't yet been issued
        Date maxAgeCookieNotBefore = new Date(System.currentTimeMillis() + 3153600000L)

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("https://localhost:${mockServerHttpsPort}/testCaptureResponseCookiesInHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected one HAR entry", har.getLog().getEntries(), hasSize(1))
        assertThat("Expected to find two cookies in the HAR", har.getLog().getEntries().first().response.cookies, hasSize(2))

        HarCookie maxAgeCookie = har.getLog().getEntries().first().response.cookies[0]
        HarCookie expiresCookie = har.getLog().getEntries().first().response.cookies[1]

        assertEquals("Incorrect cookie name in HAR", "max-age-cookie", maxAgeCookie.name)
        assertEquals("Incorrect cookie value in HAR", "mock-value", maxAgeCookie.value)
        assertThat("Incorrect expiration date in cookie with Max-Age", maxAgeCookie.expires, greaterThan(maxAgeCookieNotBefore))

        assertEquals("Incorrect cookie name in HAR", "expires-cookie", expiresCookie.name)
        assertEquals("Incorrect cookie value in HAR", "mock-value", expiresCookie.value)

        assertEquals("Incorrect expiration date in cookie with Expires", expiresCookie.expires, expiresDate)

        //verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testCaptureResponseHeaderInHar() {
        def stubUrl = "/testCaptureResponseHeaderInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Mock-Header", "mock value"))
        )

        proxy = new MitmProxyServer()
        proxy.start()
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_HEADERS, CaptureType.RESPONSE_CONTENT] as Set)

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testCaptureResponseHeaderInHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        List<HarHeader> headers = har.getLog().getEntries().first().response.headers
        assertThat("Expected to find headers in the HAR", headers, not(empty()))

        HarHeader header = headers.find { it.name == "Mock-Header" }
        assertNotNull("Expected to find header with name Mock-Header in HAR", header)
        assertEquals("Incorrect header value for Mock-Header", "mock value", header.value)

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testCaptureDataOfEnabledCaptureType() {
        def stubUrl = "/testCaptureResponseHeaderInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Mock-Header", "mock value"))
        )

        proxy = new MitmProxyServer()
        proxy.start()
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_CONTENT] as Set)
        proxy.enableHarCaptureTypes([CaptureType.RESPONSE_HEADERS] as Set)

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testCaptureResponseHeaderInHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        List<HarHeader> headers = har.getLog().getEntries().first().response.headers
        assertThat("Expected to find headers in the HAR", headers, not(empty()))

        HarHeader header = headers.find { it.name == "Mock-Header" }
        assertNotNull("Expected to find header with name Mock-Header in HAR", header)
        assertEquals("Incorrect header value for Mock-Header", "mock value", header.value)

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testDontCaptureDisabledCaptureType() {
        def stubUrl = "/testCaptureResponseHeaderInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Mock-Header", "mock value"))
        )

        proxy = new MitmProxyServer()
        proxy.start()
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_HEADERS, CaptureType.RESPONSE_CONTENT] as Set)
        proxy.disableHarCaptureTypes(CaptureType.RESPONSE_HEADERS)

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testCaptureResponseHeaderInHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        List<HarHeader> headers = har.getLog().getEntries().first().response.headers
        assertThat("Expected to find headers in the HAR", headers, empty())

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testMultipleNewHarCallsLeadToCorrectEntries() {
        String firstResponseBody = "firstResponseBody"
        String responseContentType = "text/plain;charset=utf-8"

        def firstRequestUrl = "/testFirstRequest"
        stubFor(get(urlEqualTo(firstRequestUrl))
                .willReturn(ok()
                .withBody("firstResponseBody").withHeader("Content-Type", responseContentType))
        )

        proxy = new MitmProxyServer()
        proxy.setHarCaptureTypes(CaptureType.RESPONSE_CONTENT)
        proxy.start()
        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}$firstRequestUrl")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", firstResponseBody, responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertEquals("Expected to get exactly one entry", 1, har.log.entries.size())

        HarContent content = har.getLog().getEntries().first().response.content
        assertNotNull("Expected to find HAR content", content)
        assertEquals("Expected to find HAR content body of first response", firstResponseBody, content.text)


        proxy.newHar()

        String secondResponseBody = "secondResponseBody"

        def secondRequestUrl = "/testSecondRequest"
        stubFor(get(urlEqualTo(secondRequestUrl))
                .willReturn(ok()
                .withBody("secondResponseBody").withHeader("Content-Type", responseContentType))
        )

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}$secondRequestUrl")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", secondResponseBody, responseBody)
        }

        Thread.sleep(500)
        har = proxy.getHar()

        assertEquals("Expected to get exactly one entry after newHar was called", 1, har.log.entries.size())

        content = har.getLog().getEntries().first().response.content
        assertNotNull("Expected to find HAR content", content)
        assertEquals("Expected to find HAR content body of second response", secondResponseBody, content.text)


        verify(1, getRequestedFor(urlEqualTo(secondRequestUrl)))
    }

    @Test
    void testCaptureResponseContentInHar() {
        String expectedResponseBody = "success"
        String responseContentType = "text/plain;charset=utf-8"

        def stubUrl = "/testCaptureResponseContentInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Content-Type", responseContentType))
        )

        proxy = new MitmProxyServer()
        proxy.setHarCaptureTypes(CaptureType.RESPONSE_CONTENT)
        proxy.start()

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testCaptureResponseContentInHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", expectedResponseBody, responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        HarContent content = har.getLog().getEntries().first().response.content
        assertNotNull("Expected to find HAR content", content)

        assertEquals("Expected to capture response mimeType in HAR", responseContentType, content.mimeType)

        assertEquals("Expected to capture body content in HAR", expectedResponseBody, content.text)
        assertEquals("Unexpected response content length", expectedResponseBody.getBytes("UTF-8").length, content.size)

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testCaptureResponseInfoWhenResponseCaptureDisabled() {
        String expectedResponseBody = "success"
        String responseContentType = "text/plain;charset=utf-8"

        def stubUrl = "/testCaptureResponseContentInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Content-Type", responseContentType))
        )

        proxy = new MitmProxyServer()
        proxy.setHarCaptureTypes([] as Set)
        proxy.start()

        proxy.newHar()

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testCaptureResponseContentInHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", expectedResponseBody, responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        HarContent content = har.getLog().getEntries().first().response.content
        assertNotNull("Expected to find HAR content", content)

        assertEquals("Expected to capture response mimeType in HAR", responseContentType, content.mimeType)

        assertEquals("Expected to not capture body content in HAR", "", content.text)

        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })

        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testEndHar() {
        def stubUrl = "/testEndHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Content-Type", "text/plain;charset=utf-8"))
        )

        proxy = new MitmProxyServer()
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_CONTENT] as Set)
        proxy.start()

        newHarInitiallyEmpty: {
            Har newHar = proxy.newHar()

            assertNull("Expected newHar() to return the old (null) har", newHar)
        }

        proxy.newHar()

        // putting tests in code blocks to avoid variable name collisions
        regularHarCanCapture: {
            NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent())
                assertEquals("Did not receive expected response from mock server", "success", responseBody)
            }

            Thread.sleep(500)
            Har har = proxy.endHar()

            assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

            HarContent content = har.getLog().getEntries().first().response.content
            assertNotNull("Expected to find HAR content", content)

            assertEquals("Expected to capture body content in HAR", "success", content.text)

            assertThat("Expected HAR page timing onLoad value to be populated", har.log.pages.last().pageTimings.onLoad, greaterThan(0))
            assertNotNull(har.log.entries[0].time)
        }

        harEmptyAfterEnd: {
            Har emptyHar = proxy.getHar()

            assertNull("Expected getHar() to return null after calling endHar()", emptyHar)
        }

        harNotEmptyAfterRequest: {
            NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent())
                assertEquals("Did not receive expected response from mock server", "success", responseBody)
            }

            Thread.sleep(500)
            Har nonEmptyHar = proxy.getHar()

            assertNotNull("Expected getHar() to return non-null Har after calling endHar() and sending request", nonEmptyHar)
        }

        newHarCanCapture: {
            NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
                String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent())
                assertEquals("Did not receive expected response from mock server", "success", responseBody)
            }

            Thread.sleep(500)
            Har populatedHar = proxy.getHar()

            assertThat("Expected to find entries in the HAR", populatedHar.getLog().getEntries(), not(empty()))

            HarContent newContent = populatedHar.getLog().getEntries().first().response.content
            assertNotNull("Expected to find HAR content", newContent)

            assertEquals("Expected to capture body content in HAR", "success", newContent.text)

            assertTrue("Expected HAR entries to have _url field",
                    populatedHar.log.entries.every { StringUtils.isNotEmpty(it.url) })
        }
    }

    @Test
    void testNewPageReturnsHarInPreviousState() {
        def stubUrl = "/testEndHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success").withHeader("Content-Type", "text/plain;charset=utf-8"))
        )

        proxy = new MitmProxyServer()
        proxy.setHarCaptureTypes([CaptureType.RESPONSE_CONTENT] as Set)
        proxy.start()

        proxy.newHar("first-page")

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        HarContent content = har.getLog().getEntries().first().response.content
        assertNotNull("Expected to find HAR content", content)

        assertEquals("Expected to capture body content in HAR", "success", content.text)

        assertEquals("Expected only one HAR page to be created", 1, har.log.pages.size())
        assertEquals("Expected id of HAR page to be 'first-page'", "first-page", har.log.pages.first().id)

        Har harWithFirstPageOnly = proxy.newPage("second-page")

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet("http://localhost:${mockServerPort}/testEndHar")).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har harWithSecondPage = proxy.getHar()

        assertEquals("Expected HAR to contain first and second page page", 2, harWithSecondPage.log.pages.size())
        assertEquals("Expected id of second HAR page to be 'second-page'", "second-page", harWithSecondPage.log.pages[1].id)

        assertEquals("Expected HAR returned from newPage() not to contain second page", 1, harWithFirstPageOnly.log.pages.size())
        assertEquals("Expected id of HAR page to be 'first-page'", "first-page", harWithFirstPageOnly.log.pages.first().id)
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
    }

    @Test
    void testCaptureHttpRequestUrlInHar() {
        def stubUrl = "/httprequesturlcaptured"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success"))
        )

        proxy = new MitmProxyServer()
        proxy.start()

        proxy.newHar()

        String requestUrl = "http://localhost:${mockServerPort}/httprequesturlcaptured"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(requestUrl)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        verify(1, getRequestedFor(urlEqualTo(stubUrl)))
    }

    @Test
    void testCaptureHttpRequestUrlWithQueryParamInHar() {
        def stubUrl = "/httprequesturlcaptured.*"
        stubFor(get(urlMatching(stubUrl)).withQueryParam("param1", WireMock.equalTo("value1"))
                .willReturn(ok()
                .withBody("success"))
        )

        proxy = new MitmProxyServer()
        proxy.start()

        proxy.newHar()

        String requestUrl = "http://localhost:${mockServerPort}/httprequesturlcaptured?param1=value1"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(requestUrl)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

        assertThat("Expected to find query parameters in the HAR", har.log.entries[0].request.queryString, not(empty()))

        assertEquals("Expected first query parameter name to be param1", "param1", har.log.entries[0].request.queryString[0].name)
        assertEquals("Expected first query parameter value to be value1", "value1", har.log.entries[0].request.queryString[0].value)
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testCaptureHttpsRequestUrlInHar() {
        def stubUrl = "/httpsrequesturlcaptured.*"
        stubFor(get(urlMatching(stubUrl)).withQueryParam("param1", WireMock.equalTo("value1"))
                .willReturn(ok()
                .withBody("success"))
        )

        proxy = new MitmProxyServer()
        proxy.setTrustAllServers(true)
        proxy.start()

        proxy.newHar()

        // use HTTPS to force a CONNECT. subsequent requests through the tunnel will only contain the resource path, not the full hostname.
        String requestUrl = "https://localhost:${mockServerHttpsPort}/httpsrequesturlcaptured?param1=value1"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(it.execute(new HttpGet(requestUrl)).getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

        assertThat("Expected to find query parameters in the HAR", har.log.entries[0].request.queryString, not(empty()))

        assertEquals("Expected first query parameter name to be param1", "param1", har.log.entries[0].request.queryString[0].name)
        assertEquals("Expected first query parameter value to be value1", "value1", har.log.entries[0].request.queryString[0].value)
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testCaptureHttpsRewrittenUrlInHar() {
        def stubUrl = "/httpsrewrittenurlcaptured.*"
        stubFor(get(urlMatching(stubUrl)).withQueryParam("param1", WireMock.equalTo("value1"))
                .willReturn(ok()
                .withBody("success"))
        )

        proxy = new MitmProxyServer()
        proxy.rewriteUrl("https://localhost:${mockServerHttpsPort}/originalurl(.*)", "https://localhost:${mockServerHttpsPort}/httpsrewrittenurlcaptured\\1")
        proxy.setTrustAllServers(true)
        proxy.start()

        proxy.newHar()

        String requestUrl = "https://localhost:${mockServerHttpsPort}/originalurl?param1=value1"
        String expectedRewrittenUrl = "https://localhost:${mockServerHttpsPort}/httpsrewrittenurlcaptured?param1=value1"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 200 from mock server", 200, response.getStatusLine().getStatusCode())

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent())
            assertEquals("Did not receive expected response from mock server", "success", responseBody)
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", expectedRewrittenUrl, capturedUrl)

        assertThat("Expected to find query parameters in the HAR", har.log.entries[0].request.queryString, not(empty()))

        assertEquals("Expected first query parameter name to be param1", "param1", har.log.entries[0].request.queryString[0].name)
        assertEquals("Expected first query parameter value to be value1", "value1", har.log.entries[0].request.queryString[0].value)
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        verify(1, getRequestedFor(urlMatching(stubUrl)))
    }

    @Test
    void testHttpDnsFailureCapturedInHar() {
        AdvancedHostResolver mockFailingResolver = mock(AdvancedHostResolver)
        when(mockFailingResolver.resolve("www.doesnotexist.address")).thenReturn([])

        proxy = new MitmProxyServer()
        proxy.start()

        proxy.newHar()

        String requestUrl = "http://www.doesnotexist.address/some-resource"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 502 from proxy", 502, response.getStatusLine().getStatusCode())
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        // make sure request data is still captured despite the failure
        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

        HarResponse harResponse = har.log.entries[0].response
        assertNotNull("No HAR response found", harResponse)

        assertEquals("Error in HAR response did not match expected DNS failure error message", HarCaptureUtil.getResolutionFailedErrorMessage("www.doesnotexist.address"), harResponse.additional.get("_errorMessage"))
        assertEquals("Expected HTTP status code of 0 for failed request", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.status)
        assertEquals("Expected unknown HTTP version for failed request", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.httpVersion)
        assertEquals("Expected default value for headersSize for failed request", -1L, harResponse.headersSize)
        assertEquals("Expected default value for bodySize for failed request", -1L, harResponse.bodySize)

        HarTiming harTimings = har.log.entries[0].timings
        assertNotNull("No HAR timings found", harTimings)

        assertThat("Expected dns time to be populated after dns resolution failure", harTimings.getDns(TimeUnit.NANOSECONDS), greaterThan(0L))

        assertEquals("Expected HAR timings to contain default values after DNS failure", -1L, harTimings.getConnect(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after DNS failure", -1L, harTimings.getSsl(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after DNS failure", 0L, harTimings.getSend(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after DNS failure", 0L, harTimings.getWait(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after DNS failure", 0L, harTimings.getReceive(TimeUnit.NANOSECONDS))
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        assertNotNull(har.log.entries[0].time)
    }

    @Test
    void testHttpsDnsFailureCapturedInHar() {
        AdvancedHostResolver mockFailingResolver = mock(AdvancedHostResolver)
        when(mockFailingResolver.resolve("www.doesnotexist.address")).thenReturn([])

        proxy = new MitmProxyServer()
        proxy.setHostNameResolver(mockFailingResolver)
        proxy.start()

        proxy.newHar()

        String requestUrl = "https://www.doesnotexist.address/some-resource"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 502 from proxy", 502, response.getStatusLine().getStatusCode())
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        // make sure request data is still captured despite the failure
        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match expected HTTP CONNECT URL", requestUrl, capturedUrl)

        HarResponse harResponse = har.log.entries[0].response
        assertNotNull("No HAR response found", harResponse)

        assertEquals("Error in HAR response did not match expected DNS failure error message", HarCaptureUtil.getResolutionFailedErrorMessage("www.doesnotexist.address:443"), harResponse.additional.get("_errorMessage"))
        assertEquals("Expected HTTP status code of 0 for failed request", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.status)
        assertEquals("Expected unknown HTTP version for failed request", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.httpVersion)
        assertEquals("Expected default value for headersSize for failed request", -1L, harResponse.headersSize)
        assertEquals("Expected default value for bodySize for failed request", -1L, harResponse.bodySize)

        HarTiming harTimings = har.log.entries[0].timings
        assertNotNull("No HAR timings found", harTimings)

        assertThat("Expected dns time to be populated after dns resolution failure", harTimings.getDns(TimeUnit.NANOSECONDS), greaterThan(0L))

        assertEquals("Expected HAR timings to contain default values after DNS failure", -1L, harTimings.getConnect(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after DNS failure", -1L, harTimings.getSsl(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after DNS failure", 0L, harTimings.getSend(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after DNS failure", 0L, harTimings.getWait(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after DNS failure", 0L, harTimings.getReceive(TimeUnit.NANOSECONDS))
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        assertNotNull(har.log.entries[0].time)
    }

    @Test
    void testHttpConnectTimeoutCapturedInHar() {
        proxy = new MitmProxyServer()
        proxy.start()

        proxy.newHar()

        // TCP port 2 is reserved for "CompressNET Management Utility". since it's almost certainly not in use, connections
        // to port 2 will fail.
        String requestUrl = "http://localhost:2/some-resource"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 502 from proxy", 502, response.getStatusLine().getStatusCode())
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        // make sure request data is still captured despite the failure
        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

        assertTrue("Expected IP address to be populated", har.log.entries[0].serverIPAddress in ["127.0.0.1", "::1"] )

        HarResponse harResponse = har.log.entries[0].response
        assertNotNull("No HAR response found", harResponse)

        assertEquals("Error in HAR response did not match expected connection failure error message", HarCaptureUtil.getConnectionFailedErrorMessage(), harResponse.additional.get("_errorMessage"))
        assertEquals("Expected HTTP status code of 0 for failed request", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.status)
        assertEquals("Expected unknown HTTP version for failed request", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.httpVersion)
        assertEquals("Expected default value for headersSize for failed request", -1L, harResponse.headersSize)
        assertEquals("Expected default value for bodySize for failed request", -1L, harResponse.bodySize)

        HarTiming harTimings = har.log.entries[0].timings
        assertNotNull("No HAR timings found", harTimings)

        assertThat("Expected dns time to be populated after connection failure", harTimings.getDns(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertThat("Expected connect time to be populated after connection failure", harTimings.getConnect(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertEquals("Expected HAR timings to contain default values after connection failure", -1L, harTimings.getSsl(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after connection failure", 0L, harTimings.getSend(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after connection failure", 0L, harTimings.getWait(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after connection failure", 0L, harTimings.getReceive(TimeUnit.NANOSECONDS))
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        assertNotNull(har.log.entries[0].time)
    }

    @Test
    void testHttpsConnectTimeoutCapturedInHar() {
        proxy = new MitmProxyServer()
        proxy.start()

        proxy.newHar()

        // TCP port 2 is reserved for "CompressNET Management Utility". since it's almost certainly not in use, connections
        // to port 2 will fail.
        String requestUrl = "https://localhost:2/some-resource"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 502 from proxy", 502, response.getStatusLine().getStatusCode())
        }

        Thread.sleep(1000)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        // make sure request data is still captured despite the failure
        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", "https://localhost:2", capturedUrl)

        assertTrue("Expected IP address to be populated", har.log.entries[0].serverIPAddress in ["127.0.0.1", "::1"] )

        HarResponse harResponse = har.log.entries[0].response
        assertNotNull("No HAR response found", harResponse)

        assertEquals("Error in HAR response did not match expected connection failure error message", HarCaptureUtil.getConnectionFailedErrorMessage(), harResponse.additional.get("_errorMessage"))
        assertEquals("Expected HTTP status code of 0 for failed request", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.status)
        assertEquals("Expected unknown HTTP version for failed request", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.httpVersion)
        assertEquals("Expected default value for headersSize for failed request", -1L, harResponse.headersSize)
        assertEquals("Expected default value for bodySize for failed request", -1L, harResponse.bodySize)

        HarTiming harTimings = har.log.entries[0].timings
        assertNotNull("No HAR timings found", harTimings)

        assertThat("Expected dns time to be populated after connection failure", harTimings.getDns(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertThat("Expected connect time to be populated after connection failure", harTimings.getConnect(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertEquals("Expected HAR timings to contain default values after connection failure", -1L, harTimings.getSsl(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after connection failure", 0L, harTimings.getSend(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after connection failure", 0L, harTimings.getWait(TimeUnit.NANOSECONDS))
        assertEquals("Expected HAR timings to contain default values after connection failure", 0L, harTimings.getReceive(TimeUnit.NANOSECONDS))
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        assertTrue(har.log.entries[0].time > 0)
    }

    @Test
    void testHttpResponseTimeoutCapturedInHar() {
        def stubUrl = "/testResponseTimeoutCapturedInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withFixedDelay(TimeUnit.SECONDS.toMillis(10) as Integer)
                .withBody("success"))
        )

        proxy = new MitmProxyServer()
        proxy.setIdleConnectionTimeout(3, TimeUnit.SECONDS)
        proxy.start()

        proxy.newHar()

        String requestUrl = "http://localhost:${mockServerPort}/testResponseTimeoutCapturedInHar"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 504 from proxy", 504, response.getStatusLine().getStatusCode())
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        // make sure request data is still captured despite the failure
        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

        assertTrue("Expected IP address to be populated", har.log.entries[0].serverIPAddress in ["127.0.0.1", "::1"])

        HarResponse harResponse = har.log.entries[0].response
        assertNotNull("No HAR response found", harResponse)

        assertEquals("Error in HAR response did not match expected response timeout error message", HarCaptureUtil.getResponseTimedOutErrorMessage(), harResponse.additional.get("_errorMessage"))
        assertEquals("Expected HTTP status code of 0 for response timeout", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.status)
        assertEquals("Expected unknown HTTP version for response timeout", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.httpVersion)
        assertEquals("Expected default value for headersSize for response timeout", -1L, harResponse.headersSize)
        assertEquals("Expected default value for bodySize for response timeout", -1L, harResponse.bodySize)

        HarTiming harTimings = har.log.entries[0].timings
        assertNotNull("No HAR timings found", harTimings)

        assertEquals("Expected ssl timing to contain default value", -1L, harTimings.getSsl(TimeUnit.NANOSECONDS))

        // this timeout was caused by a failure of the server to respond, so dns, connect, send, and wait should all be populated,
        // but receive should not be populated since no response was received.
        assertThat("Expected dns time to be populated", harTimings.getDns(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertThat("Expected connect time to be populated", harTimings.getConnect(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertThat("Expected send time to be populated", harTimings.getSend(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertThat("Expected wait time to be populated", harTimings.getWait(TimeUnit.NANOSECONDS), greaterThan(0L))

        assertEquals("Expected receive time to not be populated", 0L, harTimings.getReceive(TimeUnit.NANOSECONDS))
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        assertTrue(har.log.entries[0].time > 0)
    }

    @Test
    void testHttpsResponseTimeoutCapturedInHar() {
        def stubUrl = "/testResponseTimeoutCapturedInHar"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withFixedDelay(TimeUnit.SECONDS.toMillis(10) as Integer)
                .withBody("success"))
        )

        proxy = new MitmProxyServer()
        proxy.setTrustAllServers(true)
        proxy.setIdleConnectionTimeout(1, TimeUnit.SECONDS)
        proxy.start()

        proxy.newHar()

        String requestUrl = "https://localhost:${mockServerHttpsPort}/testResponseTimeoutCapturedInHar"

        NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
            CloseableHttpResponse response = it.execute(new HttpGet(requestUrl))
            assertEquals("Did not receive HTTP 504 from proxy", 504, response.getStatusLine().getStatusCode())
        }

        Thread.sleep(500)
        Har har = proxy.getHar()

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

        // make sure request data is still captured despite the failure
        String capturedUrl = har.log.entries[0].request.url
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

        assertEquals("Expected IP address to be populated", "127.0.0.1", har.log.entries[0].serverIPAddress)

        HarResponse harResponse = har.log.entries[0].response
        assertNotNull("No HAR response found", harResponse)

        assertEquals("Error in HAR response did not match expected response timeout error message", HarCaptureUtil.RESPONSE_TIMED_OUT_ERROR_MESSAGE, harResponse.additional.get("_errorMessage"))
        assertEquals("Expected HTTP status code of 0 for response timeout", HarCaptureUtil.HTTP_STATUS_CODE_FOR_FAILURE, harResponse.status)
        assertEquals("Expected unknown HTTP version for response timeout", HarCaptureUtil.HTTP_VERSION_STRING_FOR_FAILURE, harResponse.httpVersion)
        assertEquals("Expected default value for headersSize for response timeout", -1L, harResponse.headersSize)
        assertEquals("Expected default value for bodySize for response timeout", -1L, harResponse.bodySize)

        HarTiming harTimings = har.log.entries[0].timings
        assertNotNull("No HAR timings found", harTimings)

        assertThat("Expected ssl timing to be populated", harTimings.getSsl(TimeUnit.NANOSECONDS), greaterThan(0L))

        // this timeout was caused by a failure of the server to respond, so dns, connect, send, and wait should all be populated,
        // but receive should not be populated since no response was received.
        assertThat("Expected dns time to be populated", harTimings.getDns(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertThat("Expected connect time to be populated", harTimings.getConnect(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertThat("Expected send time to be populated", harTimings.getSend(TimeUnit.NANOSECONDS), greaterThan(0L))
        assertThat("Expected wait time to be populated", harTimings.getWait(TimeUnit.NANOSECONDS), greaterThan(0L))

        assertEquals("Expected receive time to not be populated", 0L, harTimings.getReceive(TimeUnit.NANOSECONDS))
        assertTrue("Expected HAR entries to have _url field",
                har.log.entries.every { StringUtils.isNotEmpty(it.url) })
        assertTrue(har.log.entries[0].time > 0)
    }

    @Test
    void testRedirectUrlCapturedForRedirects() {
        def stubUrl = "/test300"
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(aResponse().withStatus(300)
                .withHeader("Location", "/redirected-location"))
        )

        def stubUrl2 = "/test301"
        stubFor(get(urlEqualTo(stubUrl2))
                .willReturn(aResponse().withStatus(301)
                .withHeader("Location", "/redirected-location"))
        )

        def stubUrl3 = "/test302"
        stubFor(get(urlEqualTo(stubUrl3))
                .willReturn(aResponse().withStatus(302)
                .withHeader("Location", "/redirected-location"))
        )

        def stubUrl4 = "/test303"
        stubFor(get(urlEqualTo(stubUrl4))
                .willReturn(aResponse().withStatus(303)
                .withHeader("Location", "/redirected-location"))
        )

        def stubUrl5 = "/test307"
        stubFor(get(urlEqualTo(stubUrl5))
                .willReturn(aResponse().withStatus(307)
                .withHeader("Location", "/redirected-location"))
        )

        def stubUrl6 = "/test301-no-location-header"
        stubFor(get(urlEqualTo(stubUrl6))
                .willReturn(aResponse().withStatus(301))
        )

        proxy = new MitmProxyServer()
        proxy.start()

        proxy.newHar()

        def verifyRedirect = { String requestUrl, expectedStatusCode, expectedLocationValue ->
            NewProxyServerTestUtil.getNewHttpClient(proxy.port).withCloseable {
                // for some reason, even when the HTTP client is built with .disableRedirectHandling(), it still tries to follow
                // the 301. so explicitly disable following redirects at the request level.
                def request = new HttpGet(requestUrl)
                request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build())

                CloseableHttpResponse response = it.execute(request)
                assertEquals("HTTP response code did not match expected response code", expectedStatusCode, response.getStatusLine().getStatusCode())
            }

            Thread.sleep(500)
            Har har = proxy.getHar()

            assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()))

            // make sure request data is still captured despite the failure
            String capturedUrl = har.log.entries[0].request.url
            assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl)

            HarResponse harResponse = har.log.entries[0].response
            assertNotNull("No HAR response found", harResponse)

            assertEquals("Expected redirect location to be populated in redirectURL field", expectedLocationValue, harResponse.redirectURL)
        }

        verifyRedirect("http://localhost:${mockServerPort}/test300", 300, "/redirected-location")

        // clear the HAR between every request, to make the verification step easier
        proxy.newHar()
        verifyRedirect("http://localhost:${mockServerPort}/test301", 301, "/redirected-location")

        proxy.newHar()
        verifyRedirect("http://localhost:${mockServerPort}/test302", 302, "/redirected-location")

        proxy.newHar()
        verifyRedirect("http://localhost:${mockServerPort}/test303", 303, "/redirected-location")

        proxy.newHar()
        verifyRedirect("http://localhost:${mockServerPort}/test307", 307, "/redirected-location")

        proxy.newHar()
        // redirectURL should always be populated or an empty string, never null
        verifyRedirect("http://localhost:${mockServerPort}/test301-no-location-header", 301, "")
    }

    //TODO: Add Request Capture Type tests
}
