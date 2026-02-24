package com.browserup.bup.proxy.mitmproxy.assertion.mostrecent.content;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.mitmproxy.BaseRestTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class MostRecentEntryAssertContentDoesNotContainRestTest extends BaseRestTest {
    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";
    private String responseNotToFind = "will not find";
    private String responseToFind = "middle body";
    private String responseBody = "begin body " + responseToFind + " end body";

    @Override
    protected String getUrlPath() { return "har/mostRecentEntry/assertContentDoesNotContain"; }

    @Test
    public void contentDoesNotContainPasses() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "contentText", responseNotToFind));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionPassed(r);
        assertFalse("Expected assertion entry result to have \"false\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void contentDoesNotContainFails() throws Exception {
        sendRequestsToTargetServer();
        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPattern, "contentText", responseToFind));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get one assertion result", r.getRequests(), Matchers.hasSize(1));
        assertAssertionFailed(r);
        assertTrue("Expected assertion entry result to have \"true\" failed flag", r.getRequests().get(0).getFailed());
        conn.disconnect();
    }

    @Test
    public void getEmptyResultIfNoEntryFoundByUrlPattern() throws Exception {
        sendRequestsToTargetServer();
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath(), toStringMap("urlPattern", urlPatternNotToMatchUrl, "contentText", responseToFind));
        AssertionResult r = new ObjectMapper().readValue(readResponseBody(conn), AssertionResult.class);
        assertAssertionNotNull(r);
        assertThat("Expected to get no assertion result entries", r.getRequests(), Matchers.hasSize(0));
        assertAssertionPassed(r);
        conn.disconnect();
    }

    @Test
    public void getBadRequestIfUrlPatternNotProvided() throws Exception {
        HttpURLConnection conn = sendGetToProxyServer(getFullUrlPath());
        assertEquals("Expected to get bad request", conn.getResponseCode(), HttpStatus.SC_BAD_REQUEST);
        conn.disconnect();
    }

    private void sendRequestsToTargetServer() throws Exception {
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_CONTENT);
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody);
        mockTargetServerResponse(urlOfOldRequest, responseBody);
        proxyManager.get().iterator().next().newHar();
        requestToTargetServer(urlOfOldRequest, responseBody);
        Thread.sleep(MILLISECONDS_BETWEEN_REQUESTS);
        requestToTargetServer(urlOfMostRecentRequest, responseBody);
    }
}
