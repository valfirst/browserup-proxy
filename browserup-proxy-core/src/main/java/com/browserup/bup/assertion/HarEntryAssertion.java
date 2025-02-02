package com.browserup.bup.assertion;

import com.browserup.bup.assertion.error.HarEntryAssertionError;

import java.util.Optional;

import de.sstoehr.harreader.model.HarEntry;

@FunctionalInterface
public interface HarEntryAssertion {

    Optional<HarEntryAssertionError> assertion(HarEntry entry);

}
