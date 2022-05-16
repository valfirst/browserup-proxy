package com.browserup.bup.assertion.supplier

import com.browserup.harreader.model.Har
import com.browserup.harreader.model.HarEntry
import com.browserup.harreader.model.HarRequest
import org.hamcrest.Matchers
import org.junit.Test

import java.time.Instant
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class MostRecentUrlFilteredHarEntrySupplierTest {

    @Test
    void getMostRecentEntryByUrl() {
        def har = new Har()
        def harEntries = [] as List<HarEntry>
        def fromIndex = 1
        def toIndex = 10
        def mostRecentUrlIndex = toIndex
        def urlPattern = "^http://abc${mostRecentUrlIndex}\\.com?"

        (fromIndex..toIndex).each({ n ->
            def harEntry = new HarEntry()
            def harRequest = new HarRequest()
            def url = "http://abc${n}.com"
            harRequest.setUrl(url)
            harEntry.setTime(n)
            harEntry.setStartedDateTime(Date.from(Instant.ofEpochSecond(n)))
            harEntry.setRequest(harRequest)
            harEntries.push(harEntry)
        })
        har.getLog().setEntries(harEntries)

        def supplier = new MostRecentUrlFilteredHarEntrySupplier(har, Pattern.compile(urlPattern))
        def result = supplier.get()

        assertThat("Expected to get one entry", result, Matchers.hasSize(1))
        assertEquals("", result.get(0).request.url, "http://abc${mostRecentUrlIndex}.com".toString())
    }

    @Test
    void returnEmptyListIfNoEntryFoundByUrl() {
        def har = new Har()
        def harEntries = [] as List<HarEntry>
        def urlPattern = "^http://does_not_match\\.com?"
        def harEntry = new HarEntry()
        def harRequest = new HarRequest()
        def url = "http://abc.com"
        harRequest.setUrl(url)
        harEntry.setStartedDateTime(Date.from(Instant.ofEpochSecond(1000)))
        harEntry.setRequest(harRequest)
        harEntries.push(harEntry)
        har.getLog().setEntries(harEntries)

        def supplier = new MostRecentUrlFilteredHarEntrySupplier(har, Pattern.compile(urlPattern))
        def result = supplier.get()

        assertNotNull("Expected to get empty list", result)
        assertThat("Expected to get no entries", result, Matchers.hasSize(0))
    }
}