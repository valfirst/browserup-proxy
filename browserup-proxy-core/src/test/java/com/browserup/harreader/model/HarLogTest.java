package com.browserup.harreader.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import de.sstoehr.harreader.model.HarPage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HarLogTest extends AbstractMapperTest<HarLog> {

    private static final String EXPECTED_DEFAULT_VERSION = "1.1";
    private static final List<HarPage> EXPECTED_PAGES_LIST = new ArrayList<>();
    private static final List<HarEntry> EXPECTED_ENTRIES_LIST = new ArrayList<>();

    @Test
    public void testVersion() {
        HarLog log = new HarLog();
        assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());

        log.setVersion("1.2");
        assertEquals("1.2", log.getVersion());

        log.setVersion(null);
        assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());

        log.setVersion("");
        assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());

        log.setVersion("  ");
        assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());
    }

    @Test
    public void testPages() {
        HarLog log = new HarLog();
        assertEquals(EXPECTED_PAGES_LIST, log.getPages());

        log.setPages(null);
        assertEquals(EXPECTED_PAGES_LIST, log.getPages());
    }

    @Test
    public void testEntries() {
        HarLog log = new HarLog();
        assertEquals(EXPECTED_ENTRIES_LIST, log.getEntries());

        log.setEntries(null);
        assertEquals(EXPECTED_ENTRIES_LIST, log.getEntries());
    }

    @Test
    public void testCreatorNull() {
        HarLog log = new HarLog();
        log.setCreator(null);
        assertNotNull(log.getCreator());
    }

    @Test
    public void testBrowserNull() {
        HarLog log = new HarLog();
        log.setBrowser(null);
        assertNull(log.getBrowser());
    }

    @Override
    public void testMapping() {
        HarLog log = map("{\"creator\": {}, \"browser\": {}, \"comment\": \"My comment\"}", HarLog.class);

        assertEquals(EXPECTED_DEFAULT_VERSION, log.getVersion());
        assertNotNull(log.getCreator());
        assertNotNull(log.getBrowser());
        assertEquals(EXPECTED_PAGES_LIST, log.getPages());
        assertEquals(EXPECTED_ENTRIES_LIST, log.getEntries());
        assertEquals("My comment", log.getComment());

        log = map(UNKNOWN_PROPERTY, HarLog.class);
        assertNotNull(log);
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
