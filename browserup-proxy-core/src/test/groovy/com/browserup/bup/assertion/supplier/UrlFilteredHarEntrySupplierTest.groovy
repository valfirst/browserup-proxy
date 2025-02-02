package com.browserup.bup.assertion.supplier

import de.sstoehr.harreader.model.Har
import de.sstoehr.harreader.model.HarEntry
import de.sstoehr.harreader.model.HarRequest
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test

import java.time.Instant
import java.util.regex.Pattern

import static org.hamcrest.MatcherAssert.assertThat

class UrlFilteredHarEntrySupplierTest {

    @Test
    void getFilteredByUrlEntries() {
        def har = new Har()
        def harEntries = [] as List<HarEntry>
        def fromIndex = 1
        def maxIndexToBeFiltered = 5
        def toIndex = 10
        def urlPattern = Pattern.compile("^http://abc[${fromIndex}-${maxIndexToBeFiltered}]\\.com\$")

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

        def supplier = new UrlFilteredHarEntriesSupplier(har, urlPattern)
        def result = supplier.get()

        assertThat("Expected to get ${maxIndexToBeFiltered} entries", result, Matchers.hasSize(maxIndexToBeFiltered))
        result.each {
            Assert.assertTrue("Expected that found entry can be matched using url pattern",
                    urlPattern.matcher(it.request.url).matches())
        }
    }

    @Test
    void getEmtpyEntriesIfNothingFoundByFilter() {
        def har = new Har()
        def harEntries = [] as List<HarEntry>
        def urlPattern = Pattern.compile("^http://def\\.com\$")

        (1..5).each({ n ->
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

        def supplier = new UrlFilteredHarEntriesSupplier(har, urlPattern)
        def result = supplier.get()

        assertThat("Expected to get empty array", result, Matchers.hasSize(0))
    }
}