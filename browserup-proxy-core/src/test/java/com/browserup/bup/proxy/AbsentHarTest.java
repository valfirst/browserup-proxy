package com.browserup.bup.proxy;

import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.sstoehr.harreader.model.HarEntry;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AbsentHarTest extends MockServerTest {

    private static final String SUCCESSFUL_RESPONSE_BODY = "success";
    private static final String FIRST_URL = "first-url";
    private static final String SECOND_URL = "second-url";
    private static final String THIRD_URL = "third-url";
    private static final String INITIAL_STEP_NAME = "Step Name";
    private static final String DEFAULT_STEP_NAME = "Default";

    private BrowserUpProxyServer proxy;
    private CloseableHttpClient clientToProxy;

    @Before
    public void startUp() {
        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        clientToProxy = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort());
    }

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void noHarAvailableCallNewPageCheckHarCreatedAndRequestsLogged() throws IOException, InterruptedException {
        proxy.newPage(INITIAL_STEP_NAME, INITIAL_STEP_NAME);

        mockResponseForPath(FIRST_URL);
        mockResponseForPath(SECOND_URL);
        mockResponseForPath(THIRD_URL);

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = NewProxyServerTestUtil.toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody);
        assertEquals("Expected first request entry to have initial page ref", INITIAL_STEP_NAME,
                proxy.getHar().getLog().getEntries().get(0).getPageref());
        MatcherAssert.assertThat("Expected 1 page available", proxy.getHar().getLog().getPages(), Matchers.hasSize(1));

        proxy.endPage();

        Thread.sleep(10);

        String secondUrl = "http://localhost:" + mockServerPort + "/" + SECOND_URL;

        String respBody2 = NewProxyServerTestUtil.toStringAndClose(clientToProxy.execute(new HttpGet(secondUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody2);

        Optional<HarEntry> newestEntry = proxy.getHar().getLog().getEntries().stream()
                .max(Comparator.comparing(HarEntry::getStartedDateTime));
        assertTrue(newestEntry.isPresent());
        assertEquals("Expected to get default step name ", DEFAULT_STEP_NAME, newestEntry.get().getPageref());

        String thirdUrl = "http://localhost:" + mockServerPort + "/" + THIRD_URL;

        String respBody3 = NewProxyServerTestUtil.toStringAndClose(clientToProxy.execute(new HttpGet(thirdUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody3);

        MatcherAssert.assertThat("Expected two pages available", proxy.getHar().getLog().getPages(), Matchers.hasSize(2));
        assertTrue("Expected to find default page among pages",
                proxy.getHar().getLog().getPages().stream().anyMatch(p -> p.getId().equals(DEFAULT_STEP_NAME)));
    }

    @Test
    public void noHarAvailableCallEndPage() {
        proxy.endPage();

        //No exception thrown
    }

    private void mockResponseForPath(final String path) {
        stubFor(get(urlEqualTo("/" + path)).willReturn(ok().withBody(SUCCESSFUL_RESPONSE_BODY)));
    }
}
