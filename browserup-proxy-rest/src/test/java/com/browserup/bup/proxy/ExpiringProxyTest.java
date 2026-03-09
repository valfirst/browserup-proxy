package com.browserup.bup.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Random;
import com.browserup.bup.BrowserUpProxyServer;
import org.junit.jupiter.api.Test;

class ExpiringProxyTest {
    @Test
    void testExpiredProxyStops() throws InterruptedException {
        int minPort = new Random().nextInt(50000) + 10000;

        ProxyManager proxyManager = new ProxyManager(
            minPort,
            minPort + 100,
            2);

        BrowserUpProxyServer proxy = proxyManager.create();
        int port = proxy.getPort();

        BrowserUpProxyServer retrievedProxy = proxyManager.get(port);

        assertEquals(proxy, retrievedProxy, "ProxyManager did not return the expected proxy instance");

        Thread.sleep(2500);

        // explicitly create a new proxy to cause a write to the cache. cleanups happen on "every" write and "occasional" reads, so force a cleanup by writing.
        int newPort = proxyManager.create().getPort();
        proxyManager.delete(newPort);

        BrowserUpProxyServer expiredProxy = proxyManager.get(port);

        assertNull(expiredProxy, "ProxyManager did not expire proxy as expected");
    }

    @Test
    void testZeroTtlProxyDoesNotExpire() throws InterruptedException {
        int minPort = new Random().nextInt(50000) + 10000;

        ProxyManager proxyManager = new ProxyManager(
            minPort,
            minPort + 100,
            0);

        BrowserUpProxyServer proxy = proxyManager.create();
        int port = proxy.getPort();

        BrowserUpProxyServer retrievedProxy = proxyManager.get(port);

        assertEquals(proxy, retrievedProxy, "ProxyManager did not return the expected proxy instance");

        Thread.sleep(2500);

        // explicitly create a new proxy to cause a write to the cache. cleanups happen on "every" write and "occasional" reads, so force a cleanup by writing.
        int newPort = proxyManager.create().getPort();
        proxyManager.delete(newPort);

        BrowserUpProxyServer nonExpiredProxy = proxyManager.get(port);

        assertEquals(proxy, nonExpiredProxy, "ProxyManager did not return the expected proxy instance");
    }

}