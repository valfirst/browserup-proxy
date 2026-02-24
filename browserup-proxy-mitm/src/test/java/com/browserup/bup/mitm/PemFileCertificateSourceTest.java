package com.browserup.bup.mitm;

import com.browserup.bup.mitm.exception.ImportException;
import com.browserup.bup.mitm.util.EncryptionUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static com.browserup.bup.mitm.test.util.CertificateTestUtil.verifyTestRSACertWithCNandO;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

public class PemFileCertificateSourceTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private File certificateFile;
    private File encryptedPrivateKeyFile;
    private File unencryptedPrivateKeyFile;

    @Before
    public void stageFiles() throws IOException {
        certificateFile = tmpDir.newFile("certificate.crt");
        encryptedPrivateKeyFile = tmpDir.newFile("encrypted-private-key.key");
        unencryptedPrivateKeyFile = tmpDir.newFile("unencrypted-private-key.key");

        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/certificate.crt"), certificateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/encrypted-private-key.key"), encryptedPrivateKeyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/unencrypted-private-key.key"), unencryptedPrivateKeyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testCanLoadCertificateAndPasswordProtectedKey() {
        assumeTrue("Skipping test because unlimited strength cryptography is not available", EncryptionUtil.isUnlimitedStrengthAllowed());

        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, encryptedPrivateKeyFile, "password");

        CertificateAndKey certificateAndKey = pemFileCertificateSource.load();
        assertNotNull(certificateAndKey);

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    public void testCanLoadCertificateAndUnencryptedKey() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, unencryptedPrivateKeyFile, null);

        CertificateAndKey certificateAndKey = pemFileCertificateSource.load();
        assertNotNull(certificateAndKey);

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test(expected = ImportException.class)
    public void testCannotLoadEncryptedKeyWithoutPassword() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, encryptedPrivateKeyFile, "wrongpassword");

        pemFileCertificateSource.load();
    }

    @Test(expected = ImportException.class)
    public void testIncorrectCertificateFile() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(new File("does-not-exist.crt"), encryptedPrivateKeyFile, "password");

        pemFileCertificateSource.load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCertificateFile() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(null, encryptedPrivateKeyFile, "password");

        pemFileCertificateSource.load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPrivateKeyFile() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, null, "password");

        pemFileCertificateSource.load();
    }
}
