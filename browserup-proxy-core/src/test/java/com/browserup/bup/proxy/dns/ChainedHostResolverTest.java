package com.browserup.bup.proxy.dns;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.browserup.bup.proxy.test.util.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled
class ChainedHostResolverTest {
    @Test
    void testEmptyResolver() {
        ChainedHostResolver resolver = new ChainedHostResolver(null);

        Collection<InetAddress> results = resolver.resolve("www.google.com");

        assertNotNull(results, "Resolver should not return null results");
        assertThat("Empty resolver chain should return empty results", results, empty());

        Map<String, String> remappings = resolver.getHostRemappings();
        assertTrue(remappings.isEmpty(), "Empty resolver chain should return empty results");

        // make sure no exception is thrown when attempting write operations on an empty resolver
        resolver.setNegativeDNSCacheTimeout(1000, TimeUnit.DAYS);
        resolver.setPositiveDNSCacheTimeout(1000, TimeUnit.DAYS);
        resolver.clearDNSCache();
        resolver.remapHost("", "");
        resolver.remapHosts(ImmutableMap.of("", ""));
        resolver.removeHostRemapping("");
        resolver.clearHostRemappings();
    }

    @Test
    void testResolveReturnsFirstResults() {
        AdvancedHostResolver firstResolver = mock(AdvancedHostResolver.class);
        AdvancedHostResolver secondResolver = mock(AdvancedHostResolver.class);
        ChainedHostResolver chainResolver = new ChainedHostResolver(ImmutableList.of(firstResolver, secondResolver));

        when(firstResolver.resolve("1.1.1.1")).thenReturn(TestConstants.addressOnesList);
        when(secondResolver.resolve("1.1.1.1")).thenReturn(Collections.emptyList());

        Collection<InetAddress> results = chainResolver.resolve("1.1.1.1");
        assertNotNull(results, "Resolver should not return null results");
        assertThat("Expected resolver to return a result", results, not(empty()));
        Assertions.assertEquals(TestConstants.addressOnes, Iterables.get(results, 0), "Resolver returned unexpected result");

        verify(secondResolver, never()).resolve("1.1.1.1");

        reset(firstResolver);
        reset(secondResolver);

        when(firstResolver.resolve("2.2.2.2")).thenReturn(Collections.emptyList());
        when(secondResolver.resolve("2.2.2.2")).thenReturn(TestConstants.addressTwosList);

        results = chainResolver.resolve("2.2.2.2");
        assertNotNull(results, "Resolver should not return null results");
        assertThat("Expected resolver to return a result", results, not(empty()));
        Assertions.assertEquals(TestConstants.addressTwos, Iterables.get(results, 0), "Resolver returned unexpected result");

        verify(firstResolver).resolve("2.2.2.2");
        verify(secondResolver).resolve("2.2.2.2");
    }

    @Test
    void testGetterUsesFirstResolver() {
        AdvancedHostResolver firstResolver = mock(AdvancedHostResolver.class);
        AdvancedHostResolver secondResolver = mock(AdvancedHostResolver.class);
        ChainedHostResolver chainResolver = new ChainedHostResolver(ImmutableList.of(firstResolver, secondResolver));

        when(firstResolver.getOriginalHostnames("one")).thenReturn(ImmutableList.of("originalOne"));

        Collection<String> results = chainResolver.getOriginalHostnames("one");
        assertNotNull(results, "Resolver should not return null results");
        assertThat("Expected resolver to return a result", results, not(empty()));
        assertEquals("originalOne", Iterables.get(results, 0), "Resolver returned unexpected result");

        verify(secondResolver, never()).getOriginalHostnames(any(String.class));
    }

    @Test
    void testResolveWaitsForWriteOperation() throws InterruptedException {
        AdvancedHostResolver firstResolver = mock(AdvancedHostResolver.class);
        AdvancedHostResolver secondResolver = mock(AdvancedHostResolver.class);
        final ChainedHostResolver chainResolver = new ChainedHostResolver(ImmutableList.of(firstResolver, secondResolver));

        final AtomicBoolean secondResolverClearingCache = new AtomicBoolean(false);
        // track the time when the second resolver finishes clearing the cache, so we can verify the simultaneous resolve in the
        // first resolver starts AFTER the cache clear completes
        final AtomicLong secondResolverCacheClearFinishedTime = new AtomicLong(0);

        // set up the second resolver to sleep for a few seconds when a clearDNSCache() call is made
        doAnswer((Answer<Void>) invocationOnMock -> {
            secondResolverClearingCache.set(true);

            Thread.sleep(4000);

            secondResolverCacheClearFinishedTime.set(System.nanoTime());

            return null;
        }).when(secondResolver).clearDNSCache();

        // track the time the first resolver starts resolving the address, to make sure it is AFTER the DNS cache clear time
        final AtomicLong firstResolverStartedResolvingTime = new AtomicLong(0);

        // set up the first resolver to capture the time it starts resolving the address
        when(firstResolver.resolve("1.1.1.1")).then((Answer<Collection<InetAddress>>) invocationOnMock -> {
            firstResolverStartedResolvingTime.set(System.nanoTime());
            return TestConstants.addressOnesList;
        });

        // run the DNS cache clear in a separate thread, so it will be running (and sleeping) when we test the resolve() method
        new Thread(chainResolver::clearDNSCache).start();

        // wait for the clearDNSCache() call to start executing
        Thread.sleep(1000);

        // make sure the second resolver has started clearing the cache before invoking resolve on the chained resolver, to make sure
        // that this call to resolve will result in a wait
        assertTrue(secondResolverClearingCache.get(), "Expected resolver to already be clearing the DNS cache in a separate thread");
        assertEquals(0, secondResolverCacheClearFinishedTime.get(), "Did not expect resolver to already be finished clearing the DNS cache");

        Collection<InetAddress> results = chainResolver.resolve("1.1.1.1");

        assertNotNull(results, "Resolver should not return null results");
        assertThat("Expected resolver to return a result", results, not(empty()));
        Assertions.assertEquals(TestConstants.addressOnes, Iterables.get(results, 0), "Resolver returned unexpected result");

        assertThat("Expected resolver to be finished clearing DNS cache", secondResolverCacheClearFinishedTime.get(), greaterThan(0L));

        assertThat("Expected resolver to finish clearing the DNS cache before starting to resolve an address", firstResolverStartedResolvingTime.get(), greaterThan(secondResolverCacheClearFinishedTime.get()));
    }
}
