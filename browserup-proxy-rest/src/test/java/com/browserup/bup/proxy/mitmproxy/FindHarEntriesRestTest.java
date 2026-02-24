package com.browserup.bup.proxy.mitmproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.sstoehr.harreader.model.HarEntry;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class FindHarEntriesRestTest extends BaseRestTest {

    @Override
    protected String getUrlPath() {
        return "har/entries";
    }

    @Test
    public void findHarEntryByUrlPattern() throws Exception {
        String urlToCatch = "test";
        String urlNotToCatch = "missing";
        String responseBody = "success";

        mockTargetServerResponse(urlToCatch, responseBody);
        mockTargetServerResponse(urlNotToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);
        requestToTargetServer(urlNotToCatch, responseBody);

        String urlPattern = ".*" + urlToCatch;
        HttpURLConnection conn = sendGetToProxyServer(
                "/proxy/" + proxy.getPort() + "/" + getUrlPath(),
                toStringMap("urlPattern", urlPattern));
        String body = readResponseBody(conn);
        HarEntry[] entries = new ObjectMapper().readValue(body, HarEntry[].class);
        assertThat("Expected to find only one entry", entries, Matchers.arrayWithSize(1));
        assertThat("Expected to find entry containing url from url filter pattern",
                entries[0].getRequest().getUrl(), Matchers.containsString(urlToCatch));
        assertThat("Expected to find no entries with urlNotToCatch filter",
                entries[0].getRequest().getUrl(), Matchers.not(Matchers.containsString(urlNotToCatch)));
        conn.disconnect();

        WireMock.verify(1, getRequestedFor(urlEqualTo("/" + urlToCatch)));
        WireMock.verify(1, getRequestedFor(urlEqualTo("/" + urlNotToCatch)));
    }

    @Test
    public void getEmptyEntriesArrayIfNoEntriesFoundByUrl() throws Exception {
        String urlToCatch = "test";
        String urlNotToCatch = "missing";
        String responseBody = "success";

        mockTargetServerResponse(urlNotToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlNotToCatch, responseBody);

        String urlPattern = ".*" + urlToCatch;
        HttpURLConnection conn = sendGetToProxyServer(
                "/proxy/" + proxy.getPort() + "/" + getUrlPath(),
                toStringMap("urlPattern", urlPattern));
        String body = readResponseBody(conn);
        HarEntry[] entries = new ObjectMapper().readValue(body, HarEntry[].class);
        assertThat("Expected get empty har entries array", entries, Matchers.arrayWithSize(0));
        conn.disconnect();

        WireMock.verify(1, getRequestedFor(urlEqualTo("/" + urlNotToCatch)));
    }
}
