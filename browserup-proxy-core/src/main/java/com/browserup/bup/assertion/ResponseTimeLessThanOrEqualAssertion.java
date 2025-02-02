package com.browserup.bup.assertion;

import com.browserup.bup.assertion.error.HarEntryAssertionError;

import java.util.Optional;

import de.sstoehr.harreader.model.HarEntry;

public class ResponseTimeLessThanOrEqualAssertion implements HarEntryAssertion {
    private final Long time;

    public ResponseTimeLessThanOrEqualAssertion(Long time) {
        this.time = time;
    }

    @Override
    public Optional<HarEntryAssertionError> assertion(HarEntry entry) {
        if (entry.getTime() > time) {
            return Optional.of(new HarEntryAssertionError("Time exceeded", time, entry.getTime()));
        }
        return Optional.empty();
    }
}
