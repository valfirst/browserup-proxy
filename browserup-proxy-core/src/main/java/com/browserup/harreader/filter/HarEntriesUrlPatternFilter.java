package com.browserup.harreader.filter;

import java.util.regex.Pattern;

import de.sstoehr.harreader.model.HarEntry;

public class HarEntriesUrlPatternFilter implements HarEntriesFilter {

    private final Pattern pattern;

    public HarEntriesUrlPatternFilter(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean test(HarEntry entry) {
        return pattern.matcher(entry.getRequest().getUrl()).matches();
    }
}
