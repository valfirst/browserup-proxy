package com.browserup.bup.proxy.test.util;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * A base class that spins up and shuts down a BrowserUpProxy instance using the new interface. It also provides mock server support via
 * {@link com.browserup.bup.proxy.test.util.MockServerTest}.
 */
public class NewProxyServerTest extends MockServerTest {
    protected BrowserUpProxy proxy;

    @BeforeEach
    protected void setUpProxyServer() {
        proxy = new BrowserUpProxyServer();
        proxy.start();
    }

    @AfterEach
    protected void shutDownProxyServer() {
        if (proxy != null) {
            proxy.abort();
        }
    }

}
