package com.browserup.bup.assertion.supplier;

import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarRequest;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;

public class MostRecentUrlFilteredHarEntrySupplierTest {
    @Test
    public void getMostRecentEntryByUrl() {
        int fromIndex = 1;
        int toIndex = 10;
        int mostRecentUrlIndex = toIndex;
        String urlPattern = "^http://abc" + mostRecentUrlIndex + "\\.com$";

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

        MostRecentUrlFilteredHarEntrySupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(har, Pattern.compile(urlPattern));
        List<HarEntry> result = supplier.get();

        assertThat("Expected to get one entry", result, Matchers.hasSize(1));
        Assert.assertEquals("", result.get(0).getRequest().getUrl(), "http://abc" + mostRecentUrlIndex + ".com");
    }

    @Test
    public void returnEmptyListIfNoEntryFoundByUrl() {
        String urlPattern = "^http://does_not_match\\.com?";
        HarEntry harEntry = new HarEntry();
        HarRequest harRequest = new HarRequest();
        String url = "http://abc.com";
        harRequest.setUrl(url);
        harEntry.setStartedDateTime(Date.from(Instant.ofEpochSecond(1000)));
        harEntry.setRequest(harRequest);
        List<HarEntry> harEntries = List.of(harEntry);
        Har har = new Har();
        har.getLog().setEntries(harEntries);

        MostRecentUrlFilteredHarEntrySupplier supplier = new MostRecentUrlFilteredHarEntrySupplier(har, Pattern.compile(urlPattern));
        List<HarEntry> result = supplier.get();

        Assert.assertNotNull("Expected to get empty list", result);
        assertThat("Expected to get no entries", result, Matchers.hasSize(0));
    }

}
