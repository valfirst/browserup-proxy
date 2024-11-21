package com.browserup.harreader.filter;

import java.util.function.Predicate;

import de.sstoehr.harreader.model.HarEntry;

public interface HarEntriesFilter extends Predicate<HarEntry> {

    @Override
    boolean test(HarEntry entry);
}
