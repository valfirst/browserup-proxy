package com.browserup.bup.proxy.mitmproxy

import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.bricks.ProxyResource
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.sitebricks.headless.Request
import groovyx.net.http.HTTPBuilder
import org.junit.Before
import org.junit.Rule
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import java.nio.charset.StandardCharsets

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

abstract class ProxyResourceTest extends ProxyManagerTest {
    ProxyResource proxyResource
    int proxyPort
    protected int mockServerPort
    protected int mockServerHttpsPort

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(0).httpsPort(0))

    @Before
    void setUpMockServer() {
        mockServerPort = wireMockRule.port()
        mockServerHttpsPort = wireMockRule.httpsPort()
    }

    @Before
    void setUpProxyResource() {
        MitmProxyServer proxy = proxyManager.create(0)
        proxyPort = proxy.port

        proxyResource = new ProxyResource(proxyManager)
    }

    HTTPBuilder getHttpBuilder() {
        def http = new HTTPBuilder("http://localhost:${mockServerPort}")
        http.setProxy("localhost", proxyPort, "http")

        return http
    }

    /**
     * Creates a mock sitebricks REST request with the specified entity body.
     */
    static Request createMockRestRequestWithEntity(String entityBody) {
        Request mockRestRequest = mock(Request)
        when(mockRestRequest.header("Content-Type")).thenReturn("text/plain; charset=utf-8")
        when(mockRestRequest.readTo(any(OutputStream))).then(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                OutputStream os = invocationOnMock.getArguments()[0]
                os.write(entityBody.getBytes(StandardCharsets.UTF_8))

                return null
            }
        })
        mockRestRequest
    }
}