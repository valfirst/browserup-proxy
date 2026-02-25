package com.browserup.bup.mitm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.browserup.bup.mitm.test.util.CertificateTestUtil.verifyTestRSACertWithCNandO;

class KeyStoreFileCertificateSourceTest {

    @TempDir
    Path tmpDir;

    private File pkcs12File;
    private File jksFile;

    @BeforeEach
    void stageFiles() throws IOException {
        pkcs12File = Files.createFile(tmpDir.resolve("keystore.p12")).toFile();
        jksFile = Files.createFile(tmpDir.resolve("keystore.jks")).toFile();

        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/keystore.p12"), pkcs12File.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/keystore.jks"), jksFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void testPkcs12FileOnClasspath() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("PKCS12", "/com/browserup/bup/mitm/keystore.p12", "privateKey", "password");

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load();

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    void testPkcs12FileOnDisk() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("PKCS12", pkcs12File, "privateKey", "password");

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load();

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    void testJksFileOnClasspath() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("JKS", "/com/browserup/bup/mitm/keystore.jks", "privateKey", "password");

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load();

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    void testJksFileOnDisk() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("JKS", jksFile, "privateKey", "password");

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load();

        verifyTestRSACertWithCNandO(certificateAndKey);
    }
}
