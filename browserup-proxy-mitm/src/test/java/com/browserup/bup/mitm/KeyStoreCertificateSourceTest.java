package com.browserup.bup.mitm;

import org.junit.jupiter.api.Test;

import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class KeyStoreCertificateSourceTest {

    private final KeyStore mockKeyStore = mock(KeyStore.class);

    // the happy-path test cases are already covered implicitly as part of KeyStoreFileCertificateSourceTest, so just test negative cases

    @Test
    void testMustSupplyKeystore() {
        assertThrows(IllegalArgumentException.class, () -> {
            KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(null, "privatekey", "password");
            keyStoreCertificateSource.load();
        });
    }

    @Test
    void testMustSupplyPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(mockKeyStore, "privatekey", null);
            keyStoreCertificateSource.load();
        });
    }

    @Test
    void testMustSupplyPrivateKeyAlias() {
        assertThrows(IllegalArgumentException.class, () -> {
            KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(mockKeyStore, null, "password");
            keyStoreCertificateSource.load();
        });
    }

}
