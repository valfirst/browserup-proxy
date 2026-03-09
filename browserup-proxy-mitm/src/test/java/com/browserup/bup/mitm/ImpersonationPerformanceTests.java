package com.browserup.bup.mitm;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import com.browserup.bup.mitm.keys.ECKeyGenerator;
import com.browserup.bup.mitm.keys.KeyGenerator;
import com.browserup.bup.mitm.keys.RSAKeyGenerator;
import com.browserup.bup.mitm.manager.ImpersonatingMitmManager;
import com.browserup.bup.mitm.tools.BouncyCastleSecurityProviderTool;
import com.browserup.bup.mitm.tools.DefaultSecurityProviderTool;
import com.browserup.bup.mitm.util.MitmConstants;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.security.KeyPair;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ImpersonationPerformanceTests {
    private static final Logger log = LoggerFactory.getLogger(ImpersonationPerformanceTests.class);

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(new RSAKeyGenerator(), "SHA384", new RSAKeyGenerator(), "SHA384"),
                Arguments.of(new RSAKeyGenerator(), "SHA384", new RSAKeyGenerator(1024), "SHA384"),
                Arguments.of(new RSAKeyGenerator(1024), "SHA384", new RSAKeyGenerator(1024), "SHA384"),
                Arguments.of(new RSAKeyGenerator(), "SHA384", new ECKeyGenerator(), "SHA384"),
                Arguments.of(new ECKeyGenerator(), "SHA384", new ECKeyGenerator(), "SHA384"),
                Arguments.of(new ECKeyGenerator(), "SHA384", new RSAKeyGenerator(), "SHA384")
        );
    }

    private static final int WARM_UP_ITERATIONS = 5;

    private static final int ITERATIONS = 50;

    @ParameterizedTest
    @MethodSource("data")
    public void testImpersonatingMitmManagerPerformance(KeyGenerator rootCertKeyGen, String rootCertDigest, KeyGenerator serverCertKeyGen, String serverCertDigest) {
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(RootCertificateGenerator.builder()
                        .keyGenerator(rootCertKeyGen)
                        .messageDigest(rootCertDigest)
                        .build())
                .serverKeyGenerator(serverCertKeyGen)
                .serverMessageDigest(serverCertDigest)
                .build();

        final AtomicInteger iteration = new AtomicInteger();

        SSLSession mockSession = Mockito.mock(SSLSession.class);

        log.info("Test parameters:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);

        // warm up, init root cert, etc.
        log.info("Executing {} warm up iterations", WARM_UP_ITERATIONS);
        for (iteration.set(0); iteration.get() < WARM_UP_ITERATIONS; iteration.incrementAndGet()) {
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "https://warmup-" + iteration.get() + ".com");
            mitmManager.clientSslEngineFor(request, mockSession);
        }

        log.info("Executing {} performance test iterations", ITERATIONS);

        long start = System.currentTimeMillis();

        for (iteration.set(0); iteration.get() < ITERATIONS; iteration.incrementAndGet()) {
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "https://" + iteration.get() + ".com");
            mitmManager.clientSslEngineFor(request, mockSession);
        }

        long finish = System.currentTimeMillis();

        log.info("Finished performance test:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);
        log.info("Generated {} certificates in {}ms. Average time per certificate: {}ms", iteration.get(), finish - start, (finish - start) / iteration.get());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testServerCertificateCreationAndAssembly(KeyGenerator rootCertKeyGen, String rootCertDigest, KeyGenerator serverCertKeyGen, String serverCertDigest) {
        CertificateAndKey rootCert = RootCertificateGenerator.builder()
                .keyGenerator(rootCertKeyGen)
                .messageDigest(rootCertDigest)
                .build()
                .load();

        log.info("Test parameters:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);

        log.info("Executing {} warm up iterations", WARM_UP_ITERATIONS);
        IntStream.range(0, WARM_UP_ITERATIONS).forEach(i -> {
            KeyPair serverCertKeyPair = serverCertKeyGen.generate();
            CertificateAndKey serverCert = new BouncyCastleSecurityProviderTool().createServerCertificate(
                    createCertificateInfo("warnmup-" + i + ".com"),
                    rootCert.getCertificate(),
                    rootCert.getPrivateKey(),
                    serverCertKeyPair,
                    serverCertDigest);
            new DefaultSecurityProviderTool().createServerKeyStore(MitmConstants.DEFAULT_KEYSTORE_TYPE, serverCert, rootCert.getCertificate(), "alias", "password");
        });

        log.info("Executing {} performance test iterations", ITERATIONS);

        long start = System.currentTimeMillis();

        IntStream.range(0, ITERATIONS).forEach(i -> {
            KeyPair serverCertKeyPair = serverCertKeyGen.generate();
            CertificateAndKey serverCert = new BouncyCastleSecurityProviderTool().createServerCertificate(
                    createCertificateInfo(i + ".com"),
                    rootCert.getCertificate(),
                    rootCert.getPrivateKey(),
                    serverCertKeyPair,
                    serverCertDigest);
            new DefaultSecurityProviderTool().createServerKeyStore(MitmConstants.DEFAULT_KEYSTORE_TYPE, serverCert, rootCert.getCertificate(), "alias", "password");
        });

        long finish = System.currentTimeMillis();

        log.info("Finished performance test:\n\tRoot Cert Key Gen: {}\n\tRoot Cert Digest: {}\n\tServer Cert Key Gen: {}\n\tServer Cert Digest: {}",
                rootCertKeyGen, rootCertDigest, serverCertKeyGen, serverCertDigest);
        log.info("Assembled {} Key Stores in {}ms. Average time per Key Store: {}ms", ITERATIONS, finish - start, (finish - start) / ITERATIONS);
    }

    private static CertificateInfo createCertificateInfo(String hostname) {
        return new CertificateInfo().commonName(hostname).notBefore(Instant.now()).notAfter(Instant.now());
    }
}
