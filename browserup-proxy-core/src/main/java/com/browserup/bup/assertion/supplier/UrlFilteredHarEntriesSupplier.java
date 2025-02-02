package com.browserup.bup.assertion.supplier;

import com.browserup.bup.assertion.model.filter.AssertionUrlFilterInfo;
import com.browserup.harreader.filter.HarLogFilter;

import java.util.List;
import java.util.regex.Pattern;

import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;

public class UrlFilteredHarEntriesSupplier extends HarEntriesSupplier {
    private final Pattern pattern;

    public UrlFilteredHarEntriesSupplier(Har har, Pattern pattern) {
        super(har, new AssertionUrlFilterInfo(pattern.pattern()));
        this.pattern = pattern;
    }

    @Override
    public List<HarEntry> get() {
        return HarLogFilter.findEntries(getHar().getLog(), pattern);
    }

    public Pattern getPattern() {
        return pattern;
    }
}
