package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarPage;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.Optional;

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class DefaultStepIdTest extends MockServerTest {
    private static final String SUCCESSFUL_RESPONSE_BODY = "success";
    private static final String FIRST_URL = "first-url";
    private static final String SECOND_URL = "second-url";
    private static final String THIRD_URL = "third-url";
    private static final String INITIAL_STEP_NAME = "Step Name";
    private static final String DEFAULT_STEP_NAME = "Default";

    private MitmProxyServer proxy;
    private CloseableHttpClient clientToProxy;

    @Before
    public void startUp() {
        proxy = new MitmProxyServer();
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
    public void testDefaultStepIdIfNoCurrentPage() throws Exception {
        proxy.newHar(INITIAL_STEP_NAME);

        Thread.sleep(2000);

        mockResponseForPath(FIRST_URL);
        mockResponseForPath(SECOND_URL);
        mockResponseForPath(THIRD_URL);

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody);
        assertEquals("Expected first request entry to have initial page ref", INITIAL_STEP_NAME, proxy.getHar().getLog().getEntries().get(0).getPageref());
        assertThat("Expected 1 page available", proxy.getHar().getLog().getPages(), Matchers.hasSize(1));

        proxy.endPage();

        Thread.sleep(10);

        String secondUrl = "http://localhost:" + mockServerPort + "/" + SECOND_URL;

        String respBody2 = toStringAndClose(clientToProxy.execute(new HttpGet(secondUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody2);
        HarPage defaultPage = proxy.getHar().getLog().getPages().stream()
                .filter(p -> p.getId().equals(DEFAULT_STEP_NAME))
                .findFirst().orElse(null);
        assertNotNull("Expected not null default page", defaultPage);
        assertNotNull("Expected page with started date time", defaultPage.getStartedDateTime());
        Optional<HarEntry> newestEntry = proxy.getHar().getLog().getEntries().stream()
                .max(Comparator.comparing(HarEntry::getStartedDateTime));
        assertTrue(newestEntry.isPresent());
        assertEquals("Expected to get default step name ", DEFAULT_STEP_NAME, newestEntry.get().getPageref());

        String thirdUrl = "http://localhost:" + mockServerPort + "/" + THIRD_URL;

        String respBody3 = toStringAndClose(clientToProxy.execute(new HttpGet(thirdUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody3);

        assertThat("Expected two pages available", proxy.getHar().getLog().getPages(), Matchers.hasSize(2));
        assertNotNull("Expected to find default page among pages",
                proxy.getHar().getLog().getPages().stream()
                        .filter(p -> p.getId().equals(DEFAULT_STEP_NAME))
                        .findFirst().orElse(null));
    }

    @Test
    public void testHarIsCreatedAfterFirstRequestIfNoNewHarCalled() throws Exception {
        mockResponseForPath(FIRST_URL);

        assertNull("Expected null har before any requests sent", proxy.getHar());

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody);

        assertNotNull("Expected non null har after request sent", proxy.getHar());
    }

    @Test
    public void testEntryCapturedIfNoNewHarCalled() throws Exception {
        mockResponseForPath(FIRST_URL);

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody);

        assertThat("Expected to get one entry", proxy.getHar().getLog().getEntries(), Matchers.hasSize(1));
        assertEquals("Expected entry to have default page ref", proxy.getHar().getLog().getEntries().get(0).getPageref(), DEFAULT_STEP_NAME);
    }

    @Test
    public void testEntryWithDefaultPageRefRemovedAfterNewHarCreated() throws Exception {
        mockResponseForPath(FIRST_URL);

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals("Did not receive expected response from mock server", SUCCESSFUL_RESPONSE_BODY, respBody);

        assertThat("Expected to get one entry", proxy.getHar().getLog().getEntries(), Matchers.hasSize(1));
        assertEquals("Expected entry to have default page ref", proxy.getHar().getLog().getEntries().get(0).getPageref(), DEFAULT_STEP_NAME);

        proxy.newHar(INITIAL_STEP_NAME);
        assertThat("Expected to get no entries after new har called", proxy.getHar().getLog().getEntries(), Matchers.hasSize(0));
    }

    private void mockResponseForPath(String path) {
        stubFor(get(urlEqualTo("/" + path)).willReturn(ok().withBody(SUCCESSFUL_RESPONSE_BODY)));
    }
}
