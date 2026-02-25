package com.browserup.bup.mitm.tools;

import com.browserup.bup.mitm.keys.ECKeyGenerator;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ECKeyGeneratorTest {
    @Test
    void testGenerateWithDefaults() {
        ECKeyGenerator keyGenerator = new ECKeyGenerator();
        KeyPair keyPair = keyGenerator.generate();

        assertNotNull(keyPair);
    }

    @Test
    void testGenerateWithExplicitNamedCurve() {
        ECKeyGenerator keyGenerator = new ECKeyGenerator("secp384r1");
        KeyPair keyPair = keyGenerator.generate();

        assertNotNull(keyPair);
        // not much else to verify, other than successful generation
    }
}
