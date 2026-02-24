package com.browserup.bup.proxy.mitmproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.sstoehr.harreader.model.HarEntry;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Comparator;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class FindMostRecentEntryRestTest extends BaseRestTest {
    private static final int MILLISECONDS_BETWEEN_REQUESTS = 100;

    private String urlOfMostRecentRequest = "url-most-recent";
    private String urlOfOldRequest = "url-old";
    private String urlPatternToMatchUrl = ".*url-.*";
    private String urlPatternNotToMatchUrl = ".*does_not_match-.*";
    private String responseBody = "success";

    @Override
    protected String getUrlPath() {
        return "har/mostRecentEntry";
    }

    @Test
    public void findMostRecentHarEntryByUrlPattern() throws Exception {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody);
        mockTargetServerResponse(urlOfOldRequest, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlOfOldRequest, responseBody);

        Thread.sleep(MILLISECONDS_BETWEEN_REQUESTS);

        requestToTargetServer(urlOfMostRecentRequest, responseBody);

        HttpURLConnection conn1 = sendGetToProxyServer(
                "/proxy/" + proxy.getPort() + "/har/entries",
                toStringMap("urlPattern", urlPatternToMatchUrl));
        String body1 = readResponseBody(conn1);
        HarEntry[] allCapturedEntries = new ObjectMapper().readValue(body1, HarEntry[].class);
        assertThat("Expected to find both entries", allCapturedEntries, Matchers.arrayWithSize(2));
        conn1.disconnect();

        String urlPattern = ".*" + urlPatternToMatchUrl;
        HttpURLConnection conn2 = sendGetToProxyServer(
                "/proxy/" + proxy.getPort() + "/" + getUrlPath(),
                toStringMap("urlPattern", urlPattern));
        String body2 = readResponseBody(conn2);
        HarEntry actualEntry = new ObjectMapper().readValue(body2, HarEntry.class);
        HarEntry expectedMostRecentEntry = Arrays.stream(allCapturedEntries)
                .max(Comparator.comparing(HarEntry::getStartedDateTime)).orElse(null);
        assertNotNull("Expected to find an entry", actualEntry);
        assertThat("Expected to find most recent entry containing url from url filter pattern",
                actualEntry.getRequest().getUrl(), Matchers.containsString(urlOfMostRecentRequest));
        assertThat("Expected that found entry has maximum started date time",
                actualEntry.getStartedDateTime(), Matchers.equalTo(expectedMostRecentEntry.getStartedDateTime()));
        conn2.disconnect();

        WireMock.verify(1, getRequestedFor(urlEqualTo("/" + urlOfMostRecentRequest)));
        WireMock.verify(1, getRequestedFor(urlEqualTo("/" + urlOfOldRequest)));
    }

    @Test
    public void getEmptyEntryIfNoEntryFoundByUrlPattern() throws Exception {
        mockTargetServerResponse(urlOfMostRecentRequest, responseBody);
        mockTargetServerResponse(urlOfOldRequest, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlOfOldRequest, responseBody);

        Thread.sleep(MILLISECONDS_BETWEEN_REQUESTS);

        requestToTargetServer(urlOfMostRecentRequest, responseBody);

        HttpURLConnection conn = sendGetToProxyServer(
                "/proxy/" + proxy.getPort() + "/" + getUrlPath(),
                toStringMap("urlPattern", urlPatternNotToMatchUrl));
        int statusCode = conn.getResponseCode();
        assertEquals(204, statusCode);
        conn.disconnect();

        WireMock.verify(1, getRequestedFor(urlEqualTo("/" + urlOfMostRecentRequest)));
        WireMock.verify(1, getRequestedFor(urlEqualTo("/" + urlOfOldRequest)));
    }
}
