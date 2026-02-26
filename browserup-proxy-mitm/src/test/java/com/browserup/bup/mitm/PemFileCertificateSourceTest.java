package com.browserup.bup.mitm;

import com.browserup.bup.mitm.exception.ImportException;
import com.browserup.bup.mitm.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.browserup.bup.mitm.test.util.CertificateTestUtil.verifyTestRSACertWithCNandO;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PemFileCertificateSourceTest {

    @TempDir
    Path tmpDir;

    private File certificateFile;
    private File encryptedPrivateKeyFile;
    private File unencryptedPrivateKeyFile;

    @BeforeEach
    void stageFiles() throws IOException {
        certificateFile = Files.createFile(tmpDir.resolve("certificate.crt")).toFile();
        encryptedPrivateKeyFile = Files.createFile(tmpDir.resolve("encrypted-private-key.key")).toFile();
        unencryptedPrivateKeyFile = Files.createFile(tmpDir.resolve("unencrypted-private-key.key")).toFile();

        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/certificate.crt"), certificateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/encrypted-private-key.key"), encryptedPrivateKeyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(KeyStoreFileCertificateSourceTest.class.getResourceAsStream("/com/browserup/bup/mitm/unencrypted-private-key.key"), unencryptedPrivateKeyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void testCanLoadCertificateAndPasswordProtectedKey() {
        assumeTrue(EncryptionUtil.isUnlimitedStrengthAllowed(), "Skipping test because unlimited strength cryptography is not available");

        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, encryptedPrivateKeyFile, "password");

        CertificateAndKey certificateAndKey = pemFileCertificateSource.load();
        assertNotNull(certificateAndKey);

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    void testCanLoadCertificateAndUnencryptedKey() {
        PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, unencryptedPrivateKeyFile, null);

        CertificateAndKey certificateAndKey = pemFileCertificateSource.load();
        assertNotNull(certificateAndKey);

        verifyTestRSACertWithCNandO(certificateAndKey);
    }

    @Test
    void testCannotLoadEncryptedKeyWithoutPassword() {
        assertThrows(ImportException.class, () -> {
            PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, encryptedPrivateKeyFile, "wrongpassword");
            pemFileCertificateSource.load();
        });
    }

    @Test
    void testIncorrectCertificateFile() {
        assertThrows(ImportException.class, () -> {
            PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(new File("does-not-exist.crt"), encryptedPrivateKeyFile, "password");
            pemFileCertificateSource.load();
        });
    }

    @Test
    void testNullCertificateFile() {
        assertThrows(IllegalArgumentException.class, () -> {
            PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(null, encryptedPrivateKeyFile, "password");
            pemFileCertificateSource.load();
        });
    }

    @Test
    void testNullPrivateKeyFile() {
        assertThrows(IllegalArgumentException.class, () -> {
            PemFileCertificateSource pemFileCertificateSource = new PemFileCertificateSource(certificateFile, null, "password");
            pemFileCertificateSource.load();
        });
    }
}
