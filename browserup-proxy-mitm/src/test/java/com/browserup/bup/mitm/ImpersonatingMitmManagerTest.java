package com.browserup.bup.mitm;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import com.browserup.bup.mitm.keys.ECKeyGenerator;
import com.browserup.bup.mitm.keys.RSAKeyGenerator;
import com.browserup.bup.mitm.manager.ImpersonatingMitmManager;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImpersonatingMitmManagerTest {
    private final SSLSession mockSession = mock(SSLSession.class);

    @Test
    void testCreateDefaultServerEngine() {
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder().build();

        SSLEngine serverSslEngine = mitmManager.serverSslEngine("hostname", 80);
        assertNotNull(serverSslEngine);
    }

    @Test
    void testCreateDefaultClientEngine() {
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder().build();

        when(mockSession.getPeerHost()).thenReturn("hostname");

        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "https://test.connection");
        SSLEngine clientSslEngine = mitmManager.clientSslEngineFor(request, mockSession);
        assertNotNull(clientSslEngine);
    }

    @Test
    void testCreateCAAndServerCertificatesOfDifferentTypes() {
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(RootCertificateGenerator.builder().keyGenerator(new RSAKeyGenerator()).build())
                .serverKeyGenerator(new ECKeyGenerator())
                .build();

        when(mockSession.getPeerHost()).thenReturn("hostname");

        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "https://test.connection");
        SSLEngine clientSslEngine = mitmManager.clientSslEngineFor(request, mockSession);
        assertNotNull(clientSslEngine);
    }
}
