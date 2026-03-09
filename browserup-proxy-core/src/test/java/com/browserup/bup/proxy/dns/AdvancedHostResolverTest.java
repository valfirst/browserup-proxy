package com.browserup.bup.proxy.dns;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Disabled
public class AdvancedHostResolverTest {

    static Stream<AdvancedHostResolver> resolvers() {
        return Stream.of(
                new NativeResolver(),
                new NativeCacheManipulatingResolver(),
                new ChainedHostResolver(ImmutableList.of(new NativeResolver(), new NativeCacheManipulatingResolver()))
        );
    }

    private boolean checkIpv6Enabled() {
        try {
            InetAddress[] addresses = InetAddress.getAllByName("::1");
            if (addresses != null) {
                return Arrays.stream(addresses).anyMatch(addr -> addr.getClass() == Inet6Address.class);
            }
        } catch (UnknownHostException e) {
            // IPv6 not available
        }
        return false;
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testResolveAddress(AdvancedHostResolver resolver) {
        Collection<InetAddress> yahooAddresses = resolver.resolve("www.yahoo.com");

        assertNotNull(yahooAddresses, "Collection of resolved addresses should never be null");

        assertNotEquals(0, yahooAddresses.size(), "Expected to find at least one address for www.yahoo.com");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testCannotResolveAddress(AdvancedHostResolver resolver) {
        Collection<InetAddress> noAddresses = resolver.resolve("www.notarealaddress.grenyarnia");

        assertNotNull(noAddresses, "Collection of resolved addresses should never be null");

        assertEquals(0, noAddresses.size(), "Expected to find no address for www.notarealaddress.grenyarnia");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testResolveIPv4AndIPv6Addresses(AdvancedHostResolver resolver) {
        assumeTrue(checkIpv6Enabled(), "Skipping test because IPv6 is not enabled");

        Collection<InetAddress> addresses = resolver.resolve("www.google.com");
        boolean foundIPv4 = addresses.stream().anyMatch(address -> address.getClass() == Inet4Address.class);

        assertTrue(foundIPv4, "Expected to find at least one IPv4 address for www.google.com");

        // disabling this assert to prevent test failures on systems without ipv6 access, or when the DNS server does not return IPv6 addresses
        //assertTrue(foundIPv6, "Expected to find at least one IPv6 address for www.google.com");

    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testResolveLocalhost(AdvancedHostResolver resolver) {
        Collection<InetAddress> addresses = resolver.resolve("localhost");

        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, addresses.size(), "Expected to find at least one address for localhost");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testResolveIPv4Address(AdvancedHostResolver resolver) {
        Collection<InetAddress> addresses = resolver.resolve("127.0.0.1");

        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, addresses.size(), "Expected to find at least one address for 127.0.0.1");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testResolveIPv6Address(AdvancedHostResolver resolver) {
        assumeTrue(checkIpv6Enabled(), "Skipping test because IPv6 is not enabled");

        Collection<InetAddress> addresses = resolver.resolve("::1");

        assertNotNull(addresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, addresses.size(), "Expected to find at least one address for ::1");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testResolveRemappedHost(AdvancedHostResolver resolver) {
        Collection<InetAddress> originalAddresses = resolver.resolve("www.google.com");

        assertNotNull(originalAddresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, originalAddresses.size(), "Expected to find at least one address for www.google.com");

        resolver.remapHost("www.google.com", "www.bing.com");

        Collection<InetAddress> remappedAddresses = resolver.resolve("www.google.com");
        assertNotNull(remappedAddresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, remappedAddresses.size(), "Expected to find at least one address for www.google.com remapped to www.bing.com");

        InetAddress firstRemappedAddr = remappedAddresses.iterator().next();

        //TODO: verify this is correct -- should remapping return the remapped hostname, or the original hostname but with an IP address corresponding to the remapped hostname?
        assertEquals("www.bing.com", firstRemappedAddr.getHostName(), "Expected hostname for returned address to reflect the remapped address.");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testReplaceRemappedHostWithNewRemapping(AdvancedHostResolver resolver) {
        // remap the hostname twice. the second remapping should supercede the first.
        resolver.remapHost("www.google.com", "www.yahoo.com");
        resolver.remapHost("www.google.com", "www.bing.com");

        Collection<InetAddress> remappedAddresses = resolver.resolve("www.google.com");
        assertNotNull(remappedAddresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, remappedAddresses.size(), "Expected to find at least one address for www.google.com remapped to www.bing.com");

        InetAddress firstRemappedAddr = remappedAddresses.iterator().next();

        //TODO: verify this is correct -- should remapping return the remapped hostname, or the original hostname but with an IP address corresponding to the remapped hostname?
        assertEquals("www.bing.com", firstRemappedAddr.getHostName(), "Expected hostname for returned address to reflect the remapped address.");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testRetrieveOriginalHostByRemappedHost(AdvancedHostResolver resolver) {
        resolver.remapHost("www.google.com", "www.bing.com");

        Collection<String> originalHostnames = resolver.getOriginalHostnames("www.bing.com");
        assertEquals(1, originalHostnames.size(), "Expected to find one original hostname after remapping");

        String original = originalHostnames.iterator().next();
        assertEquals("www.google.com", original, "Expected to find original hostname of www.google.com after remapping to www.bing.com");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testRemoveHostRemapping(AdvancedHostResolver resolver) {
        resolver.remapHost("www.google.com", "www.notarealaddress");

        Collection<InetAddress> remappedAddresses = resolver.resolve("www.google.com");
        assertEquals(0, remappedAddresses.size(), "Expected to find no address for remapped www.google.com");

        resolver.removeHostRemapping("www.google.com");

        Collection<InetAddress> regularAddress = resolver.resolve("www.google.com");
        assertNotNull(remappedAddresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, regularAddress.size(), "Expected to find at least one address for www.google.com after removing remapping");
    }

    @ParameterizedTest
    @MethodSource("resolvers")
    public void testClearHostRemappings(AdvancedHostResolver resolver) {
        resolver.remapHost("www.google.com", "www.notarealaddress");

        Collection<InetAddress> remappedAddresses = resolver.resolve("www.google.com");
        assertEquals(0, remappedAddresses.size(), "Expected to find no address for remapped www.google.com");

        resolver.clearHostRemappings();

        Collection<InetAddress> regularAddress = resolver.resolve("www.google.com");
        assertNotNull(remappedAddresses, "Collection of resolved addresses should never be null");
        assertNotEquals(0, regularAddress.size(), "Expected to find at least one address for www.google.com after removing remapping");
    }
}
