package com.browserup.bup.util;

import com.google.common.net.HostAndPort;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/**
 * Contains utility methods for netty {@link HttpRequest} and related objects.
 */
public class HttpUtil {
    /**
     * Identify the host of an HTTP request. This method uses the URI of the request if possible, otherwise it attempts to find the host
     * in the request headers.
     *
     * @param httpRequest HTTP request to parse the host from
     * @return the host the request is connecting to, or null if no host can be found
     */
    public static String getHostFromRequest(HttpRequest httpRequest) {
        // try to use the URI from the request first, if the URI starts with http:// or https://. checking for http/https avoids confusing
        // java's URI class when the request is for a malformed URL like '//some-resource'.
        String host = null;
        if (startsWithHttpOrHttps(httpRequest.getUri())) {
            try {
                URI uri = new URI(httpRequest.getUri());
                host = uri.getHost();
            } catch (URISyntaxException e) {
            }
        }

        return parseHost(host, httpRequest, false);
    }

    /**
     * Gets the host and port from the specified request. Returns the host and port from the request URI if available,
     * otherwise retrieves the host and port from the Host header.
     *
     * @param httpRequest HTTP request
     * @return host and port of the request
     */
    public static String getHostAndPortFromRequest(HttpRequest httpRequest) {
        String host = null;
        if (startsWithHttpOrHttps(httpRequest.uri())) {
            try {
                host = getHostAndPortFromUri(httpRequest.uri());
            } catch (URISyntaxException e) {
                // the URI could not be parsed, so return the host and port in the Host header
            }
        }

        return parseHost(host, httpRequest, true);
    }

    /**
     * Returns true if the string starts with http:// or https://.
     *
     * @param uri string to evaluate
     * @return true if the string starts with http:// or https://
     */
    public static boolean startsWithHttpOrHttps(String uri) {
        if (uri == null) {
            return false;
        }

        // the scheme is case insensitive, according to RFC 7230, section 2.7.3:
        /*
            The scheme and host
            are case-insensitive and normally provided in lowercase; all other
            components are compared in a case-sensitive manner.
        */
        String lowercaseUri = uri.toLowerCase(Locale.US);

        return lowercaseUri.startsWith("http://") || lowercaseUri.startsWith("https://");
    }

    /**
     * Retrieves the host and port from the specified URI.
     *
     * @param uriString URI to retrieve the host and port from
     * @return the host and port from the URI as a String
     * @throws URISyntaxException if the specified URI is invalid or cannot be parsed
     */
    public static String getHostAndPortFromUri(String uriString) throws URISyntaxException {
        URI uri = new URI(uriString);
        if (uri.getPort() == -1) {
            return uri.getHost();
        } else {
            return HostAndPort.fromParts(uri.getHost(), uri.getPort()).toString();
        }
    }

    private static String parseHost(String host, HttpRequest httpRequest, boolean includePort) {
        // if there was no host in the URI, attempt to grab the host from the Host header
        if (isEmpty(host)) {
            host = parseHostHeader(httpRequest, includePort);
        }

        // if there was not a Host header, and the method is CONNECT, use that as the host
        if (isEmpty(host)) {
            host = hostFromConnect(httpRequest, includePort);
        }

        return host;
    }

    /**
     * Retrieves the host and, optionally, the port from the specified request's Host header.
     *
     * @param httpRequest HTTP request
     * @param includePort when true, include the port
     * @return the host and, optionally, the port specified in the request's Host header
     */
    private static String parseHostHeader(HttpRequest httpRequest, boolean includePort) {
        // this header parsing logic is adapted from ClientToProxyConnection#identifyHostAndPort.
        List<String> hosts = httpRequest.headers().getAll(HttpHeaderNames.HOST);
        if (!hosts.isEmpty()) {
            String hostAndPort = hosts.get(0);

            if (includePort) {
                return hostAndPort;
            } else {
                HostAndPort parsedHostAndPort = HostAndPort.fromString(hostAndPort);
                return parsedHostAndPort.getHost();
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieves the host and, optionally, the port from the specified request's URI if the method is CONNECT.
     *
     * @param httpRequest HTTP request
     * @param includePort when true, include the port
     * @return the host and, optionally, the port specified in the request's URI
     */
    private static String hostFromConnect(HttpRequest httpRequest, boolean includePort) {
        if (HttpMethod.CONNECT.equals(httpRequest.method())) {
            if (includePort) {
                return httpRequest.uri();
            }
            HostAndPort parsedHostAndPort = HostAndPort.fromString(httpRequest.uri());
            return parsedHostAndPort.getHost();
        }
        return null;
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
