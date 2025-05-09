package com.browserup.bup.assertion.supplier;

import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarRequest;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class UrlFilteredHarEntrySupplierTest {
    @Test
    public void getFilteredByUrlEntries() {
        int fromIndex = 1;
        int maxIndexToBeFiltered = 5;
        int toIndex = 10;
        final Pattern urlPattern = Pattern.compile("^http://abc[" + fromIndex + "-" + maxIndexToBeFiltered + "]\\.com$");

        List<HarEntry> harEntries = IntStream.range(fromIndex, toIndex + 1).mapToObj(i -> {
            HarEntry harEntry = new HarEntry();
            HarRequest harRequest = new HarRequest();
            harRequest.setUrl("http://abc" + i + ".com");
            harEntry.setTime(i);
            harEntry.setStartedDateTime(Date.from(Instant.ofEpochSecond(i)));
            harEntry.setRequest(harRequest);
            return harEntry;
        }).collect(Collectors.toList());
        Har har = new Har();
        har.getLog().setEntries(harEntries);

        UrlFilteredHarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(har, urlPattern);
        List<HarEntry> result = supplier.get();

        assertThat("Expected to get " + maxIndexToBeFiltered + " entries", result, Matchers.hasSize(maxIndexToBeFiltered));
        result.forEach(harEntry -> {
            assertTrue("Expected that found entry can be matched using url pattern", urlPattern.matcher(harEntry.getRequest().getUrl()).matches());
        });
    }

    @Test
    public void getEmptyEntriesIfNothingFoundByFilter() {
        Pattern urlPattern = Pattern.compile("^http://def\\.com$");

        List<HarEntry> harEntries = IntStream.range(1, 5).mapToObj(i -> {
            HarEntry harEntry = new HarEntry();
            HarRequest harRequest = new HarRequest();
            harRequest.setUrl("http://abc" + i + ".com");
            harEntry.setTime(i);
            harEntry.setStartedDateTime(Date.from(Instant.ofEpochSecond(i)));
            harEntry.setRequest(harRequest);
            return harEntry;
        }).collect(Collectors.toList());
        Har har = new Har();
        har.getLog().setEntries(harEntries);

        UrlFilteredHarEntriesSupplier supplier = new UrlFilteredHarEntriesSupplier(har, urlPattern);
        List<HarEntry> result = supplier.get();

        assertThat("Expected to get empty array", result, Matchers.hasSize(0));
    }

}
