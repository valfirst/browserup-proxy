package com.browserup.harreader.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Date;

import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarLog;
import com.browserup.harreader.model.HarRequest;
import org.junit.Test;

import java.util.Optional;
import java.util.regex.Pattern;

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

        assertEquals("Expected to find all entries",
                HarLogFilter.findEntries(log, urlPattern).size(), entriesNumber);
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

        assertFalse("Expected to get empty entry",
                HarLogFilter.findMostRecentEntry(log, urlPattern).isPresent());
    }

    @Test
    public void testFindEntryReturnsMostRecentEntryFilteredByUrl() {
        String url = "http://abc.com";
        Date firstDate = Date.from(Instant.ofEpochSecond(1000));
        Date secondDate = Date.from(Instant.ofEpochSecond(2000));

        HarLog log = createHarLog(url, firstDate, secondDate);

        Optional<HarEntry> entry = HarLogFilter.findMostRecentEntry(log, Pattern.compile("^http://abc\\.com?"));
        assertTrue("Expected to find entry", entry.isPresent());
        assertEquals("Expected to find the most recent entry",
                entry.get().getStartedDateTime(), secondDate);
    }

    @Test
    public void testFindEntryReturnsMostRecentEntry() {
        String url = "http://abc.com";
        Date firstDate = Date.from(Instant.ofEpochSecond(1000));
        Date secondDate = Date.from(Instant.ofEpochSecond(2000));

        HarLog log = createHarLog(url, firstDate, secondDate);

        Optional<HarEntry> entry = HarLogFilter.findMostRecentEntry(log);
        assertTrue("Expected to find entry", entry.isPresent());
        assertEquals("Expected to find the most recent entry",
                entry.get().getStartedDateTime(), secondDate);
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
