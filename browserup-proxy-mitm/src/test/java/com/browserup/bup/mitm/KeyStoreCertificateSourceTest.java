package com.browserup.bup.mitm;

import org.junit.Test;

import java.security.KeyStore;

import static org.mockito.Mockito.mock;

public class KeyStoreCertificateSourceTest {

    private final KeyStore mockKeyStore = mock(KeyStore.class);

    // the happy-path test cases are already covered implicitly as part of KeyStoreFileCertificateSourceTest, so just test negative cases

    @Test(expected = IllegalArgumentException.class)
    public void testMustSupplyKeystore() {
        KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(null, "privatekey", "password");
        keyStoreCertificateSource.load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMustSupplyPassword() {
        KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(mockKeyStore, "privatekey", null);
        keyStoreCertificateSource.load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMustSupplyPrivateKeyAlias() {
        KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(mockKeyStore, null, "password");
        keyStoreCertificateSource.load();
    }

}
