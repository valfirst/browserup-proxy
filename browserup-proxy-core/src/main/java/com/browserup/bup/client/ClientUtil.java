package com.browserup.bup.client;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.proxy.dns.AdvancedHostResolver;
import com.browserup.bup.proxy.dns.NativeCacheManipulatingResolver;
import com.browserup.bup.proxy.dns.NativeResolver;
import org.openqa.selenium.Proxy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * A utility class with convenience methods for clients using BrowserUp Proxy in embedded mode.
 */
public class ClientUtil {
    /**
     * Creates a {@link NativeCacheManipulatingResolver} instance that can be used when
     * calling {@link BrowserUpProxy#setHostNameResolver(AdvancedHostResolver)}.
     *
     * @return a new NativeCacheManipulatingResolver
     */
    public static AdvancedHostResolver createNativeCacheManipulatingResolver() {
        return new NativeCacheManipulatingResolver();
    }

    /**
     * Creates a {@link NativeResolver} instance that <b>does not support cache manipulation</b> that can be used when
     * calling {@link BrowserUpProxy#setHostNameResolver(AdvancedHostResolver)}.
     *
     * @return a new NativeResolver
     */
    public static AdvancedHostResolver createNativeResolver() {
        return new NativeResolver();
    }

    /**
     * Creates a Selenium Proxy object from the BrowserUpProxy instance. The BrowserUpProxy must be started. Retrieves the address
     * of the Proxy using {@link #getConnectableAddress()}.
     *
     * @param browserUpProxy started BrowserUpProxy instance to read connection information from
     * @return a Selenium Proxy instance, configured to use the BrowserUpProxy instance as its proxy server
     * @throws IllegalStateException if the proxy has not been started.
     */
    public static Proxy createSeleniumProxy(BrowserUpProxy browserUpProxy) {
        return createSeleniumProxy(browserUpProxy, getConnectableAddress());
    }

    /**
     * Creates a Selenium Proxy object from the BrowserUpProxy instance, using the specified connectableAddress as the Selenium Proxy object's
     * proxy address. Determines the port using {@link BrowserUpProxy#getPort()}. The BrowserUpProxy must be started.
     *
     * @param browserUpProxy    started BrowserUpProxy instance to read the port from
     * @param connectableAddress the network address the Selenium Proxy will use to reach this BrowserUpProxy instance
     * @return a Selenium Proxy instance, configured to use the BrowserUpProxy instance as its proxy server
     * @throws IllegalStateException if the proxy has not been started.
     */
    public static Proxy createSeleniumProxy(BrowserUpProxy browserUpProxy, InetAddress connectableAddress) {
        return createSeleniumProxy(new InetSocketAddress(connectableAddress, browserUpProxy.getPort()));
    }

    /**
     * Creates a Selenium Proxy object from the BrowserUpProxy instance, using the specified hostnameOrAddress as the Selenium Proxy object's
     * proxy address. Determines the port using {@link BrowserUpProxy#getPort()}. The BrowserUpProxy must be started.
     *
     * @param browserUpProxy    started BrowserUpProxy instance to read the port from
     * @param hostnameOrAddress the hostnameOrAddress or the String form of the address the Selenium Proxy will use to
     *                          reach its proxy server.
     * @return a Selenium Proxy instance, configured to use the BrowserUpProxy instance as its proxy server
     * @throws IllegalStateException if the proxy has not been started.
     */
    public static Proxy createSeleniumProxy(BrowserUpProxy browserUpProxy, String hostnameOrAddress) {
        return createSeleniumProxy(hostnameOrAddress, browserUpProxy.getPort());
    }

    /**
     * Creates a Selenium Proxy object using the specified connectableAddressAndPort as the HTTP proxy server.
     *
     * @param connectableAddressAndPort the network address (or hostname) and port the Selenium Proxy will use to reach its
     *                                  proxy server (the InetSocketAddress may be unresolved).
     * @return a Selenium Proxy instance, configured to use the specified address and port as its proxy server
     */
    public static Proxy createSeleniumProxy(InetSocketAddress connectableAddressAndPort) {
        return createSeleniumProxy(connectableAddressAndPort.getHostString(), connectableAddressAndPort.getPort());
    }

    /**
     * Creates a Selenium Proxy object using the specified connectableAddressAndPort as the HTTP proxy server.
     *
     * @param hostnameOrAddress the hostnameOrAddress or the String form of the address the Selenium Proxy will use to
     *                          reach its proxy server.
     * @param port              the port the Selenium Proxy will use to reach its proxy server.
     * @return a Selenium Proxy instance, configured to use the specified address and port as its proxy server
     */
    public static Proxy createSeleniumProxy(String hostnameOrAddress, int port) {
        Proxy proxy = new Proxy();
        proxy.setProxyType(Proxy.ProxyType.MANUAL);

        String proxyStr = String.format("%s:%d", hostnameOrAddress, port);
        proxy.setHttpProxy(proxyStr);
        proxy.setSslProxy(proxyStr);

        return proxy;
    }

    /**
     * Attempts to retrieve a "connectable" address for this device that other devices on the network can use to connect to a local proxy.
     * This is a "reasonable guess" that is suitable in many (but not all) common scenarios.
     * TODO: define the algorithm used to discover a "connectable" local host
     *
     * @return a "reasonable guess" at an address that can be used by other machines on the network to reach this host
     */
    public static InetAddress getConnectableAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not resolve localhost", e);
        }
    }
}
