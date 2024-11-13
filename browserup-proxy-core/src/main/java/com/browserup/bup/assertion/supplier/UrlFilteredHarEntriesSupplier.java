package com.browserup.bup.assertion.supplier;

import com.browserup.bup.assertion.model.filter.AssertionUrlFilterInfo;
import com.browserup.harreader.filter.HarLogFilter;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;

import java.util.List;
import java.util.regex.Pattern;

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
