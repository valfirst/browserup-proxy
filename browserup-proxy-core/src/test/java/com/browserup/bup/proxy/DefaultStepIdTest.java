package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarPage;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.toStringAndClose;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DefaultStepIdTest extends MockServerTest {
    private static final String SUCCESSFUL_RESPONSE_BODY = "success";
    private static final String FIRST_URL = "first-url";
    private static final String SECOND_URL = "second-url";
    private static final String THIRD_URL = "third-url";
    private static final String INITIAL_STEP_NAME = "Step Name";
    private static final String DEFAULT_STEP_NAME = "Default";

    private BrowserUpProxyServer proxy;
    private CloseableHttpClient clientToProxy;

    @BeforeEach
    public void startUp() {
        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start();

        clientToProxy = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort());
    }

    @AfterEach
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testDefaultStepIdIfNoCurrentPage() throws Exception {
        proxy.newHar(INITIAL_STEP_NAME);

        mockResponseForPath(FIRST_URL);
        mockResponseForPath(SECOND_URL);
        mockResponseForPath(THIRD_URL);

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals(SUCCESSFUL_RESPONSE_BODY, respBody, "Did not receive expected response from mock server");
        assertEquals(INITIAL_STEP_NAME, proxy.getHar().getLog().getEntries().get(0).getPageref(), "Expected first request entry to have initial page ref");
        assertThat("Expected 1 page available", proxy.getHar().getLog().getPages(), Matchers.hasSize(1));

        proxy.endPage();

        Thread.sleep(10);

        String secondUrl = "http://localhost:" + mockServerPort + "/" + SECOND_URL;

        String respBody2 = toStringAndClose(clientToProxy.execute(new HttpGet(secondUrl)).getEntity().getContent());
        assertEquals(SUCCESSFUL_RESPONSE_BODY, respBody2, "Did not receive expected response from mock server");
        HarPage defaultPage = proxy.getHar().getLog().getPages().stream().filter(p -> p.getId().equals(DEFAULT_STEP_NAME)).findFirst().orElse(null);
        assertNotNull(defaultPage, "Expected not null default page");
        assertNotNull(defaultPage.getStartedDateTime(), "Expected page with started date time");
        HarEntry newestEntry = proxy.getHar().getLog().getEntries().stream().sorted(Comparator.comparing(HarEntry::getStartedDateTime).reversed()).findFirst().orElse(null);
        assertEquals(DEFAULT_STEP_NAME, newestEntry.getPageref(), "Expected to get default step name ");

        String thirdUrl = "http://localhost:" + mockServerPort + "/" + THIRD_URL;

        String respBody3 = toStringAndClose(clientToProxy.execute(new HttpGet(thirdUrl)).getEntity().getContent());
        assertEquals(SUCCESSFUL_RESPONSE_BODY, respBody3, "Did not receive expected response from mock server");

        assertThat("Expected two pages available", proxy.getHar().getLog().getPages(), Matchers.hasSize(2));
        assertNotNull(proxy.getHar().getLog().getPages().stream().filter(p -> p.getId().equals(DEFAULT_STEP_NAME)).findFirst().orElse(null), "Expected to find default page among pages");
    }

    @Test
    public void testHarIsCreatedAfterFirstRequestIfNoNewHarCalled() throws Exception {
        mockResponseForPath(FIRST_URL);

        assertNull(proxy.getHar(), "Expected null har before any requests sent");

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals(SUCCESSFUL_RESPONSE_BODY, respBody, "Did not receive expected response from mock server");

        assertNotNull(proxy.getHar(), "Expected non null har after request sent");
    }

    @Test
    public void testEntryCapturedIfNoNewHarCalled() throws Exception {
        mockResponseForPath(FIRST_URL);

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals(SUCCESSFUL_RESPONSE_BODY, respBody, "Did not receive expected response from mock server");

        assertThat("Expected to get one entry", proxy.getHar().getLog().getEntries(), Matchers.hasSize(1));
        assertEquals(DEFAULT_STEP_NAME, proxy.getHar().getLog().getEntries().get(0).getPageref(), "Expected entry to have default page ref");
    }

    @Test
    public void testEntryWithDefaultPageRefRemovedAfterNewHarCreated() throws Exception {
        mockResponseForPath(FIRST_URL);

        String firstUrl = "http://localhost:" + mockServerPort + "/" + FIRST_URL;

        String respBody = toStringAndClose(clientToProxy.execute(new HttpGet(firstUrl)).getEntity().getContent());
        assertEquals(SUCCESSFUL_RESPONSE_BODY, respBody, "Did not receive expected response from mock server");

        assertThat("Expected to get one entry", proxy.getHar().getLog().getEntries(), Matchers.hasSize(1));
        assertEquals(DEFAULT_STEP_NAME, proxy.getHar().getLog().getEntries().get(0).getPageref(), "Expected entry to have default page ref");

        proxy.newHar(INITIAL_STEP_NAME);
        assertThat("Expected to get no entries after new har called", proxy.getHar().getLog().getEntries(), Matchers.hasSize(0));
    }

    private void mockResponseForPath(String path) {
        stubFor(get(urlEqualTo("/" + path)).willReturn(ok().withBody(SUCCESSFUL_RESPONSE_BODY)));
    }
}
