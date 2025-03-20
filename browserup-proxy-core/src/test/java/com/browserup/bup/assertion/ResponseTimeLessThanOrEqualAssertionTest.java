package com.browserup.bup.assertion;

import com.browserup.bup.assertion.error.HarEntryAssertionError;
import de.sstoehr.harreader.model.HarEntry;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResponseTimeLessThanOrEqualAssertionTest {
    @Test
    public void testAssertionFailsIfTimeExceeds() {
        long expectedTime = 500;
        ResponseTimeLessThanOrEqualAssertion assertion = new ResponseTimeLessThanOrEqualAssertion(expectedTime);
        HarEntry entry = new HarEntry();
        int time = 1000;
        entry.setTime(time);

        Optional<HarEntryAssertionError> result = assertion.assertion(entry);

        assertTrue("Expected assertion to return error", result.isPresent());
    }

    @Test
    public void testAssertionDoesNotFailIfTimeDoesNotExceed() {
        long expectedTime = 2000;
        ResponseTimeLessThanOrEqualAssertion assertion = new ResponseTimeLessThanOrEqualAssertion(expectedTime);
        HarEntry entry = new HarEntry();
        int time = 1000;
        entry.setTime(time);

        Optional<HarEntryAssertionError> result = assertion.assertion(entry);

        assertFalse("Expected assertion not to return error", result.isPresent());
    }

}
