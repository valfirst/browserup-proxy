package com.browserup.bup;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import de.sstoehr.harreader.model.Har;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MitmProxyProcessManagerTest {

    private static final int PROXY_PORT = 8443;
    private final MitmProxyProcessManager mitmProxyManager = new MitmProxyProcessManager();

    @After
    public void setUp() {
        mitmProxyManager.stop();
    }

    @Test
    public void proxyStartedAndHarIsAvailable() throws IOException, InterruptedException {
        //GIVEN
        mitmProxyManager.start(PROXY_PORT);

        //WHEN
        sendRequestThroughProxy(PROXY_PORT);

        //THEN
        Har har = mitmProxyManager.getHarCaptureFilterManager().getHar();
        assertNotNull(har);
        assertEquals("Expected to capture 1 har entry", 1, har.getLog().getEntries().size());
    }

    @Test
    public void proxyStartedAndHarCleanRequestParamCleansHar() throws IOException, InterruptedException {
        //GIVEN
        mitmProxyManager.start(PROXY_PORT);

        //WHEN
        sendRequestThroughProxy(PROXY_PORT);

        //THEN
        Har har = mitmProxyManager.getHarCaptureFilterManager().getHar();
        assertNotNull(har);
        assertEquals("Expected to capture 1 har entry", 1, har.getLog().getEntries().size());

        har = mitmProxyManager.getHarCaptureFilterManager().getHar(true);
        assertNotNull(har);
        assertEquals("Expected to capture 1 har entry", 1, har.getLog().getEntries().size());

        har = mitmProxyManager.getHarCaptureFilterManager().getHar();
        assertNotNull(har);
        assertEquals("Expected to capture no har entries", 0, har.getLog().getEntries().size());
    }

    @Test
    public void proxyStartedCurrentHarIsBeingPopulated() throws IOException, InterruptedException {
        //GIVEN
        mitmProxyManager.start(PROXY_PORT);

        //WHEN
        int reqNumber = 5;
        for (int i = 0; i < reqNumber; i++) {
            sendRequestThroughProxy(PROXY_PORT);
        }

        //THEN
        Har har = mitmProxyManager.getHarCaptureFilterManager().getHar();
        assertNotNull(har);
        assertEquals("Expected to capture " + reqNumber + " har entries", reqNumber, har.getLog().getEntries().size());

        // One more request through proxy
        sendRequestThroughProxy(PROXY_PORT);

        har = mitmProxyManager.getHarCaptureFilterManager().getHar();
        assertNotNull(har);
        assertEquals("Expected to capture 1 har entry", reqNumber + 1, har.getLog().getEntries().size());
    }

    @Test
    @Ignore
    public void afterStopLastCapturedHarIsReturned() throws IOException, InterruptedException {
        //GIVEN
        int proxyPort = 18443;
        mitmProxyManager.start(proxyPort);

        //WHEN
        sendRequestThroughProxy(proxyPort);

        mitmProxyManager.stop();
        Exception ex = null;
        try {
            sendRequestThroughProxy(proxyPort);
        } catch (Exception e) {
            ex = e;
        }

        assertNotNull(ex);
        assertTrue(ex.getMessage().contains("Connection refused"));

        Har har = mitmProxyManager.getHarCaptureFilterManager().getHar();
        assertNotNull(har);
        assertEquals("Expected to capture 1 har entry", 1, har.getLog().getEntries().size());
    }

    @Test
    public void afterStopConnectionToProxyRefused() throws IOException, InterruptedException {
        //GIVEN
        int proxyPort = 18443;
        mitmProxyManager.start(proxyPort);

        //WHEN
        sendRequestThroughProxy(proxyPort);

        mitmProxyManager.stop();
        Exception ex = null;
        try {
            sendRequestThroughProxy(proxyPort);
        } catch (Exception e) {
            ex = e;
        }

        assertNotNull(ex);
        assertThat(ex.getMessage(), containsString("Connection refused"));
    }

    private void sendRequestThroughProxy(int proxyPort) throws IOException, InterruptedException {
        HttpResponse<Void> httpResponse = HttpClient
                .newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress("localhost", proxyPort)))
                .build()
                .send(HttpRequest.newBuilder()
                        .uri(URI.create("http://petclinic.targets.browserup.com/"))
                        .GET()
                        .build(), HttpResponse.BodyHandlers.discarding()
                );
        assertEquals(200, httpResponse.statusCode());
    }
}
