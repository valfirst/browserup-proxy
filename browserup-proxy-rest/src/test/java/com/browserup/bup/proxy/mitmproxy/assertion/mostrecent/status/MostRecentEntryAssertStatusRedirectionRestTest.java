package com.browserup.bup.proxy.mitmproxy.assertion.mostrecent.status;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class MostRecentEntryAssertStatusRedirectionRestTest extends BaseRestTest {
    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";
    private int redirectionStatus = HttpStatus.SC_TEMPORARY_REDIRECT;
    private int nonRedirectionStatus = HttpStatus.SC_BAD_REQUEST;
    private String responseBody = "success";

    @Override
    protected String getUrlPath() { return "har/mostRecentEntry/assertStatusRedirection"; }

    @Test
    public void getBadRequestUrlPatternIsInvalid() throws Exception {
        proxyManager.get().iterator().next().newHar();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", "["));
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpStatus.SC_BAD_REQUEST);
        conn.disconnect();
    }

    @Test
    public void statusRedirectionPasses() throws Exception {
        sendRequestsToTargetServer(nonRedirectionStatus, redirectionStatus);
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionPassed(r);
        assertFalse("Expected assertion entry result to have \"false\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void statusRedirectionFails() throws Exception {
        sendRequestsToTargetServer(redirectionStatus, nonRedirectionStatus);
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionFailed(r);
        assertTrue("Expected assertion entry result to have \"true\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void getEmptyResultIfNoEntryFoundByUrlPattern() throws Exception {
        sendRequestsToTargetServer(redirectionStatus, nonRedirectionStatus);
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPatternNotToMatchUrl));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get no assertion result entries", r.getRequests(), Matchers.hasSize(0));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    private void sendRequestsToTargetServer(int oldStatus, int recentStatus) throws Exception {
        mockTargetServerResponse(urlOfMostRecentRequest, recentStatus);
        mockTargetServerResponse(urlOfOldRequest, oldStatus);
        proxyManager.get().iterator().next().newHar();
        requestToTargetServer(urlOfOldRequest);
        Thread.sleep(MILLISECONDS_BETWEEN_REQUESTS);
        requestToTargetServer(urlOfMostRecentRequest);
    }

    protected void requestToTargetServer(String url) throws java.io.IOException {
        java.net.URL targetUrl = new java.net.URL("http://localhost:" + mockServerPort + "/" + url);
        java.net.Proxy proxyAddr = new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("localhost", proxy.getPort()));
        HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection(proxyAddr);
        conn.setRequestMethod("GET");
        try { conn.getResponseCode(); } catch (java.io.IOException ignored) { }
        conn.disconnect();
    }


    protected void mockTargetServerResponse(String url, int status) {
        String redirectUrl = "http://test.com";
        stubFor(get(urlEqualTo("/" + url)).willReturn(
                aResponse().withStatus(status).withHeader("Location", redirectUrl).withHeader("Content-Type", "text/plain")));
        stubFor(get(urlMatching("/" + redirectUrl)).willReturn(ok().withBody("success")));
    }

}
