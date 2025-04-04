package com.browserup.bup;

import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.filters.RequestFilter;
import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.mitm.TrustSource;
import com.browserup.bup.proxy.BlocklistEntry;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.auth.AuthType;
import com.browserup.bup.proxy.dns.AdvancedHostResolver;
import com.browserup.bup.util.HttpStatusClass;

import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarPageTiming;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.MitmManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public interface BrowserUpProxy {
    /**
     * Starts the proxy on port 0 (a JVM-selected open port). The proxy will bind the listener to the wildcard address (0:0:0:0 - all network interfaces).
     *
     * @throws java.lang.IllegalStateException if the proxy has already been started
     */
    void start();

    /**
     * Starts the proxy on the specified port. The proxy will bind the listener to the wildcard address (0:0:0:0 - all network interfaces).
     *
     * @param port port to listen on
     * @throws java.lang.IllegalStateException if the proxy has already been started
     */
    void start(int port);

    /**
     * Starts the proxy on the specified port. The proxy will listen for connections on the network interface specified by the bindAddress, and will
     * also initiate connections to upstream servers on the same network interface.
     *
     * @param port port to listen on
     * @param bindAddress address of the network interface on which the proxy will listen for connections and also attempt to connect to upstream servers.
     * @throws java.lang.IllegalStateException if the proxy has already been started
     */
    void start(int port, InetAddress bindAddress);

    /**
     * Starts the proxy on the specified port. The proxy will listen for connections on the network interface specified by the clientBindAddress, and will
     * initiate connections to upstream servers from the network interface specified by the serverBindAddress.
     *
     * @param port port to listen on
     * @param clientBindAddress address of the network interface on which the proxy will listen for connections
     * @param serverBindAddress address of the network interface on which the proxy will connect to upstream servers
     * @throws java.lang.IllegalStateException if the proxy has already been started
     */
    void start(int port, InetAddress clientBindAddress, InetAddress serverBindAddress);

    /**
     * Returns true if the proxy is started and listening for connections, otherwise false.
     * @return is proxy started
     */
    boolean isStarted();

    /**
     * Stops accepting new client connections and initiates a graceful shutdown of the proxy server, waiting up to 5 seconds for network
     * traffic to stop. If the proxy was previously stopped or aborted, this method has no effect.
     *
     * @throws java.lang.IllegalStateException if the proxy has not been started.
     */
    void stop();

    /**
     * Like {@link #stop()}, shuts down the proxy server and no longer accepts incoming connections, but does not wait for any existing
     * network traffic to cease. Any existing connections to clients or to servers may be force-killed immediately.
     * If the proxy was previously stopped or aborted, this method has no effect.
     *
     * @throws java.lang.IllegalStateException if the proxy has not been started
     */
    void abort();

    /**
     * Returns the address of the network interface on which the proxy is listening for client connections.
     *
     * @return the client bind address, or null if the proxy has not been started
     */
    InetAddress getClientBindAddress();

    /**
     * Returns the actual port on which the proxy is listening for client connections.
     *
     * @throws java.lang.IllegalStateException if the proxy has not been started
     * @return port
     */
    int getPort();

    /**
     * Returns the address address of the network interface the proxy will use to initiate upstream connections. If no server bind address
     * has been set, this method returns null, even if the proxy has been started.
     *
     * @return server bind address if one has been set, otherwise null
     */
    InetAddress getServerBindAddress();

    /**
     * Retrieves the current HAR.
     *
     * @return current HAR, or null if HAR capture is not enabled
     */
    default Har getHar() {
        return getHar(false);
    }

    /**
     * @param cleanHar reset/clear the in-memory har
     * If cleanHar is false - returns current HAR.
     * If cleanHar is true - cleans current HAR and returns HAR with data it has before cleaning.
     *
     * @return current HAR, or null if HAR capture is not enabled
     */
    Har getHar(boolean cleanHar);

    /**
     * Starts a new HAR file with the default page name (see {@link #newPage()}. Enables HAR capture if it was not previously enabled.
     *
     * @return existing HAR file, or null if none exists or HAR capture was disabled
     */
    Har newHar();

    /**
     * Starts a new HAR file with the specified initialPageRef as the page name and page title. Enables HAR capture if it was not previously enabled.
     *
     * @param initialPageRef initial page name of the new HAR file
     * @return existing HAR file, or null if none exists or HAR capture was disabled
     */
    Har newHar(String initialPageRef);

    /**
     * Starts a new HAR file with the specified page name and page title. Enables HAR capture if it was not previously enabled.
     *
     * @param initialPageRef initial page name of the new HAR file
     * @param initialPageTitle initial page title of the new HAR file
     * @return existing HAR file, or null if none exists or HAR capture was disabled
     */
    Har newHar(String initialPageRef, String initialPageTitle);

    /**
     * Sets the data types that will be captured in the HAR file for future requests. Replaces any existing capture types with the specified
     * capture types. A null or empty set will not disable HAR capture, but will disable collection of
     * additional {@link com.browserup.bup.proxy.CaptureType} data types. {@link com.browserup.bup.proxy.CaptureType} provides several
     * convenience methods to retrieve commonly-used capture settings.
     * <b>Note:</b> HAR capture must still be explicitly enabled via {@link #newHar()} or {@link #newHar(String)} to begin capturing
     * any request and response contents.
     *
     * @param captureTypes HAR data types to capture
     */
    void setHarCaptureTypes(Set<CaptureType> captureTypes);

    /**
     * Sets the data types that will be captured in the HAR file for future requests. Replaces any existing capture types with the specified
     * capture types. A null or empty set will not disable HAR capture, but will disable collection of
     * additional {@link com.browserup.bup.proxy.CaptureType} data types. {@link com.browserup.bup.proxy.CaptureType} provides several
     * convenience methods to retrieve commonly-used capture settings.
     * <b>Note:</b> HAR capture must still be explicitly enabled via {@link #newHar()} or {@link #newHar(String)} to begin capturing
     * any request and response contents.
     *
     * @param captureTypes HAR data types to capture
     */
    void setHarCaptureTypes(CaptureType... captureTypes);

    /**
     * @return A copy of HAR capture types currently in effect. The EnumSet cannot be used to modify the HAR capture types currently in effect.
     */
    EnumSet<CaptureType> getHarCaptureTypes();

    /**
     * Enables the specified HAR capture types. Does not replace or disable any other capture types that may already be enabled.
     *
     * @param captureTypes capture types to enable
     */
    void enableHarCaptureTypes(Set<CaptureType> captureTypes);

    /**
     * Enables the specified HAR capture types. Does not replace or disable any other capture types that may already be enabled.
     *
     * @param captureTypes capture types to enable
     */
    void enableHarCaptureTypes(CaptureType... captureTypes);

    /**
     * Disables the specified HAR capture types. Does not replace or disable any other capture types that may already be enabled.
     *
     * @param captureTypes capture types to disable
     */
    void disableHarCaptureTypes(Set<CaptureType> captureTypes);

    /**
     * Disables the specified HAR capture types. Does not replace or disable any other capture types that may already be enabled.
     *
     * @param captureTypes capture types to disable
     */
    void disableHarCaptureTypes(CaptureType... captureTypes);

    /**
     * Starts a new HAR page using the default page naming convention. The default page naming convention is "Page #", where "#" resets to 1
     * every time {@link #newHar()} or {@link #newHar(String)} is called, and increments on every subsequent call to {@link #newPage()} or
     * {@link #newHar(String)}. Populates the {@link HarPageTiming#getOnLoad()} value based on the amount of time
     * the current page has been captured.
     *
     * @return the HAR as it existed immediately after ending the current page
     * @throws java.lang.IllegalStateException if HAR capture has not been enabled via {@link #newHar()} or {@link #newHar(String)}
     */
    Har newPage();

    /**
     * Starts a new HAR page using the specified pageRef as the page name and the page title. Populates the
     * {@link HarPageTiming#getOnLoad()} value based on the amount of time the current page has been captured.
     *
     * @param pageRef name of the new page
     * @return the HAR as it existed immediately after ending the current page
     * @throws java.lang.IllegalStateException if HAR capture has not been enabled via {@link #newHar()} or {@link #newHar(String)}
     */
    Har newPage(String pageRef);

    /**
     * Starts a new HAR page using the specified pageRef as the page name and the pageTitle as the page title. Populates the
     * {@link HarPageTiming#getOnLoad()} value based on the amount of time the current page has been captured.
     *
     * @param pageRef name of the new page
     * @param pageTitle title of the new page
     * @return the HAR as it existed immediately after ending the current page
     * @throws java.lang.IllegalStateException if HAR capture has not been enabled via {@link #newHar()} or {@link #newHar(String)}
     */
    Har newPage(String pageRef, String pageTitle);

    /**
     * Stops capturing traffic in the HAR. Populates the {@link HarPageTiming#getOnLoad()} value for the current page
     * based on the amount of time it has been captured.
     *
     * @return the existing HAR
     */
    Har endHar();

    /**
     * Sets the maximum bandwidth to consume when reading server responses.
     *
     * @param bytesPerSecond maximum bandwidth, in bytes per second
     */
    void setReadBandwidthLimit(long bytesPerSecond);

    /**
     * Returns the current bandwidth limit for reading, in bytes per second.
     * @return ReadBandwidthLimit
     */
    long getReadBandwidthLimit();

    /**
     * Sets the maximum bandwidth to consume when sending requests to servers.
     *
     * @param bytesPerSecond maximum bandwidth, in bytes per second
     */
    void setWriteBandwidthLimit(long bytesPerSecond);

    /**
     * Returns the current bandwidth limit for writing, in bytes per second.
     * @return WriteBandwidthLimit
     */
    long getWriteBandwidthLimit();

    /**
     * The minimum amount of time that will elapse between the time the proxy begins receiving a response from the server and the time the
     * proxy begins sending the response to the client.
     *
     * @param latency minimum latency, or 0 for no minimum
     * @param timeUnit TimeUnit for the latency
     */
    void setLatency(long latency, TimeUnit timeUnit);

    /**
     * Maximum amount of time to wait to establish a connection to a remote server. If the connection has not been established within the
     * specified time, the proxy will respond with an HTTP 502 Bad Gateway. The default value is 60 seconds.
     *
     * @param connectionTimeout maximum time to wait to establish a connection to a server, or 0 to wait indefinitely
     * @param timeUnit TimeUnit for the connectionTimeout
     */
    void setConnectTimeout(int connectionTimeout, TimeUnit timeUnit);

    /**
     * Maximum amount of time to allow a connection to remain idle. A connection becomes idle when it has not received data from a server
     * within the the specified timeout. If the proxy has not yet begun to forward the response to the client, the proxy
     * will respond with an HTTP 504 Gateway Timeout. If the proxy has already started forwarding the response to the client, the
     * connection to the client <i>may</i> be closed abruptly. The default value is 60 seconds.
     *
     * @param idleConnectionTimeout maximum time to allow a connection to remain idle, or 0 to wait indefinitely.
     * @param timeUnit TimeUnit for the idleConnectionTimeout
     */
    void setIdleConnectionTimeout(int idleConnectionTimeout, TimeUnit timeUnit);

    /**
     * Maximum amount of time to wait for an HTTP response from the remote server after the request has been sent in its entirety. The HTTP
     * request must complete within the specified time. If the proxy has not yet begun to forward the response to the client, the proxy
     * will respond with an HTTP 504 Gateway Timeout. If the proxy has already started forwarding the response to the client, the
     * connection to the client <i>may</i> be closed abruptly. The default value is 0 (wait indefinitely).
     *
     * @param requestTimeout maximum time to wait for an HTTP response, or 0 to wait indefinitely
     * @param timeUnit TimeUnit for the requestTimeout
     */
    void setRequestTimeout(int requestTimeout, TimeUnit timeUnit);

    /**
     * Enables automatic authorization for the specified domain and auth type. Every request sent to the specified domain will contain the
     * specified authorization information.
     *
     * @param domain domain automatically send authorization information to
     * @param username authorization username
     * @param password authorization password
     * @param authType authorization type
     */
    void autoAuthorization(String domain, String username, String password, AuthType authType);

    /**
     * Stops automatic authorization for the specified domain.
     *
     * @param domain domain to stop automatically sending authorization information to
     */
    void stopAutoAuthorization(String domain);

    /**
     * Enables chained proxy authorization using the Proxy-Authorization header described in RFC 7235, section 4.4 (https://tools.ietf.org/html/rfc7235#section-4.4).
     * Currently, only {@link AuthType#BASIC} authentication is supported.
     *
     * @param username the username to use to authenticate with the chained proxy
     * @param password the password to use to authenticate with the chained proxy
     * @param authType the auth type to use (currently, must be BASIC)
     */
    void chainedProxyAuthorization(String username, String password, AuthType authType);

    /**
     * Adds a rewrite rule for the specified URL-matching regular expression. If there are any existing rewrite rules, the new rewrite
     * rule will be applied last, after all other rewrite rules are applied. The specified urlPattern will be replaced with the specified
     * replacement expression. The urlPattern is treated as a Java regular expression and must be properly escaped (see {@link java.util.regex.Pattern}).
     * The replacementExpression may consist of capture groups specified in the urlPattern, denoted
     * by a $ (see {@link java.util.regex.Matcher#appendReplacement(StringBuffer, String)}.
     * For HTTP requests (not HTTPS), if the hostname and/or port is changed as a result of a rewrite rule, the Host header of the request will be modified
     * to reflect the updated hostname/port. For HTTPS requests, the host and port cannot be changed by rewrite rules
     * (use {@link #getHostNameResolver()} and {@link AdvancedHostResolver#remapHost(String, String)} to direct HTTPS requests
     * to a different host).
     * <b>Note:</b> The rewriting applies to the entire URL, including scheme (http:// or https://), hostname/address, port, and query string. Note that this means
     * a urlPattern of {@code "http://www\.website\.com/page"} will NOT match {@code http://www.website.com:80/page}.
     * For example, the following rewrite rule:
     *
     * <pre>   {@code proxy.rewriteUrl("http://www\\.(yahoo|bing)\\.com/\\?(\\w+)=(\\w+)", "http://www.google.com/?originalDomain=$1&$2=$3");}</pre>
     *
     * will match an HTTP request (but <i>not</i> HTTPS!) to www.yahoo.com or www.bing.com with exactly 1 query parameter,
     * and replace it with a call to www.google.com with an 'originalDomain' query parameter, as well as the original query parameter.
     * When applied to the URL:
     * <pre>   {@code http://www.yahoo.com?theFirstParam=someValue}</pre>
     * will result in the proxy making a request to:
     * <pre>   {@code http://www.google.com?originalDomain=yahoo&theFirstParam=someValue}</pre>
     * When applied to the URL:
     * <pre>   {@code http://www.bing.com?anotherParam=anotherValue}</pre>
     * will result in the proxy making a request to:
     * <pre>   {@code http://www.google.com?originalDomain=bing&anotherParam=anotherValue}</pre>
     *
     * @param urlPattern URL-matching regular expression
     * @param replacementExpression an expression, which may optionally contain capture groups, which will replace any URL which matches urlPattern
     */
    void rewriteUrl(String urlPattern, String replacementExpression);

    /**
     * Replaces existing rewrite rules with the specified patterns and replacement expressions. The rules will be applied in the order
     * specified by the Map's iterator.
     * See {@link #rewriteUrl(String, String)} for details on the format of the rewrite rules.
     *
     * @param rewriteRules {@code Map<urlPattern, replacementExpression>}
     */
    void rewriteUrls(Map<String, String> rewriteRules);

    /**
     * Returns all rewrite rules currently in effect. Iterating over the returned Map is guaranteed to return rewrite rules
     * in the order in which the rules are actually applied.
     *
     * @return {@code Map<URL-matching regex, replacement expression>}
     */
    Map<String, String> getRewriteRules();

    /**
     * Removes an existing rewrite rule whose urlPattern matches the specified pattern.
     *
     * @param urlPattern rewrite rule pattern to remove
     */
    void removeRewriteRule(String urlPattern);

    /**
     * Clears all existing rewrite rules.
     */
    void clearRewriteRules();

    /**
     * Adds a URL-matching regular expression to the blocklist. Requests that match a blocklisted URL will return the specified HTTP
     * statusCode for all HTTP methods. If there are existing patterns on the blocklist, the urlPattern will be evaluated last,
     * after the URL is checked against all other blocklist entries.
     * The urlPattern matches the full URL of the request, including scheme, host, and port, path, and query parameters
     * for both HTTP and HTTPS requests. For example, to blocklist both HTTP and HTTPS requests to www.google.com,
     * use a urlPattern of "https?://www\\.google\\.com/.*".
     *
     * @param urlPattern URL-matching regular expression to blocklist
     * @param statusCode HTTP status code to return
     */
    void blocklistRequests(String urlPattern, int statusCode);

    /**
     * Adds a URL-matching regular expression to the blocklist. Requests that match a blocklisted URL will return the specified HTTP
     * statusCode only when the request's HTTP method (GET, POST, PUT, etc.) matches the specified httpMethodPattern regular expression.
     * If there are existing patterns on the blocklist, the urlPattern will be evaluated last, after the URL is checked against all
     * other blocklist entries.
     * See {@link #blocklistRequests(String, int)} for details on the URL the urlPattern will match.
     *
     * @param urlPattern URL-matching regular expression to blocklist
     * @param statusCode HTTP status code to return
     * @param httpMethodPattern regular expression matching a request's HTTP method
     */
    void blocklistRequests(String urlPattern, int statusCode, String httpMethodPattern);

    /**
     * Replaces any existing blocklist with the specified blocklist. URLs will be evaluated against the blocklist in the order
     * specified by the Collection's iterator.
     *
     * @param blocklist new blocklist entries
     */
    void setBlocklist(Collection<BlocklistEntry> blocklist);

    /**
     * Returns all blocklist entries currently in effect. Iterating over the returned Collection is guaranteed to return
     * blocklist entries in the order in which URLs are actually evaluated against the blocklist.
     *
     * @return blocklist entries, or an empty collection if none exist
     */
    Collection<BlocklistEntry> getBlocklist();

    /**
     * Clears any existing blocklist.
     */
    void clearBlocklist();

    /**
     * Allowlists URLs matching the specified regular expression patterns. Replaces any existing allowlist.
     * The urlPattern matches the full URL of the request, including scheme, host, and port, path, and query parameters
     * for both HTTP and HTTPS requests. For example, to allowlist both HTTP and HTTPS requests to www.google.com, use a urlPattern
     * of "https?://www\\.google\\.com/.*".
     * <b>Note:</b> All HTTP CONNECT requests are automatically allowlisted and cannot be short-circuited using the
     * allowlist response code.
     *
     * @param urlPatterns URL-matching regular expressions to allowlist; null or an empty collection will enable an empty allowlist
     * @param statusCode HTTP status code to return to clients when a URL matches a pattern
     */
    void allowlistRequests(Collection<String> urlPatterns, int statusCode);

    /**
     * Adds a URL-matching regular expression to an existing allowlist.
     *
     * @param urlPattern URL-matching regular expressions to allowlist
     * @throws java.lang.IllegalStateException if the allowlist is not enabled
     */
    void addAllowlistPattern(String urlPattern);

    /**
     * Enables the allowlist, but with no matching URLs. All requests will generated the specified HTTP statusCode.
     *
     * @param statusCode HTTP status code to return to clients on all requests
     */
    void enableEmptyAllowlist(int statusCode);

    /**
     * Clears any existing allowlist and disables allowlisting.
     */
    void disableAllowlist();

    /**
     * Returns the URL-matching regular expressions currently in effect. If the allowlist is disabled, this method always returns an empty collection.
     * If the allowlist is enabled but empty, this method return an empty collection.
     *
     * @return allowlist currently in effect, or an empty collection if the allowlist is disabled or empty
     */
    Collection<String> getAllowlistUrls();

    /**
     * Returns the status code returned for all URLs that do not match the allowlist. If the allowlist is not currently enabled, returns -1.
     *
     * @return HTTP status code returned for non-allowlisted URLs, or -1 if the allowlist is disabled.
     */
    int getAllowlistStatusCode();

    /**
     * Returns true if the allowlist is enabled, otherwise false.
     * @return is AllowlistEnabled
     */
    boolean isAllowlistEnabled();

    /**
     * Adds the specified HTTP headers to every request. Replaces any existing additional headers with the specified headers.
     *
     * @param headers {@code Map<header name, header value>} to append to every request.
     */
    void addHeaders(Map<String, String> headers);

    /**
     * Adds a new HTTP header to every request. If the header already exists on the request, it will be replaced with the specified header.
     *
     * @param name name of the header to add
     * @param value new header's value
     */
    void addHeader(String name, String value);

    /**
     * Removes a header previously added with {@link #addHeader(String name, String value)}.
     *
     * @param name previously-added header's name
     */
    void removeHeader(String name);

    /**
     * Removes all headers previously added with {@link #addHeader(String name, String value)}.
     */
    void removeAllHeaders();

    /**
     * Returns all headers previously added with {@link #addHeader(String name, String value)}.
     *
     * @return {@code Map<header name, header value>}
     */
    Map<String, String> getAllHeaders();

    /**
     * Sets the resolver that will be used to look up host names. To chain multiple resolvers, wrap a list
     * of resolvers in a {@link com.browserup.bup.proxy.dns.ChainedHostResolver}.
     *
     * @param resolver host name resolver
     */
    void setHostNameResolver(AdvancedHostResolver resolver);

    /**
     * Returns the current host name resolver.
     *
     * @return current host name resolver
     */
    AdvancedHostResolver getHostNameResolver();

    /**
     * Waits for existing network traffic to stop, and for the specified quietPeriod to elapse. Returns true if there is no network traffic
     * for the quiet period within the specified timeout, otherwise returns false.
     *
     * @param quietPeriod amount of time after which network traffic will be considered "stopped"
     * @param timeout maximum amount of time to wait for network traffic to stop
     * @param timeUnit TimeUnit for the quietPeriod and timeout
     * @return true if network traffic is stopped, otherwise false
     */
    boolean waitForQuiescence(long quietPeriod, long timeout, TimeUnit timeUnit);

    /**
     * Instructs this proxy to route traffic through an upstream proxy.
     *
     * <b>Note:</b> A chained proxy must be set before the proxy is started, though it can be changed after the proxy is started.
     *
     * @param chainedProxyAddress address of the upstream proxy
     */
    void setChainedProxy(InetSocketAddress chainedProxyAddress);

    /**
     * Instructs this proxy to route traffic through an upstream proxy using HTTPS.
     *
     * @param chainedProxyHTTPS address of the upstream proxy
     */
    void setChainedProxyHTTPS(boolean chainedProxyHTTPS);

    /**
     * Instructs this proxy to route traffic trough an upstream proxy but handling this addresses as exceptions (non proxy hosts)
     *
     * @param upstreamNonProxyHosts non proxy hosts also called proxy exceptions
     */
    void setChainedProxyNonProxyHosts(List<String> upstreamNonProxyHosts);

    /**
     * Returns the address and port of the upstream proxy.
     *
     * @return address and port of the upstream proxy, or null of there is none.
     */
    InetSocketAddress getChainedProxy();

    /**
     * Adds a new filter factory (request/response interceptor) to the beginning of the HttpFilters chain.
     * <b>Usage note:</b> The actual filter (interceptor) instance is created on every request by implementing the
     * {@link HttpFiltersSource#filterRequest(io.netty.handler.codec.http.HttpRequest, io.netty.channel.ChannelHandlerContext)} method and returning an
     * {@link org.littleshoot.proxy.HttpFilters} instance (typically, a subclass of {@link org.littleshoot.proxy.HttpFiltersAdapter}).
     * To disable or bypass a filter on a per-request basis, the filterRequest() method may return null.
     *
     * @param filterFactory factory to generate HttpFilters
     */
    void addFirstHttpFilterFactory(HttpFiltersSource filterFactory);

    /**
     * Adds a new filter factory (request/response interceptor) to the end of the HttpFilters chain.
     * <b>Usage note:</b> The actual filter (interceptor) instance is created on every request by implementing the
     * {@link HttpFiltersSource#filterRequest(io.netty.handler.codec.http.HttpRequest, io.netty.channel.ChannelHandlerContext)} method and returning an
     * {@link org.littleshoot.proxy.HttpFilters} instance (typically, a subclass of {@link org.littleshoot.proxy.HttpFiltersAdapter}).
     * To disable or bypass a filter on a per-request basis, the filterRequest() method may return null.
     *
     *  @param filterFactory factory to generate HttpFilters
     */
    void addLastHttpFilterFactory(HttpFiltersSource filterFactory);

    /**
     * Adds a new ResponseFilter that can be used to examine and manipulate the response before sending it to the client.
     *
     * @param filter filter instance
     */
    void addResponseFilter(ResponseFilter filter);

    /**
     * Removes a previously added ResponseFilter when it's not needed anymore
     *
     * @param filter (previously added) filter instance
     */
    void removeResponseFilter(ResponseFilter filter);

    /**
     * Adds a new RequestFilter that can be used to examine and manipulate the request before sending it to the server.
     *
     * @param filter filter instance
     */
    void addRequestFilter(RequestFilter filter);

    /**
     * Removes a previously added RequestFilter when it's not needed anymore
     *
     * @param filter (previously added) filter instance
     */
    void removeRequestFilter(RequestFilter filter);

    /**
     * Completely disables MITM for this proxy server. The proxy will no longer intercept HTTPS requests, but they will
     * still be pass-through proxied. This option must be set before the proxy is started; otherwise an IllegalStateException will be thrown.
     *
     * @param mitmDisabled when true, MITM capture will be disabled
     * @throws java.lang.IllegalStateException if the proxy is already started
     */
    void setMitmDisabled(boolean mitmDisabled);

    /**
     * Sets the MITM manager, which is responsible for generating forged SSL certificates to present to clients. By default,
     * BrowserUp Proxy uses the ca-certificate-rsa.cer root certificate for impersonation. See the documentation at
     * {@link com.browserup.bup.mitm.manager.ImpersonatingMitmManager} and {@link com.browserup.bup.mitm.manager.ImpersonatingMitmManager.Builder}
     * for details on customizing the root and server certificate generation.
     *
     * @param mitmManager MITM manager to use
     */
    void setMitmManager(MitmManager mitmManager);

    /**
     * Disables verification of all upstream servers' SSL certificates. All upstream servers will be trusted, even if they
     * do not present valid certificates signed by certification authorities in the JDK's trust store. <b>This option
     * exposes the proxy to MITM attacks and should only be used when testing in trusted environments.</b>
     *
     * @param trustAllServers when true, disables upstream server certificate verification
     */
    void setTrustAllServers(boolean trustAllServers);

    /**
     * Sets the {@link TrustSource} that contains trusted root certificate authorities that will be used to validate
     * upstream servers' certificates. When null, disables certificate validation (see warning at {@link #setTrustAllServers(boolean)}).
     *
     * @param trustSource TrustSource containing root CAs, or null to disable upstream server validation
     */
    void setTrustSource(TrustSource trustSource);

    /**
     * Search the entire log for the most recent entry whose request URL matches the given <code>url</code>.
     *
     * @param url Regular expression match of URL to find.
     *            URLs are formatted as: scheme://host:port/path?querystring.
     *            Port is not included in the URL if it is the standard port for the scheme.
     *            Fragments (example.com/#fragment) should not be included in the URL.
     *            If more than one URL found, return the most recently requested URL.
     *            Pattern examples:
     *            - Match a URL with "http" or "https" protocol, "example.com" domain, and "/index.html" exact file path, with no query parameters:
     *              "^(http|https)://example\\.com/index\\.html$"
     *            - Match a URL with "http" protocol, "example.com" domain, "/customer" exact path, followed by any query string:
     *              "^http://example\\.com/customer\\?.*"
     *            - Match a URL with "http" protocol, "example.com" domain, "/products" path, and exactly 1 UUID query parameter named "id":
     *              "^http://example\\.com/products\\?id=[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}$"
     * @return <code>HarEntry</code> for the most recently requested URL matching the given <code>url</code> pattern.
     */
    Optional<HarEntry> findMostRecentEntry(Pattern url);

    /**
     * Search the entire log for entries whose request URL matches the given <code>url</code>.
     *
     * @param url Regular expression match of URL to find.
     *            URLs are formatted as: scheme://host:port/path?querystring.
     *            Port is not included in the URL if it is the standard port for the scheme.
     *            Fragments (example.com/#fragment) should not be included in the URL.
     *            If more than one URL found, use the most recently requested URL.
     *            Pattern examples:
     *            - Match a URL with "http" or "https" protocol, "example.com" domain, and "/index.html" exact file path, with no query parameters:
     *              "^(http|https)://example\\.com/index\\.html$"
     *            - Match a URL with "http" protocol, "example.com" domain, "/customer" exact path, followed by any query string:
     *              "^http://example\\.com/customer\\?.*"
     *            - Match a URL with "http" protocol, "example.com" domain, "/products" path, and exactly 1 UUID query parameter named "id":
     *              "^http://example\\.com/products\\?id=[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}$"
     *
     * @return A list of <code>HarEntry</code> for any requests whose URL matches the given <code>url</code> pattern,
     *         or an empty list if none match.
     */
    Collection<HarEntry> findEntries(Pattern url);

    /**
     * Assert that the response time for the most recent request
     * found by a given URL pattern is less than or equal to a given number of milliseconds.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param milliseconds Maximum time in milliseconds, inclusive.
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseTimeLessThanOrEqual(Pattern url, long milliseconds);

    /**
     * Assert that the response times for all requests
     * found by a given URL pattern are less than or equal to a given number of milliseconds.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param milliseconds Maximum time in milliseconds, inclusive.
     * @return Assertion result
     */
    AssertionResult assertResponseTimeLessThanOrEqual(Pattern url, long milliseconds);

    /**
     * Assert that response content for the most recent request found by a given URL pattern contains specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param text String to search in the content
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseContentContains(Pattern url, String text);

    /**
     * Assert that response content for the most recent request
     * found by a given URL pattern doesn't contain specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param text String to search in the content
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseContentDoesNotContain(Pattern url, String text);

    /**
     * Assert that response content for the most recent request
     * found by a given URL pattern matches content pattern.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param contentPattern Regular expression match of content to find.
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseContentMatches(Pattern url, Pattern contentPattern);

    /**
     * Assert that content length of all responses found by url pattern do not exceed max value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param max Max length of content, inclusive
     * @return Assertion result
     */
    AssertionResult assertAnyUrlContentLengthLessThanOrEquals(Pattern url, Long max);

    /**
     * Assert that responses content for all requests found by url pattern matches content pattern.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param contentPattern Regular expression match of content to find.
     * @return Assertion result
     */
    AssertionResult assertAnyUrlContentMatches(Pattern url, Pattern contentPattern);

    /**
     * Assert that responses content for all requests found by a given URL pattern contain specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param text String to search in the content
     * @return Assertion result
     */
    AssertionResult assertAnyUrlContentContains(Pattern url, String text);

    /**
     * Assert that responses content for all requests found by a given URL pattern don't contain specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param text String to search in the content
     * @return Assertion result
     */
    AssertionResult assertAnyUrlContentDoesNotContain(Pattern url, String text);

    /**
     * Assert that headers of all responses found by url pattern contain specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param value Header value
     * @return Assertion result
     */
    AssertionResult assertAnyUrlResponseHeaderContains(Pattern url, String value);

    /**
     * Assert that if responses found by url pattern have headers with specified name
     * - among them must be one header with value containing specified text.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param name Header name
     * @param value Header value
     * @return Assertion result
     */
    AssertionResult assertAnyUrlResponseHeaderContains(Pattern url, String name, String value);

    /**
     * Assert that headers of all responses found by url pattern don't contain specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param value Header value
     * @return Assertion result
     */
    AssertionResult assertAnyUrlResponseHeaderDoesNotContain(Pattern url, String value);

    /**
     * Assert that if responses found by url pattern have headers with specified name
     * - their values must not contain specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param name Header name
     * @param value Header value
     * @return Assertion result
     */
    AssertionResult assertAnyUrlResponseHeaderDoesNotContain(Pattern url, String name, String value);

    /**
     * Assert that all headers of all responses found by url pattern have values matching value pattern.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param valuePattern Regular expression match of header value.
     * @return Assertion result
     */
    default AssertionResult assertAnyUrlResponseHeaderMatches(Pattern url, Pattern valuePattern) {
        return assertAnyUrlResponseHeaderMatches(url, null, valuePattern);
    }

    /**
     * Assert that if responses found by url pattern have headers with name
     * found by name pattern - their values should match value pattern.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param namePattern Regular expression match of header name to find.
     * @param valuePattern Regular expression match of header value.
     * @return Assertion result
     */
    AssertionResult assertAnyUrlResponseHeaderMatches(Pattern url, Pattern namePattern, Pattern valuePattern);

    /**
     * Assert that if the most recent response found by url pattern has header with specified name
     * - it's value must contain specified text.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param name Header name
     * @param value Header value
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseHeaderContains(Pattern url, String name, String value);

    /**
     * Assert that headers of the most recent response found by url pattern contain specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param value Header value
     * @return Assertion result
     */
    default AssertionResult assertMostRecentResponseHeaderContains(Pattern url, String value) {
        return assertMostRecentResponseHeaderContains(url, null, value);
    }

    /**
     * Assert that if the most recent response found by url pattern has header with specified name
     * - it's value must not contain specified text.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param name Header name
     * @param value Header value
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseHeaderDoesNotContain(Pattern url, String name, String value);

    /**
     * Assert that headers of the most recent response found by url pattern do not contain specified value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param value Header value
     * @return Assertion result
     */
    default AssertionResult assertMostRecentResponseHeaderDoesNotContain(Pattern url, String value) {
        return assertMostRecentResponseHeaderDoesNotContain(url, null, value);
    }

    /**
     * Assert that if the most recent response found by url pattern has header with name
     * found by name pattern - it's value should match value pattern.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param name Regular expression match of header name to find.
     * @param value Regular expression match of header value.
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseHeaderMatches(Pattern url, Pattern name, Pattern value);

    /**
     * Assert that all headers of the most recent response found by url pattern have values matching value pattern.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param value Regular expression match of header value.
     * @return Assertion result
     */
    default AssertionResult assertMostRecentResponseHeaderMatches(Pattern url, Pattern value) {
        return assertMostRecentResponseHeaderMatches(url, null, value);
    }

    /**
     * Assert that content length of the most recent response found by url pattern does not exceed max value.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param max Max length of content, inclusive
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseContentLengthLessThanOrEqual(Pattern url, Long max);

    /**
     * Assert that all responses of current step have specified status.
     * @param status Expected http status
     * @return Assertion result
     */
    AssertionResult assertResponseStatusCode(Integer status);

    /**
     * Assert that all responses of current step have statuses belonging to the same class.
     * @param clazz Http status class
     * @return Assertion result
     */
    AssertionResult assertResponseStatusCode(HttpStatusClass clazz);

    /**
     * Assert that all responses found by url pattern have specified http status.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param status Http status
     * @return Assertion result
     */
    AssertionResult assertResponseStatusCode(Pattern url, Integer status);

    /**
     * Assert that all responses found by url pattern have statuses belonging to the same class.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param clazz Http status class
     * @return Assertion result
     */
    AssertionResult assertResponseStatusCode(Pattern url, HttpStatusClass clazz);

    /**
     * Assert that the most recent response has specified status.
     * @param status Http status
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseStatusCode(Integer status);

    /**
     * Assert that the most recent response has status belonging to specified class.
     * @param clazz Http status class
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseStatusCode(HttpStatusClass clazz);

    /**
     * Assert that the most recent response found by url pattern has specified status.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param status Http status
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseStatusCode(Pattern url, Integer status);

    /**
     * Assert that the most recent response found by url pattern has status belonging to specified class.
     * @param url Regular expression match of URL to find.
     *            See examples {@link com.browserup.bup.BrowserUpProxy#findEntries(java.util.regex.Pattern)}
     * @param clazz Http status class
     * @return Assertion result
     */
    AssertionResult assertMostRecentResponseStatusCode(Pattern url, HttpStatusClass clazz);
}
