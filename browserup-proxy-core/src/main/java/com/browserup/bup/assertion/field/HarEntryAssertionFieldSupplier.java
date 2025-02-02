package com.browserup.bup.assertion.field;

import java.util.function.Function;

import de.sstoehr.harreader.model.HarEntry;

@FunctionalInterface
public interface HarEntryAssertionFieldSupplier<FieldType> extends Function<HarEntry, FieldType> {
}
