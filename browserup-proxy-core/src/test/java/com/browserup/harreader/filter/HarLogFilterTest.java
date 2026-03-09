package com.browserup.harreader.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.regex.Pattern;

import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarLog;
import de.sstoehr.harreader.model.HarRequest;

public class HarLogFilterTest {
    @Test
    public void testFindEntries() {
        Pattern urlPattern = Pattern.compile("http://abc\\.com\\?param=\\d?");
        HarLog log = new HarLog();
        int entriesNumber = 10;

        for (int i = 0; i < entriesNumber; i++) {
            HarEntry entry = new HarEntry();
            HarRequest request1 = new HarRequest();
            request1.setUrl("http://abc.com?param=" + i);
            entry.setRequest(request1);
            log.getEntries().add(entry);
        }

        assertEquals(HarLogFilter.findEntries(log, urlPattern).size(), entriesNumber, "Expected to find all entries");
    }

    @Test
    public void testFindEntryReturnsEmpty() {
        String url = "http://abc.com";
        Pattern urlPattern = Pattern.compile("^doesnotmatch?");

        HarLog log = new HarLog();

        HarEntry entry = new HarEntry();
        HarRequest req = new HarRequest();
        req.setUrl(url);
        entry.setRequest(req);

        log.getEntries().add(entry);

        assertFalse(HarLogFilter.findMostRecentEntry(log, urlPattern).isPresent(), "Expected to get empty entry");
    }

    @Test
    public void testFindEntryReturnsMostRecentEntryFilteredByUrl() {
        String url = "http://abc.com";
        Date firstDate = Date.from(Instant.ofEpochSecond(1000));
        Date secondDate = Date.from(Instant.ofEpochSecond(2000));

        HarLog log = createHarLog(url, firstDate, secondDate);

        Optional<HarEntry> entry = HarLogFilter.findMostRecentEntry(log, Pattern.compile("^http://abc\\.com?"));
        assertTrue(entry.isPresent(), "Expected to find entry");
        assertEquals(entry.get().getStartedDateTime(), secondDate, "Expected to find the most recent entry");
    }

    @Test
    public void testFindEntryReturnsMostRecentEntry() {
        String url = "http://abc.com";
        Date firstDate = Date.from(Instant.ofEpochSecond(1000));
        Date secondDate = Date.from(Instant.ofEpochSecond(2000));

        HarLog log = createHarLog(url, firstDate, secondDate);

        Optional<HarEntry> entry = HarLogFilter.findMostRecentEntry(log);
        assertTrue(entry.isPresent(), "Expected to find entry");
        assertEquals(entry.get().getStartedDateTime(), secondDate, "Expected to find the most recent entry");
    }

    private HarLog createHarLog(String url, Date firstDate, Date secondDate) {
        HarLog log = new HarLog();

        HarEntry entry1 = new HarEntry();
        HarRequest request1 = new HarRequest();
        request1.setUrl(url);
        entry1.setRequest(request1);
        entry1.setStartedDateTime(firstDate);

        HarEntry entry2 = new HarEntry();
        HarRequest request2 = new HarRequest();
        request2.setUrl(url);
        entry2.setRequest(request2);
        entry2.setStartedDateTime(secondDate);

        log.getEntries().add(entry1);
        log.getEntries().add(entry2);
        return log;
    }
}
