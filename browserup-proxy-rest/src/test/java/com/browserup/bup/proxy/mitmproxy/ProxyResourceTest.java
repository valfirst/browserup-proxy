package com.browserup.bup.proxy.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.bricks.ProxyResource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.sitebricks.headless.Request;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    protected static Request createMockRestRequestWithEntity(String entityBody) throws IOException {
        Request mockRestRequest = mock(Request.class);
        when(mockRestRequest.header("Content-Type")).thenReturn("text/plain; charset=utf-8");
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                OutputStream os = (OutputStream) invocationOnMock.getArguments()[0];
                os.write(entityBody.getBytes(StandardCharsets.UTF_8));
                return null;
            }
        }).when(mockRestRequest).readTo(any(OutputStream.class));
        return mockRestRequest;
    }
}
