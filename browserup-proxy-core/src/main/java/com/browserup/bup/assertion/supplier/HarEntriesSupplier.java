package com.browserup.bup.assertion.supplier;

import com.browserup.bup.assertion.model.filter.AssertionFilterInfo;

import java.util.List;
import java.util.function.Supplier;

import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;

public abstract class HarEntriesSupplier implements Supplier<List<HarEntry>> {
    private final Har har;
    private final AssertionFilterInfo filterInfo;

    public HarEntriesSupplier(Har har, AssertionFilterInfo filterInfo) {
        this.har = har;
        this.filterInfo = filterInfo;
    }

    public Har getHar() {
        return har;
    }

    public AssertionFilterInfo getFilterInfo() {
        return filterInfo;
    }
}
