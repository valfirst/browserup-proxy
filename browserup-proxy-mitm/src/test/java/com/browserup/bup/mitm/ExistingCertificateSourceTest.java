package com.browserup.bup.mitm;

import org.junit.Test;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ExistingCertificateSourceTest {

    private final X509Certificate mockCertificate = mock(X509Certificate.class);
    private final PrivateKey mockPrivateKey = mock(PrivateKey.class);

    @Test
    public void testLoadExistingCertificateAndKey() {
        ExistingCertificateSource certificateSource = new ExistingCertificateSource(mockCertificate, mockPrivateKey);
        CertificateAndKey certificateAndKey = certificateSource.load();

        assertEquals(mockCertificate, certificateAndKey.getCertificate());
        assertEquals(mockPrivateKey, certificateAndKey.getPrivateKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMustSupplyCertificate() {
        ExistingCertificateSource certificateSource = new ExistingCertificateSource(null, mockPrivateKey);
        certificateSource.load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMustSupplyPrivateKey() {
        ExistingCertificateSource certificateSource = new ExistingCertificateSource(mockCertificate, null);
        certificateSource.load();
    }
}
