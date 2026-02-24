package com.browserup.bup.mitm.tools;

import com.browserup.bup.mitm.keys.ECKeyGenerator;
import org.junit.Test;

import java.security.KeyPair;

import static org.junit.Assert.assertNotNull;

public class ECKeyGeneratorTest {
    @Test
    public void testGenerateWithDefaults() {
        ECKeyGenerator keyGenerator = new ECKeyGenerator();
        KeyPair keyPair = keyGenerator.generate();

        assertNotNull(keyPair);
    }

    @Test
    public void testGenerateWithExplicitNamedCurve() {
        ECKeyGenerator keyGenerator = new ECKeyGenerator("secp384r1");
        KeyPair keyPair = keyGenerator.generate();

        assertNotNull(keyPair);
        // not much else to verify, other than successful generation
    }
}
