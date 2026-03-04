package com.browserup.bup.proxy.test.util;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.bricks.ProxyResource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public abstract class ProxyResourceTest extends ProxyManagerTest {
    protected ProxyResource proxyResource;
    protected int proxyPort;
    protected int mockServerPort;
    protected int mockServerHttpsPort;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(0).httpsPort(0));

    @Before
    public void setUpMockServer() {
        mockServerPort = wireMockRule.port();
        mockServerHttpsPort = wireMockRule.httpsPort();
    }

    @Before
    public void setUpProxyResource() {
        MitmProxyServer proxy = proxyManager.create(0);
        proxyPort = proxy.getPort();

        proxyResource = new ProxyResource(proxyManager);
    }

    protected HttpURLConnection getHttpConnection(String path) throws Exception {
        URL url = new URL("http://localhost:" + mockServerPort + path);
        Proxy proxyAddr = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", proxyPort));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxyAddr);
        conn.setRequestMethod("GET");
        return conn;
    }
}
