package com.browserup.bup.assertion.supplier;

import com.browserup.bup.assertion.model.filter.AssertionFilterInfo;
import com.browserup.harreader.model.Har;

import java.util.List;

import de.sstoehr.harreader.model.HarEntry;

public class CurrentStepHarEntriesSupplier extends HarEntriesSupplier {
    public CurrentStepHarEntriesSupplier(Har har) {
        super(har, new AssertionFilterInfo());
    }

    @Override
    public List<HarEntry> get() {
        return getHar().getLog().getEntries();
    }
}
