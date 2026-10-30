package com.browserup.bup.proxy;

import java.io.IOException;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.sstoehr.harreader.model.Har;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BlocklistTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @AfterEach
    protected void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    void testBlocklistedHttpRequestReturnsBlocklistStatusCode() throws IOException {
        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.blocklistRequests("http://www\\.blocklisted\\.domain/.*", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://www.blocklisted.domain/someresource"));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Disabled
    @Test
    void testBlocklistedHttpRequestNotRecordedToHar() throws IOException {
        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.blocklistRequests("http://www\\.blocklisted\\.domain/.*", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://www.blocklisted.domain/someresource"));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }

        Har har = proxy.getHar();

        assertFalse(har.getLog().getEntries().stream().anyMatch(it -> it.getRequest().getUrl().contains("blocklisted")), "Expected not to find blocklisted requests in har entries");
    }

    @Test
    void testBlocklistedHttpsRequestReturnsBlocklistStatusCode() throws IOException {
        // need to set up a mock server to handle the CONNECT, since that is not blocklisted
        String stubUrl = "/thisrequestshouldnotoccur";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.blocklistRequests("https://localhost:" + String.valueOf(mockServerHttpsPort) + "/.*", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + String.valueOf(mockServerHttpsPort) + "/thisrequestshouldnotoccur"));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Test
    void testCanBlocklistSingleHttpResource() throws IOException {
        String stubUrl1 = "/blocklistedresource";
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        String stubUrl2 = "/nonblocklistedresource";
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blocklisted")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.blocklistRequests("http://localhost:" + String.valueOf(mockServerPort) + "/blocklistedresource", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse nonBlocklistedResourceResponse = httpClient.execute(new HttpGet("http://localhost:" + String.valueOf(mockServerPort) + "/nonblocklistedresource"));
            assertEquals(200, nonBlocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String nonBlocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    nonBlocklistedResourceResponse.getEntity().getContent());
            assertEquals("not blocklisted", nonBlocklistedResponseBody, "Did not receive expected response from mock server");

            CloseableHttpResponse blocklistedResourceResponse = httpClient.execute(new HttpGet("http://localhost:" + String.valueOf(mockServerPort) + "/blocklistedresource"));
            assertEquals(405, blocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody,
                    is(emptyOrNullString()));
        }
    }

    @Test
    void testCanBlocklistSingleHttpsResource() throws IOException {
        String stubUrl1 = "/blocklistedresource";
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        String stubUrl2 = "/nonblocklistedresource";
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blocklisted")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.blocklistRequests("https://localhost:" + String.valueOf(mockServerHttpsPort) + "/blocklistedresource", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse nonBlocklistedResourceResponse = httpClient.execute(new HttpGet("https://localhost:" + String.valueOf(mockServerHttpsPort) + "/nonblocklistedresource"));
            assertEquals(200, nonBlocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String nonBlocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    nonBlocklistedResourceResponse.getEntity().getContent());
            assertEquals("not blocklisted", nonBlocklistedResponseBody, "Did not receive expected response from mock server");

            CloseableHttpResponse blocklistedResourceResponse = httpClient.execute(new HttpGet("https://localhost:" + String.valueOf(mockServerHttpsPort) + "/blocklistedresource"));
            assertEquals(405, blocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody,
                    is(emptyOrNullString()));
        }
    }

    @Test
    void testCanBlocklistConnectExplicitly() throws IOException {
        String stubUrl1 = "/blocklistconnect";
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        // CONNECT requests don't contain the path to the resource, only the server and port
        proxy.blocklistRequests("https://localhost:" + mockServerHttpsPort, 405, "CONNECT");

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse blocklistedResourceResponse = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/blocklistconnect"));
            assertEquals(405, blocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody,
                    is(emptyOrNullString()));
        }
    }

    @Test
    void testBlocklistDoesNotApplyToCONNECT() throws IOException {
        String stubUrl = "/connectNotBlocklisted";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        // HTTP CONNECTs should not be blocklisted unless the method is explicitly specified
        proxy.blocklistRequests("https://localhost:" + mockServerHttpsPort, 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/connectNotBlocklisted"));
            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected to receive response from mock server after successful CONNECT");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals("success", responseBody, "Expected to receive HTTP 200 and success message from server");
        }
    }
}
