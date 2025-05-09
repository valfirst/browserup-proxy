package com.browserup.bup.proxy;

import java.io.IOException;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import de.sstoehr.harreader.model.Har;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BlocklistTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testBlocklistedHttpRequestReturnsBlocklistStatusCode() throws IOException {
        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.blocklistRequests("http://www\\.blocklisted\\.domain/.*", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://www.blocklisted.domain/someresource"));
            assertEquals("Did not receive blocklisted status code in response", 405,
                    response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Ignore
    @Test
    public void testBlocklistedHttpRequestNotRecordedToHar() throws IOException {
        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.blocklistRequests("http://www\\.blocklisted\\.domain/.*", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("http://www.blocklisted.domain/someresource"));
            assertEquals("Did not receive blocklisted status code in response", 405,
                    response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }

        Har har = proxy.getHar();

        assertFalse("Expected not to find blocklisted requests in har entries",
                har.getLog().getEntries().stream().anyMatch(it -> it.getRequest().getUrl().contains("blocklisted")));
    }

    @Test
    public void testBlocklistedHttpsRequestReturnsBlocklistStatusCode() throws IOException {
        // need to set up a mock server to handle the CONNECT, since that is not blocklisted
        String stubUrl = "/thisrequestshouldnotoccur";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.blocklistRequests("https://localhost:" + String.valueOf(mockServerHttpsPort) + "/.*", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + String.valueOf(mockServerHttpsPort) + "/thisrequestshouldnotoccur"));
            assertEquals("Did not receive blocklisted status code in response", 405,
                    response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testCanBlocklistSingleHttpResource() throws IOException {
        String stubUrl1 = "/blocklistedresource";
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        String stubUrl2 = "/nonblocklistedresource";
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blocklisted")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        proxy.blocklistRequests("http://localhost:" + String.valueOf(mockServerPort) + "/blocklistedresource", 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse nonBlocklistedResourceResponse = httpClient.execute(new HttpGet("http://localhost:" + String.valueOf(mockServerPort) + "/nonblocklistedresource"));
            assertEquals("Did not receive blocklisted status code in response", 200,
                    nonBlocklistedResourceResponse.getStatusLine().getStatusCode());

            String nonBlocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    nonBlocklistedResourceResponse.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "not blocklisted",
                    nonBlocklistedResponseBody);

            CloseableHttpResponse blocklistedResourceResponse = httpClient.execute(new HttpGet("http://localhost:" + String.valueOf(mockServerPort) + "/blocklistedresource"));
            assertEquals("Did not receive blocklisted status code in response", 405,
                    blocklistedResourceResponse.getStatusLine().getStatusCode());

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody,
                    is(emptyOrNullString()));
        }
    }

    @Test
    public void testCanBlocklistSingleHttpsResource() throws IOException {
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
            assertEquals("Did not receive blocklisted status code in response", 200,
                    nonBlocklistedResourceResponse.getStatusLine().getStatusCode());

            String nonBlocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    nonBlocklistedResourceResponse.getEntity().getContent());
            assertEquals("Did not receive expected response from mock server", "not blocklisted",
                    nonBlocklistedResponseBody);

            CloseableHttpResponse blocklistedResourceResponse = httpClient.execute(new HttpGet("https://localhost:" + String.valueOf(mockServerHttpsPort) + "/blocklistedresource"));
            assertEquals("Did not receive blocklisted status code in response", 405,
                    blocklistedResourceResponse.getStatusLine().getStatusCode());

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody,
                    is(emptyOrNullString()));
        }
    }

    @Test
    public void testCanBlocklistConnectExplicitly() throws IOException {
        String stubUrl1 = "/blocklistconnect";
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        proxy = new BrowserUpProxyServer();
        proxy.start();

        // CONNECT requests don't contain the path to the resource, only the server and port
        proxy.blocklistRequests("https://localhost:" + mockServerHttpsPort, 405, "CONNECT");

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse blocklistedResourceResponse = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/blocklistconnect"));
            assertEquals("Did not receive blocklisted status code in response", 405,
                    blocklistedResourceResponse.getStatusLine().getStatusCode());

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(
                    blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody,
                    is(emptyOrNullString()));
        }
    }

    @Test
    public void testBlocklistDoesNotApplyToCONNECT() throws IOException {
        String stubUrl = "/connectNotBlocklisted";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        // HTTP CONNECTs should not be blocklisted unless the method is explicitly specified
        proxy.blocklistRequests("https://localhost:" + mockServerHttpsPort, 405);

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            CloseableHttpResponse response = httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/connectNotBlocklisted"));
            assertEquals("Expected to receive response from mock server after successful CONNECT", 200,
                    response.getStatusLine().getStatusCode());

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals("Expected to receive HTTP 200 and success message from server", "success",
                    responseBody);
        }
    }
}
