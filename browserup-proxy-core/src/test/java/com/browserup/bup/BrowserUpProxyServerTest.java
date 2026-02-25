package com.browserup.bup;

import io.netty.handler.codec.http.HttpRequest;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrowserUpProxyServerTest {
    @Test
    public void detectsNonProxyHosts_byURL() {
        BrowserUpProxyServer proxy = new BrowserUpProxyServer();
        proxy.setChainedProxyNonProxyHosts(List.of("127.0.0.1", "*.example.com"));

        assertThat(proxy.isNonProxyHost(request("http://127.0.0.1:8080")), equalTo(true));
        assertThat(proxy.isNonProxyHost(request("http://127.0.0.2:8080")), equalTo(false));
        assertThat(proxy.isNonProxyHost(request("https://foo.example.com")), equalTo(true));
        assertThat(proxy.isNonProxyHost(request("https://bar.example.com:443")), equalTo(true));
        assertThat(proxy.isNonProxyHost(request("https://example.com")), equalTo(false));
        assertThat(proxy.isNonProxyHost(request("https://a.b.c.foo.example.com")), equalTo(true));
        assertThat(proxy.isNonProxyHost(request("https://foo-example.com")), equalTo(false));
    }

    @Test
    public void detectsNonProxyHosts_byHostname() {
        BrowserUpProxyServer proxy = new BrowserUpProxyServer();
        proxy.setChainedProxyNonProxyHosts(List.of("127.0.0.1", "*.example.com"));

        assertThat(proxy.isNonProxyHost(request("127.0.0.1:36915")), equalTo(true));
        assertThat(proxy.isNonProxyHost(request("127.0.0.2:8080")), equalTo(false));
        assertThat(proxy.isNonProxyHost(request("foo.example.com")), equalTo(true));
        assertThat(proxy.isNonProxyHost(request("bar.example.com:443")), equalTo(true));
        assertThat(proxy.isNonProxyHost(request("example.com")), equalTo(false));
        assertThat(proxy.isNonProxyHost(request("a.b.c.foo.example.com")), equalTo(true));
        assertThat(proxy.isNonProxyHost(request("foo-example.com")), equalTo(false));
    }

    @Test
    public void nonProxyHostsNotSpecified() {
        BrowserUpProxyServer proxy = new BrowserUpProxyServer();
        proxy.setChainedProxyNonProxyHosts(null);

        assertThat(proxy.isNonProxyHost(request("http://127.0.0.1:8080")), equalTo(false));
        assertThat(proxy.isNonProxyHost(request("https://foo.example.com")), equalTo(false));
    }

    private HttpRequest request(String url) {
        HttpRequest request = mock();
        when(request.uri()).thenReturn(url);
        return request;
    }
}