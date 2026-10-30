package com.browserup.bup.proxy.dns;

import com.google.common.collect.ImmutableList;
import com.browserup.bup.proxy.test.util.NewProxyServerTestUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@Disabled
class AdvancedHostResolverCacheTest {
    private static final Logger log = LoggerFactory.getLogger(AdvancedHostResolverCacheTest.class);

    static Stream<AdvancedHostResolver> resolvers() {
        return Stream.of(
                // skip DNS cache operations for NativeResolver
                new NativeCacheManipulatingResolver(),
                new ChainedHostResolver(ImmutableList.of(new NativeCacheManipulatingResolver()))
        );
    }

    private void skipForTravisCi() {
        // skip these tests on the CI server since the DNS lookup is extremely fast, even when cached
        assumeFalse("true".equals(System.getenv("TRAVIS")));
    }

    private void skipOnWindows() {
        // DNS cache-manipulating features are not available on Windows, because the NativeCacheManipulatingResolver does
        // not work, since Java seems to use to the OS-level cache.
        assumeFalse(NewProxyServerTestUtil.isWindows(),
                "NativeCacheManipulatingResolver does not support cache manipulation on Windows");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testCanClearDNSCache(AdvancedHostResolver resolver) {
        skipForTravisCi();
        skipOnWindows();

        // populate the cache
        resolver.resolve("www.msn.com");

        resolver.clearDNSCache();

        long start = System.nanoTime();
        resolver.resolve("www.msn.com");
        long finish = System.nanoTime();

        assertNotEquals(0, finish - start, "Expected non-zero DNS lookup time for www.msn.com after clearing DNS cache");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testCachedPositiveLookup(AdvancedHostResolver resolver) {
        skipForTravisCi();
        skipOnWindows();

        long start = System.nanoTime();
        // must use an address that we haven't already resolved in another test
        resolver.clearDNSCache();
        resolver.resolve("news.bing.com");
        long finish = System.nanoTime();

        long uncachedLookupNs = finish - start;

        assertNotEquals(0, uncachedLookupNs, "Expected non-zero DNS lookup time for news.bing.com on first lookup");

        start = System.nanoTime();
        resolver.resolve("news.bing.com");
        finish = System.nanoTime();

        long cachedLookupNs = finish - start;

        assertTrue(cachedLookupNs <= uncachedLookupNs / 2, "Expected extremely fast DNS lookup time for news.bing.com on second (cached) lookup. Uncached: " + uncachedLookupNs + "ns; cached: " + cachedLookupNs + "ns.");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testCachedNegativeLookup(AdvancedHostResolver resolver) {
        skipForTravisCi();
        skipOnWindows();

        long start = System.nanoTime();
        resolver.resolve("fake.notarealaddress");
        long finish = System.nanoTime();

        long uncachedLookupNs = finish - start;

        assertNotEquals(0, uncachedLookupNs, "Expected non-zero DNS lookup time for fake.notarealaddress on first lookup");

        start = System.nanoTime();
        resolver.resolve("fake.notarealaddress");
        finish = System.nanoTime();

        long cachedLookupNs = finish - start;

        assertTrue(cachedLookupNs <= uncachedLookupNs / 2, "Expected extremely fast DNS lookup time for fake.notarealaddress on second (cached) lookup. Uncached: " + uncachedLookupNs + "ns; cached: " + cachedLookupNs + "ns.");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testSetPositiveCacheTtl(AdvancedHostResolver resolver) throws InterruptedException {
        skipForTravisCi();
        skipOnWindows();

        resolver.clearDNSCache();
        resolver.setPositiveDNSCacheTimeout(2, TimeUnit.SECONDS);

        // populate the cache
        Collection<InetAddress> addresses = resolver.resolve("www.msn.com");

        // make sure there are addresses, since this is a *positive* TTL test
        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, addresses.size(), "Expected to find addresses for www.msn.com");

        // wait for the cache to clear
        Thread.sleep(2500);

        long start = System.nanoTime();
        addresses = resolver.resolve("www.msn.com");
        long finish = System.nanoTime();

        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, addresses.size(), "Expected to find addresses for www.msn.com");

        assertNotEquals(0, finish - start, "Expected non-zero DNS lookup time for www.msn.com after setting positive cache TTL");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testSetNegativeCacheTtl(AdvancedHostResolver resolver) throws InterruptedException {
        skipForTravisCi();
        skipOnWindows();

        Random random = new Random();
        String fakeAddress = random.nextInt() + ".madeup.thisisafakeaddress";

        resolver.clearDNSCache();
        resolver.setNegativeDNSCacheTimeout(2, TimeUnit.SECONDS);

        // populate the cache
        Collection<InetAddress> addresses = resolver.resolve(fakeAddress);

        // make sure there are no addresses, since this is a *negative* TTL test
        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertEquals(0, addresses.size(), "Expected to find no addresses for " + fakeAddress);

        // wait for the cache to clear
        Thread.sleep(2500);

        long start = System.nanoTime();
        addresses = resolver.resolve(fakeAddress);
        long finish = System.nanoTime();

        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertEquals(0, addresses.size(), "Expected to find no addresses for " + fakeAddress);

        assertNotEquals(0, finish - start, "Expected non-zero DNS lookup time for " + fakeAddress + " after setting negative cache TTL");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testSetEternalNegativeCacheTtl(AdvancedHostResolver resolver) {
        skipForTravisCi();
        skipOnWindows();

        Random random = new Random();
        String fakeAddress = random.nextInt() + ".madeup.thisisafakeaddress";

        resolver.clearDNSCache();
        resolver.setNegativeDNSCacheTimeout(-1, TimeUnit.SECONDS);

        // populate the cache
        Collection<InetAddress> addresses = resolver.resolve(fakeAddress);

        // make sure there are no addresses, since this is a *negative* TTL test
        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertEquals(0, addresses.size(), "Expected to find no addresses for " + fakeAddress);

        long start = System.nanoTime();
        addresses = resolver.resolve(fakeAddress);
        long finish = System.nanoTime();

        long cachedLookupNs = finish - start;

        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertEquals(0, addresses.size(), "Expected to find no addresses for " + fakeAddress);

        assertTrue(cachedLookupNs <= TimeUnit.NANOSECONDS.convert(10, TimeUnit.MILLISECONDS), "Expected extremely fast DNS lookup time for " + fakeAddress + " after setting eternal negative cache TTL. Cached lookup time: " + cachedLookupNs + "ns.");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testSetEternalPositiveCacheTtl(AdvancedHostResolver resolver) {
        skipForTravisCi();
        skipOnWindows();

        resolver.clearDNSCache();
        resolver.setPositiveDNSCacheTimeout(-1, TimeUnit.SECONDS);

        log.info("Using resolver: {}", resolver.getClass().getSimpleName());

        // populate the cache
        long one = System.nanoTime();
        Collection<InetAddress> addresses = resolver.resolve("www.msn.com");
        long two = System.nanoTime();
        log.info("Time to resolve address without cache: {}ns", two - one);

        // make sure there are addresses, since this is a *positive* TTL test
        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, addresses.size(), "Expected to find addresses for www.msn.com");

        long start = System.nanoTime();
        addresses = resolver.resolve("www.msn.com");
        long finish = System.nanoTime();

        long cachedLookupNs = finish - start;

        log.info("Time to resolve address with cache: {}ns", cachedLookupNs);

        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, addresses.size(), "Expected to find addresses for www.msn.com");

        assertTrue(cachedLookupNs <= TimeUnit.NANOSECONDS.convert(10, TimeUnit.MILLISECONDS), "Expected extremely fast DNS lookup time for www.msn.com after setting eternal negative cache TTL. Cached lookup time: " + cachedLookupNs + "ns.");
    }
}
