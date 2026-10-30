package com.browserup.bup.proxy.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.bricks.ProxyResource;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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

    protected WireMockServer wireMockRule;

    @BeforeEach
    protected void setUpMockServer() {
        wireMockRule = new WireMockServer(options().port(0).httpsPort(0));
        wireMockRule.start();
        WireMock.configureFor("localhost", wireMockRule.port());
        mockServerPort = wireMockRule.port();
        mockServerHttpsPort = wireMockRule.httpsPort();
    }

    @AfterEach
    protected void tearDownMockServer() {
        if (wireMockRule != null && wireMockRule.isRunning()) {
            wireMockRule.stop();
        }
    }

    @BeforeEach
    protected void setUpProxyResource() {
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
