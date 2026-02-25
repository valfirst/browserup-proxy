package com.browserup.bup.mitm;

import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ExistingCertificateSourceTest {

    private final X509Certificate mockCertificate = mock(X509Certificate.class);
    private final PrivateKey mockPrivateKey = mock(PrivateKey.class);

    @Test
    void testLoadExistingCertificateAndKey() {
        ExistingCertificateSource certificateSource = new ExistingCertificateSource(mockCertificate, mockPrivateKey);
        CertificateAndKey certificateAndKey = certificateSource.load();

        assertEquals(mockCertificate, certificateAndKey.getCertificate());
        assertEquals(mockPrivateKey, certificateAndKey.getPrivateKey());
    }

    @Test
    void testMustSupplyCertificate() {
        assertThrows(IllegalArgumentException.class, () -> {
            ExistingCertificateSource certificateSource = new ExistingCertificateSource(null, mockPrivateKey);
            certificateSource.load();
        });
    }

    @Test
    void testMustSupplyPrivateKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            ExistingCertificateSource certificateSource = new ExistingCertificateSource(mockCertificate, null);
            certificateSource.load();
        });
    }
}
