package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import de.sstoehr.harreader.model.Har;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GetHarTest extends MockServerTest {
    private BrowserUpProxy proxy;

    @AfterEach
    protected void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    void testGetHarClean() throws Exception {
        String stubUrl = "/testCaptureResponseCookiesInHar";
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(ok()
                .withBody("success"))
        );

        proxy = new BrowserUpProxyServer();
        proxy.setHarCaptureTypes(EnumSet.of(CaptureType.RESPONSE_COOKIES));
        proxy.setTrustAllServers(true);
        proxy.start();

        proxy.newHar();

        try (CloseableHttpClient httpClient = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            String responseBody = NewProxyServerTestUtil.toStringAndClose(httpClient.execute(new HttpGet("https://localhost:" + mockServerHttpsPort + "/testCaptureResponseCookiesInHar")).getEntity().getContent());
            assertEquals("success", responseBody, "Did not receive expected response from mock server");
        }

        Thread.sleep(500);
        Har har = proxy.getHar();

        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));
        har = proxy.getHar(true);
        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        har = proxy.getHar();
        assertThat("Expected to find no entries in the HAR", har.getLog().getEntries(), empty());

        verify(1, getRequestedFor(urlEqualTo(stubUrl)));
    }
}
