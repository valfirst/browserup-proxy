package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarRequest;
import de.sstoehr.harreader.model.HarResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.browserup.bup.proxy.test.util.NewProxyServerTestUtil.getNewHttpClient;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * These tests use mocked server with long delay to verify behavior of 'unbalanced' har entries, where there might be request-only, response-only or HAR entry with both request and response, depending on when 'clean har' is called during request/response/reporting process.
 */
public class UnbalancedHarEntriesTest extends MockServerTest {
    private static final HarResponse DEFAULT_HAR_RESPONSE = new HarResponse();
    private static final HarRequest DEFAULT_HAR_REQUEST = new HarRequest();

    static {
        DEFAULT_HAR_REQUEST.setUrl("");
        DEFAULT_HAR_RESPONSE.setRedirectURL("");
    }

    private MitmProxyServer proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testResponseOnlyHarEntryReceivedIfNoResponseYet() throws Exception {
        //GIVEN
        String stubUrl = "/testResponseTimeoutCapturedInHar";
        int targetServerDelaySec = 5;
        int targetServiceResponseCode = 200;
        int idleConnectionTimeout = targetServerDelaySec + 1;

        configureMockServer(stubUrl, targetServerDelaySec, targetServiceResponseCode);

        proxy = startProxyAndCreateHar(idleConnectionTimeout);

        String requestUrl = "http://localhost:" + mockServerPort + stubUrl;

        //WHEN
        sendRequestToMockServer(requestUrl, targetServiceResponseCode);

        // Let request to be sent and captured
        Thread.sleep(500);

        Har har = proxy.getHar();

        //THEN
        assertThat("Expected to find entries in the HAR", har.getLog().getEntries(), not(empty()));

        String capturedUrl = har.getLog().getEntries().get(0).getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, har.getLog().getEntries().get(0).getResponse());
    }

    @Test
    public void testGetRequestOnlyEntryAndVerifyPopulatedEntry() throws Exception {
        //GIVEN
        String stubUrl = "/testResponseTimeoutCapturedInHar";
        int targetServerDelaySec = 5;
        int targetServiceResponseCode = 200;
        int idleConnectionTimeout = targetServerDelaySec + 1;

        configureMockServer(stubUrl, targetServerDelaySec, 200);

        proxy = startProxyAndCreateHar(idleConnectionTimeout);

        String requestUrl = "http://localhost:" + mockServerPort + stubUrl;

        //WHEN
        AtomicBoolean responseReceived = sendRequestToMockServer(requestUrl, targetServiceResponseCode);

        // Let request to be sent and captured
        Thread.sleep(500);

        //THEN
        // Verify we got request-only har entry
        Har har = proxy.getHar();
        HarEntry harEntry = har.getLog().getEntries().get(0);
        String capturedUrl = harEntry.getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, harEntry.getResponse());

        // Wait until response is received
        await().atMost(targetServerDelaySec + 1, TimeUnit.SECONDS).until(() -> responseReceived.get());

        // Verify we got response-only har entry
        har = proxy.getHar();
        harEntry = har.getLog().getEntries().get(0);
        assertNotEquals("Expected request to be not default", DEFAULT_HAR_REQUEST, harEntry.getRequest());
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);
        assertNotEquals("Expected response to be not defualt", DEFAULT_HAR_RESPONSE, harEntry.getResponse());
        assertEquals("Expected response status to be " + targetServiceResponseCode,
                harEntry.getResponse().getStatus(), targetServiceResponseCode);
        assertNotEquals("Expected response http version to be populated",
                harEntry.getResponse().getHttpVersion(), DEFAULT_HAR_RESPONSE.getHttpVersion());
    }

    @Test
    public void testMultipleRequestsAndForSlowOneWeGetOnlyResponseOnlyEntry() throws Exception {
        //GIVEN
        String slowEndpointUrl = "/testSlowEndpoint";
        String fastEndpointUrl = "/testFastEndpoint.*";
        int targetServerDelaySec = 5;
        int targetServiceResponseCode = 200;
        int idleConnectionTimeoutSec = targetServerDelaySec + 1;

        configureMockServer(slowEndpointUrl, targetServerDelaySec, targetServiceResponseCode);
        configureMockServer(fastEndpointUrl, 0, targetServiceResponseCode);

        proxy = startProxyAndCreateHar(idleConnectionTimeoutSec);

        String requestUrl = "http://localhost:" + mockServerPort + slowEndpointUrl;

        List<String> otherRequests = Arrays.asList(
                "http://localhost:" + mockServerPort + fastEndpointUrl + "/?1",
                "http://localhost:" + mockServerPort + fastEndpointUrl + "/?2",
                "http://localhost:" + mockServerPort + fastEndpointUrl + "/?3"
        );

        //WHEN
        AtomicBoolean responseReceived = sendRequestToMockServer(requestUrl, targetServiceResponseCode);
        List<AtomicBoolean> responsesReceived = new ArrayList<>();
        for (String req : otherRequests) {
            responsesReceived.add(sendRequestToMockServer(req, null));
        }
        int totalNumberOfRequests = otherRequests.size() + 1;

        // Wait for 'fast' requests
        await().atMost(7, TimeUnit.SECONDS).until(() ->
                responsesReceived.stream().allMatch(AtomicBoolean::get));

        //THEN
        // Verify we got request-only har entry for mocked server
        Har har = proxy.getHar();
        assertEquals("Expected to get correct number of entries", totalNumberOfRequests, har.getLog().getEntries().size());

        HarEntry entryForSlowEndpoint = har.getLog().getEntries().stream()
                .filter(e -> e.getRequest().getUrl().contains(slowEndpointUrl))
                .findFirst().orElse(null);
        String capturedUrl = entryForSlowEndpoint.getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, entryForSlowEndpoint.getResponse());

        // Wait until response is received
        await().atMost(targetServerDelaySec + 1, TimeUnit.SECONDS).until(() -> responseReceived.get());

        // Verify this time har entry for mocked server contains both request and response
        har = proxy.getHar();
        assertEquals("Expected to get correct number of entries", totalNumberOfRequests, har.getLog().getEntries().size());

        entryForSlowEndpoint = har.getLog().getEntries().stream()
                .filter(e -> e.getRequest().getUrl().contains(slowEndpointUrl))
                .findFirst().orElse(null);
        assertNotEquals("Expected request to be not default", DEFAULT_HAR_REQUEST, entryForSlowEndpoint.getRequest());
        assertEquals("URL captured in HAR did not match request URL", requestUrl, entryForSlowEndpoint.getRequest().getUrl());
        assertNotEquals("Expected response to be not defualt", DEFAULT_HAR_RESPONSE, entryForSlowEndpoint.getResponse());
        assertEquals("Got unexpected response status",
                entryForSlowEndpoint.getResponse().getStatus(), targetServiceResponseCode);
        assertNotEquals("Expected response http version to be populated",
                entryForSlowEndpoint.getResponse().getHttpVersion(), DEFAULT_HAR_RESPONSE.getHttpVersion());
    }

    @Test
    public void testMultipleRequestsAndAfterCleanHarWeGetOnlyOneResponseOnlyEntry() throws Exception {
        //GIVEN
        String slowEndpointUrl = "/testSlowEndpoint";
        String fastEndpointUrl = "/testFastEndpoint.*";
        int targetServerDelaySec = 5;
        int targetServiceResponseCode = 200;
        int idleConnectionTimeoutSec = targetServerDelaySec + 1;

        configureMockServer(slowEndpointUrl, targetServerDelaySec, targetServiceResponseCode);
        configureMockServer(fastEndpointUrl, 0, targetServiceResponseCode);

        proxy = startProxyAndCreateHar(idleConnectionTimeoutSec);

        String requestUrl = "http://localhost:" + mockServerPort + slowEndpointUrl;

        List<String> otherRequests = Arrays.asList(
                "http://localhost:" + mockServerPort + fastEndpointUrl + "/?1",
                "http://localhost:" + mockServerPort + fastEndpointUrl + "/?2",
                "http://localhost:" + mockServerPort + fastEndpointUrl + "/?3"
        );

        //WHEN
        AtomicBoolean responseReceived = sendRequestToMockServer(requestUrl, targetServiceResponseCode);
        List<AtomicBoolean> responsesReceived = new ArrayList<>();
        for (String req : otherRequests) {
            responsesReceived.add(sendRequestToMockServer(req, null));
        }
        int totalNumberOfRequests = otherRequests.size() + 1;

        // Wait for 'fast' requests
        await().atMost(7, TimeUnit.SECONDS).until(() ->
                responsesReceived.stream().allMatch(AtomicBoolean::get));

        //THEN
        // Verify we got request-only har entry for mocked server and clean har
        Har har = proxy.getHar(true);
        assertEquals("Expected to get correct number of entries", totalNumberOfRequests, har.getLog().getEntries().size());

        HarEntry entryForSlowEndpoint = har.getLog().getEntries().stream()
                .filter(e -> e.getRequest().getUrl().contains(slowEndpointUrl))
                .findFirst().orElse(null);
        String capturedUrl = entryForSlowEndpoint.getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, entryForSlowEndpoint.getResponse());

        // Wait until response is received
        await().atMost(targetServerDelaySec + 1, TimeUnit.SECONDS).until(() -> responseReceived.get());

        // Verify this time har entry for mocked server contains both request and response
        har = proxy.getHar();
        assertEquals("Expected to get only one entry for slow request after clean har", 1, har.getLog().getEntries().size());

        entryForSlowEndpoint = har.getLog().getEntries().get(0);
        assertEquals("Expected request to be default", DEFAULT_HAR_REQUEST, entryForSlowEndpoint.getRequest());
        assertNotEquals("Expected response to be not defualt", DEFAULT_HAR_RESPONSE, entryForSlowEndpoint.getResponse());
        assertEquals("Got unexpected response status",
                entryForSlowEndpoint.getResponse().getStatus(), targetServiceResponseCode);
        assertNotEquals("Expected response http version to be populated",
                entryForSlowEndpoint.getResponse().getHttpVersion(), DEFAULT_HAR_RESPONSE.getHttpVersion());
    }

    @Test
    public void testSlowEndpointGetRequestOnlyEntryAndCleanHarAndVerifyResponseOnlyEntry() throws Exception {
        //GIVEN
        String stubUrl = "/testResponseTimeoutCapturedInHar";
        int targetServerDelaySec = 5;
        int idleConnectionTimeoutSec = targetServerDelaySec + 1;
        int targetServiceResponseCode = 200;
        stubFor(get(urlEqualTo(stubUrl))
                .willReturn(aResponse().withStatus(targetServiceResponseCode)
                        .withFixedDelay((int) TimeUnit.SECONDS.toMillis(targetServerDelaySec))
                        .withBody("success"))
        );

        proxy = startProxyAndCreateHar(idleConnectionTimeoutSec);

        String requestUrl = "http://localhost:" + mockServerPort + stubUrl;

        AtomicBoolean responseReceived = sendRequestToMockServer(requestUrl, targetServiceResponseCode);

        // Let request to be sent and captured
        Thread.sleep(500);

        // Verify we got request-only har entry
        Har har = proxy.getHar();
        HarEntry harEntry = har.getLog().getEntries().get(0);
        String capturedUrl = harEntry.getRequest().getUrl();
        assertEquals("URL captured in HAR did not match request URL", requestUrl, capturedUrl);
        assertEquals("Expected response to be default", DEFAULT_HAR_RESPONSE, harEntry.getResponse());

        // Clean HAR
        proxy.getHar(true);

        // Wait until response is received
        await().atMost(targetServerDelaySec + 1, TimeUnit.SECONDS).until(() -> responseReceived.get());

        // Verify we got response-only har entry
        har = proxy.getHar();
        harEntry = har.getLog().getEntries().get(0);
        assertEquals("Expected request to be default", DEFAULT_HAR_REQUEST, harEntry.getRequest());
        assertNotEquals("Expected response to be not defualt", DEFAULT_HAR_RESPONSE, harEntry.getResponse());
        assertEquals("Expected response status to be " + targetServiceResponseCode,
                harEntry.getResponse().getStatus(), targetServiceResponseCode);
        assertNotEquals("Expected response http version to be populated",
                harEntry.getResponse().getHttpVersion(), DEFAULT_HAR_RESPONSE.getHttpVersion());
    }

    private void configureMockServer(String url, int delaySec, int responseCode) {
        stubFor(get(urlMatching(url))
                .willReturn(aResponse().withStatus(responseCode)
                        .withFixedDelay((int) TimeUnit.SECONDS.toMillis(delaySec))
                        .withBody("success"))
        );
    }

    private MitmProxyServer startProxyAndCreateHar(int idleConnectionTimeout) {
        proxy = new MitmProxyServer();
        proxy.setIdleConnectionTimeout(idleConnectionTimeout, TimeUnit.SECONDS);
        proxy.start();
        proxy.newHar();
        return proxy;
    }

    private AtomicBoolean sendRequestToMockServer(String requestUrl, Integer targetServiceResponseCode) {
        AtomicBoolean responseReceived = new AtomicBoolean(false);
        newSingleThreadExecutor().submit(() -> {
            try (CloseableHttpClient client = getNewHttpClient(proxy.getPort())) {
                CloseableHttpResponse response = client.execute(new HttpGet(requestUrl));
                responseReceived.set(true);
                if (targetServiceResponseCode != null) {
                    assertEquals("Did not receive HTTP " + targetServiceResponseCode + " from proxy",
                            (int) targetServiceResponseCode, response.getStatusLine().getStatusCode());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return responseReceived;
    }
}
