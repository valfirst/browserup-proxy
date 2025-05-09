package com.browserup.bup.util;

import com.browserup.bup.exception.UnsupportedCharsetException;
import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BrowserUpHttpUtilTest {

    @Test
    public void testGetResourceFromUri() throws URISyntaxException {
        Map<String, String> uriToResource = Map.of(
                "http://www.example.com/the/resource", "/the/resource",
                "https://example/resource", "/resource",
                "http://127.0.0.1/ip/address/resource", "/ip/address/resource",
                "https://hostname:8080/host/and/port/resource", "/host/and/port/resource",
                "http://hostname/", "/",
                "https://127.0.0.1/", "/",
                "http://127.0.0.1:1950/ip/port/resource", "/ip/port/resource",
                "https://[abcd:1234::17]/ipv6/literal/resource", "/ipv6/literal/resource",
                "http://[abcd:1234::17]:50/ipv6/with/port/literal/resource", "/ipv6/with/port/literal/resource",
                "https://hostname/query/param/resource?param=value", "/query/param/resource?param=value"
        );

        for (Map.Entry<String, String> entry : uriToResource.entrySet()) {
            String uri = entry.getKey();
            String parsedResource = BrowserUpHttpUtil.getRawPathAndParamsFromUri(uri);
            assertEquals("Parsed resource from URL did not match expected resource for URL: " + uri,
                    entry.getValue(), parsedResource);
        }
    }

    @Test
    public void testGetHostAndPortFromUri() throws URISyntaxException {
        Map<String, String> uriToHostAndPort = Map.of(
                "http://www.example.com/some/resource", "www.example.com",
                "https://www.example.com:8080/some/resource", "www.example.com:8080",
                "http://127.0.0.1/some/resource", "127.0.0.1",
                "https://127.0.0.1:8080/some/resource?param=value", "127.0.0.1:8080",
                "http://localhost/some/resource", "localhost",
                "https://localhost:1820/", "localhost:1820",
                "http://[abcd:1234::17]/ipv6/literal/resource", "[abcd:1234::17]",
                "https://[abcd:1234::17]:50/ipv6/with/port/literal/resource", "[abcd:1234::17]:50"
        );

        for (Map.Entry<String, String> entry : uriToHostAndPort.entrySet()) {
            String uri = entry.getKey();
            String parsedHostAndPort = HttpUtil.getHostAndPortFromUri(uri);
            assertEquals(
                    "Parsed host and port from URL did not match expected host and port for URL: " + uri,
                    entry.getValue(), parsedHostAndPort);

        }
    }

    @Test
    public void testReadCharsetInContentTypeHeader() throws UnsupportedCharsetException {
        Map<String, Charset> contentTypeHeaderAndCharset = new LinkedHashMap<String, Charset>(10);
        contentTypeHeaderAndCharset.put("text/html; charset=UTF-8", StandardCharsets.UTF_8);
        contentTypeHeaderAndCharset.put("text/html; charset=US-ASCII", StandardCharsets.US_ASCII);
        contentTypeHeaderAndCharset.put("text/html", null);
        contentTypeHeaderAndCharset.put("application/json;charset=utf-8", StandardCharsets.UTF_8);
        contentTypeHeaderAndCharset.put("text/*; charset=US-ASCII", StandardCharsets.US_ASCII);
        contentTypeHeaderAndCharset.put("unknown-type/something-incredible", null);
        contentTypeHeaderAndCharset.put("unknown-type/something-incredible;charset=UTF-8", StandardCharsets.UTF_8);
        contentTypeHeaderAndCharset.put("1234 & extremely malformed!", null);
        contentTypeHeaderAndCharset.put("1234 & extremely malformed!;charset=UTF-8", null);
        contentTypeHeaderAndCharset.put("", null);

        for (Map.Entry<String, Charset> entry : contentTypeHeaderAndCharset.entrySet()) {
            String contentTypeHeader = entry.getKey();
            Charset derivedCharset = BrowserUpHttpUtil.readCharsetInContentTypeHeader(contentTypeHeader);
            assertEquals(
                    "Charset derived from parsed content type header did not match expected charset for content type header: " + contentTypeHeader,
                    entry.getValue(), derivedCharset);
        }


        Charset derivedCharset = BrowserUpHttpUtil.readCharsetInContentTypeHeader(null);
        Assert.assertNull("Expected null Content-Type header to return a null charset", derivedCharset);

        boolean threwException = false;
        try {
            BrowserUpHttpUtil.readCharsetInContentTypeHeader("text/html; charset=FUTURE_CHARSET");
        } catch (Exception ignored) {
            threwException = true;
        }


        assertTrue(
                "Expected an UnsupportedCharsetException to occur when parsing the content type header text/html; charset=FUTURE_CHARSET",
                threwException);
    }

    @Test
    public void testHasTextualContent() {
        Map<String, Boolean> contentTypeHeaderAndTextFlag = Map.of(
                "text/html", true,
                "text/*", true,
                "application/x-javascript", true,
                "application/javascript", true,
                "application/xml", true,
                "application/xhtml+xml", true,
                "application/xhtml+xml; charset=UTF-8", true,
                "application/octet-stream", false,
                "", false
        );

        for (Map.Entry<String, Boolean> entry : contentTypeHeaderAndTextFlag.entrySet()) {
            String contentTypeHeader = entry.getKey();
            boolean isTextualContent = BrowserUpHttpUtil.hasTextualContent((String) contentTypeHeader);
            assertEquals(
                    "hasTextualContent did not return expected value for content type header: " + contentTypeHeader,
                    entry.getValue(), isTextualContent);
        }


        boolean isTextualContent = BrowserUpHttpUtil.hasTextualContent(null);
        Assert.assertFalse("Expected hasTextualContent to return false for null content type", isTextualContent);
    }

    @Test
    public void testGetRawPathWithQueryParams() throws URISyntaxException {
        String path = "/some%20resource?param%20name=value";

        assertEquals(path, BrowserUpHttpUtil.getRawPathAndParamsFromUri("https://www.example.com" + path));
    }

    @Test
    public void testGetRawPathWithoutQueryParams() throws URISyntaxException {
        String path = "/some%20resource";

        assertEquals(path, BrowserUpHttpUtil.getRawPathAndParamsFromUri("https://www.example.com" + path));
    }

    @Test
    public void testRemoveMatchingPort()
    {
        String portRemoved = BrowserUpHttpUtil.removeMatchingPort("www.example.com:443", 443);
        assertEquals("www.example.com", portRemoved);

        String hostnameWithNonMatchingPort = BrowserUpHttpUtil.removeMatchingPort("www.example.com:443", 1234);
        assertEquals("www.example.com:443", hostnameWithNonMatchingPort);

        String hostnameNoPort = BrowserUpHttpUtil.removeMatchingPort("www.example.com", 443);
        assertEquals("www.example.com", hostnameNoPort);

        String ipv4WithoutPort = BrowserUpHttpUtil.removeMatchingPort("127.0.0.1:443", 443);
        assertEquals("127.0.0.1", ipv4WithoutPort);

        String ipv4WithNonMatchingPort = BrowserUpHttpUtil.removeMatchingPort("127.0.0.1:443", 1234);
        assertEquals("127.0.0.1:443", ipv4WithNonMatchingPort);

        String ipv4NoPort = BrowserUpHttpUtil.removeMatchingPort("127.0.0.1", 443);
        assertEquals("127.0.0.1", ipv4NoPort);

        String ipv6WithoutPort = BrowserUpHttpUtil.removeMatchingPort("[::1]:443", 443);
        assertEquals("[::1]", ipv6WithoutPort);

        String ipv6WithNonMatchingPort = BrowserUpHttpUtil.removeMatchingPort("[::1]:443", 1234);
        assertEquals("[::1]:443", ipv6WithNonMatchingPort);

        String ipv6NoPort = BrowserUpHttpUtil.removeMatchingPort("[::1]", 443);
        assertEquals("[::1]", ipv6NoPort);
    }

}
