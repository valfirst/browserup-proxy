package com.browserup.bup.mitm;

import com.browserup.bup.mitm.test.util.CertificateTestUtil;
import com.browserup.bup.mitm.keys.RSAKeyGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RootCertificateGeneratorTest {
    @TempDir
    Path tmpDir;

    private final CertificateInfo certificateInfo = new CertificateInfo()
            .commonName("littleproxy-test")
            .notAfter(Instant.now())
            .notBefore(Instant.now());

    @Test
    void testGenerateRootCertificate() {
        RootCertificateGenerator generator = RootCertificateGenerator.builder()
                .certificateInfo(certificateInfo)
                .keyGenerator(new RSAKeyGenerator())
                .messageDigest("SHA256")
                .build();

        CertificateAndKey certificateAndKey = generator.load();

        CertificateTestUtil.verifyTestRSACertWithCN(certificateAndKey);

        CertificateAndKey secondLoad = generator.load();

        assertEquals(certificateAndKey, secondLoad, "Expected RootCertificateGenerator to return the same instance between calls to .load()");
    }

    @Test
    void testCanUseDefaultValues() {
        RootCertificateGenerator generator = RootCertificateGenerator.builder().build();

        CertificateAndKey certificateAndKey = generator.load();

        assertNotNull(certificateAndKey);
    }

    @Test
    void testCanSaveAsPKCS12File() throws IOException {
        RootCertificateGenerator generator = RootCertificateGenerator.builder().build();

        File file = Files.createTempFile(tmpDir, "test", null).toFile();

        generator.saveRootCertificateAndKey("PKCS12", file, "privateKey", "password");

        // trivial verification that something was written to the file
        assertThat("Expected file to be >0 bytes after writing certificate and private key", file.length(), greaterThan(0L));
    }

    @Test
    void testCanSaveAsJKSFile() throws IOException {
        RootCertificateGenerator generator = RootCertificateGenerator.builder().build();

        File file = Files.createTempFile(tmpDir, "test", null).toFile();

        generator.saveRootCertificateAndKey("JKS", file, "privateKey", "password");

        // trivial verification that something was written to the file
        assertThat("Expected file to be >0 bytes after writing certificate and private key", file.length(), greaterThan(0L));
    }

    @Test
    void testCanEncodeAsPem() {
        RootCertificateGenerator generator = RootCertificateGenerator.builder().build();

        String pemEncodedPrivateKey = generator.encodePrivateKeyAsPem("password");

        // trivial verification that something was written to the string
        assertThat("Expected string containing PEM-encoded private key to contain characters", pemEncodedPrivateKey, not(is(emptyOrNullString())));

        String pemEncodedCertificate = generator.encodeRootCertificateAsPem();
        assertThat("Expected string containing PEM-encoded certificate to contain characters", pemEncodedCertificate, not(is(emptyOrNullString())));
    }

}
