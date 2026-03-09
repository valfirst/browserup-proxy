package com.browserup.bup.assertion;

import com.browserup.bup.assertion.error.HarEntryAssertionError;
import de.sstoehr.harreader.model.HarEntry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseTimeLessThanOrEqualAssertionTest {
    @Test
    void testAssertionFailsIfTimeExceeds() {
        long expectedTime = 500;
        ResponseTimeLessThanOrEqualAssertion assertion = new ResponseTimeLessThanOrEqualAssertion(expectedTime);
        HarEntry entry = new HarEntry();
        int time = 1000;
        entry.setTime(time);

        Optional<HarEntryAssertionError> result = assertion.assertion(entry);

        assertTrue(result.isPresent(), "Expected assertion to return error");
    }

    @Test
    void testAssertionDoesNotFailIfTimeDoesNotExceed() {
        long expectedTime = 2000;
        ResponseTimeLessThanOrEqualAssertion assertion = new ResponseTimeLessThanOrEqualAssertion(expectedTime);
        HarEntry entry = new HarEntry();
        int time = 1000;
        entry.setTime(time);

        Optional<HarEntryAssertionError> result = assertion.assertion(entry);

        assertFalse(result.isPresent(), "Expected assertion not to return error");
    }

}
