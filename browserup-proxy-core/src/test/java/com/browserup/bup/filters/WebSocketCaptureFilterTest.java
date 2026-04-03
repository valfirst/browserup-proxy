package com.browserup.bup.filters;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class WebSocketCaptureFilterTest {
    private final ChannelHandlerContext ctx = mock();

    @Test
    void buildWebSocketUrl_ws() {
        HttpRequest originalRequest = request("http://foo.bar");
        WebSocketCaptureFilter filter = new WebSocketCaptureFilter(originalRequest, ctx, emptyList());
        assertEquals("ws://foo.bar", filter.buildWebSocketUrl());
    }

    @Test
    void buildWebSocketUrl_wss() {
        HttpRequest originalRequest = request("https://foo.bar");
        WebSocketCaptureFilter filter = new WebSocketCaptureFilter(originalRequest, ctx, emptyList());
        assertEquals("wss://foo.bar", filter.buildWebSocketUrl());
    }

    private @NonNull DefaultHttpRequest request(String url) {
        return new DefaultHttpRequest(HTTP_1_1, HttpMethod.GET, url);
    }
}