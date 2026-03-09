package com.browserup.bup;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import de.sstoehr.harreader.model.Har;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MitmProxyProcessManagerTest {

    private static final int PROXY_PORT = 8443;
    private final MitmProxyProcessManager mitmProxyManager = new MitmProxyProcessManager();

    @AfterEach
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
        assertEquals(1, har.getLog().getEntries().size(), "Expected to capture 1 har entry");
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
        assertEquals(1, har.getLog().getEntries().size(), "Expected to capture 1 har entry");

        har = mitmProxyManager.getHarCaptureFilterManager().getHar(true);
        assertNotNull(har);
        assertEquals(1, har.getLog().getEntries().size(), "Expected to capture 1 har entry");

        har = mitmProxyManager.getHarCaptureFilterManager().getHar();
        assertNotNull(har);
        assertEquals(0, har.getLog().getEntries().size(), "Expected to capture no har entries");
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
        assertEquals(reqNumber, har.getLog().getEntries().size(), "Expected to capture " + reqNumber + " har entries");

        // One more request through proxy
        sendRequestThroughProxy(PROXY_PORT);

        har = mitmProxyManager.getHarCaptureFilterManager().getHar();
        assertNotNull(har);
        assertEquals(reqNumber + 1, har.getLog().getEntries().size(), "Expected to capture 1 har entry");
    }

    @Test
    @Disabled
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
        assertEquals(1, har.getLog().getEntries().size(), "Expected to capture 1 har entry");
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
