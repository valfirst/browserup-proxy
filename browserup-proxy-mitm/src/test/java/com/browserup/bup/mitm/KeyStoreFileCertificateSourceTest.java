package com.browserup.bup.mitm;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static com.browserup.bup.mitm.test.util.CertificateTestUtil.verifyTestRSACertWithCNandO;

public class KeyStoreFileCertificateSourceTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private File pkcs12File;
    private File jksFile;

    @Before
    public void stageFiles() throws IOException {
        pkcs12File = tmpDir.newFile("keystore.p12");
        jksFile = tmpDir.newFile("keystore.jks");

        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/keystore.p12"), pkcs12File.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/keystore.jks"), jksFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testPkcs12FileOnClasspath() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("PKCS12", "/com/browserup/bup/mitm/keystore.p12", "privateKey", "password");

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load();

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    public void testPkcs12FileOnDisk() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("PKCS12", pkcs12File, "privateKey", "password");

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load();

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    public void testJksFileOnClasspath() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("JKS", "/com/browserup/bup/mitm/keystore.jks", "privateKey", "password");

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load();

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    public void testJksFileOnDisk() {
        KeyStoreFileCertificateSource keyStoreFileCertificateSource = new KeyStoreFileCertificateSource("JKS", jksFile, "privateKey", "password");

        CertificateAndKey certificateAndKey = keyStoreFileCertificateSource.load();

        verifyTestRSACertWithCNandO(certificateAndKey);
    }
}
