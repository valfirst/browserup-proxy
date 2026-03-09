package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.BlocklistEntry;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import de.sstoehr.harreader.model.Har;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BlocklistTest extends MockServerTest {
    private MitmProxyServer proxy;

    @AfterEach
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testBlocklistedHttpRequestReturnsBlocklistStatusCodeUsingSetBlockList() throws Exception {
        proxy = new MitmProxyServer();
        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.setBlocklist(Arrays.asList(new BlocklistEntry("http://www\\.blocklisted\\.domain/.*", 405)));

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            CloseableHttpResponse response = client.execute(new HttpGet("http://www.blocklisted.domain/someresource"));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testBlocklistedHttpRequestNotRecordedToHar() throws Exception {
        proxy = new MitmProxyServer();
        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.blocklistRequests("http://www\\.blocklisted\\.domain/.*", 405);

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            CloseableHttpResponse response = client.execute(new HttpGet("http://www.blocklisted.domain/someresource"));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }

        Har har = proxy.getHar();

        assertFalse(har.getLog().getEntries().stream().anyMatch(e -> e.getRequest().getUrl().contains("blocklisted")), "Expected not to find blocklisted requests in har entries");
    }

    @Test
    public void testBlocklistedHttpRequestReturnsBlocklistStatusCode() throws Exception {
        proxy = new MitmProxyServer();
        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.blocklistRequests("http://www\\.blocklisted\\.domain/.*", 405);

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            CloseableHttpResponse response = client.execute(new HttpGet("http://www.blocklisted.domain/someresource"));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testBlocklistedHttpsRequestReturnsBlocklistStatusCode() throws Exception {
        // need to set up a mock server to handle the CONNECT, since that is not blocklisted
        String stubUrl = "/thisrequestshouldnotoccur";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        proxy = new MitmProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.blocklistRequests("https://localhost:" + mockServerHttpsPort + "/.*", 405);

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            CloseableHttpResponse response = client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/thisrequestshouldnotoccur"));
            assertEquals(405, response.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", responseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testCanBlocklistSingleHttpResource() throws Exception {
        String stubUrl1 = "/blocklistedresource";
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        String stubUrl2 = "/nonblocklistedresource";
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blocklisted")));

        proxy = new MitmProxyServer();
        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.blocklistRequests("http://localhost:" + mockServerPort + "/blocklistedresource", 405);

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            CloseableHttpResponse nonBlocklistedResourceResponse = client.execute(new HttpGet("http://localhost:" + mockServerPort + "/nonblocklistedresource"));
            assertEquals(200, nonBlocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String nonBlocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonBlocklistedResourceResponse.getEntity().getContent());
            assertEquals(nonBlocklistedResponseBody, "Did not receive expected response from mock server", "not blocklisted");

            CloseableHttpResponse blocklistedResourceResponse = client.execute(new HttpGet("http://localhost:" + mockServerPort + "/blocklistedresource"));
            assertEquals(405, blocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testCanBlocklistSingleHttpsResource() throws Exception {
        String stubUrl1 = "/blocklistedresource";
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        String stubUrl2 = "/nonblocklistedresource";
        stubFor(get(urlEqualTo(stubUrl2)).willReturn(ok().withBody("not blocklisted")));

        proxy = new MitmProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();
        int proxyPort = proxy.getPort();

        proxy.blocklistRequests("https://localhost:" + mockServerHttpsPort + "/blocklistedresource", 405);

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            CloseableHttpResponse nonBlocklistedResourceResponse = client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/nonblocklistedresource"));
            assertEquals(200, nonBlocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String nonBlocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(nonBlocklistedResourceResponse.getEntity().getContent());
            assertEquals(nonBlocklistedResponseBody, "Did not receive expected response from mock server", "not blocklisted");

            CloseableHttpResponse blocklistedResourceResponse = client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/blocklistedresource"));
            assertEquals(405, blocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testCanBlocklistConnectExplicitly() throws Exception {
        String stubUrl1 = "/blocklistconnect";
        stubFor(get(urlEqualTo(stubUrl1)).willReturn(aResponse().withStatus(500).withBody("this URL should never be called")));

        proxy = new MitmProxyServer();
        proxy.start();
        int proxyPort = proxy.getPort();

        // CONNECT requests don't contain the path to the resource, only the server and port
        proxy.blocklistRequests("https://localhost:" + mockServerHttpsPort, 405, "CONNECT");

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            CloseableHttpResponse blocklistedResourceResponse = client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/blocklistconnect"));
            assertEquals(405, blocklistedResourceResponse.getStatusLine().getStatusCode(), "Did not receive blocklisted status code in response");

            String blocklistedResponseBody = NewProxyServerTestUtil.toStringAndClose(blocklistedResourceResponse.getEntity().getContent());
            assertThat("Expected blocklisted response to contain 0-length body", blocklistedResponseBody, is(emptyOrNullString()));
        }
    }

    @Test
    public void testBlocklistDoesNotApplyToCONNECT() throws Exception {
        String stubUrl = "/connectNotBlocklisted";
        stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withBody("success")));

        proxy = new MitmProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();
        int proxyPort = proxy.getPort();

        // HTTP CONNECTs should not be blocklisted unless the method is explicitly specified
        proxy.blocklistRequests("https://localhost:" + mockServerHttpsPort, 405);

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxyPort)) {
            CloseableHttpResponse response = client.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/connectNotBlocklisted"));
            assertEquals(200, response.getStatusLine().getStatusCode(), "Expected to receive response from mock server after successful CONNECT");

            String responseBody = NewProxyServerTestUtil.toStringAndClose(response.getEntity().getContent());
            assertEquals(responseBody, "Expected to receive HTTP 200 and success message from server", "success");
        }
    }
}
