package com.browserup.bup.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.test.util.MockServerTest;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LatencyTest extends MockServerTest {
    private MitmProxyServer proxy;

    @After
    public void tearDown() {
        if (proxy != null && proxy.isStarted()) {
            proxy.abort();
        }
    }

    @Test
    public void testLatency() throws Exception {
        String url = "/latency";
        stubFor(get(urlEqualTo(url)).willReturn(ok()));

        proxy = new MitmProxyServer();
        proxy.setLatency(2, TimeUnit.SECONDS);
        proxy.start();

        try (CloseableHttpClient client = NewProxyServerTestUtil.getNewHttpClient(proxy.getPort())) {
            long start = System.nanoTime();
            HttpResponse response = client.execute(new HttpGet("http://127.0.0.1:" + mockServerPort + "/latency"));
            long stop = System.nanoTime();

            assertEquals("Expected to receive an HTTP 200 from the upstream server", 200, response.getStatusLine().getStatusCode());
            assertTrue("Expected latency to be at least 2 seconds. Total time was: " + TimeUnit.MILLISECONDS.convert(stop - start, TimeUnit.NANOSECONDS) + "ms",
                    TimeUnit.SECONDS.convert(stop - start, TimeUnit.NANOSECONDS) >= 2);
        }

        verify(1, getRequestedFor(urlEqualTo(url)));
    }
}
