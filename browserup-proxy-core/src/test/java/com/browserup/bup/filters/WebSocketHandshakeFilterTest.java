package com.browserup.bup.filters;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebSocketHandshakeFilter}, focusing on per-direction stateful decoding.
 */
class WebSocketHandshakeFilterTest {

    private ChannelHandlerContext ctx;
    private HttpRequest wsUpgradeRequest;
    private List<WebSocketFrame> captured;
    private List<WebSocketListener> listeners;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        Attribute<Object> nullAttr = mock(Attribute.class);
        when(nullAttr.get()).thenReturn(null);

        ChannelFuture closeFuture = mock(ChannelFuture.class);
        Channel channel = mock(Channel.class);
        when(channel.closeFuture()).thenReturn(closeFuture);
        when(channel.attr(any())).thenReturn(nullAttr);

        ctx = mock(ChannelHandlerContext.class);
        when(ctx.channel()).thenReturn(channel);

        wsUpgradeRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws");
        wsUpgradeRequest.headers().set(HttpHeaderNames.CONNECTION, "Upgrade");
        wsUpgradeRequest.headers().set(HttpHeaderNames.UPGRADE, "websocket");
        wsUpgradeRequest.headers().set(HttpHeaderNames.HOST, "localhost");

        captured = new ArrayList<>();
        listeners = new ArrayList<>();
        listeners.add((frame, info) -> captured.add(frame));
    }

    @Test
    void completeClientFrameIsDecodedAndNotified() {
        WebSocketHandshakeFilter filter = new WebSocketHandshakeFilter(wsUpgradeRequest, ctx, listeners);
        byte[] frame = buildMaskedTextFrame("hello");

        filter.webSocketFrameReceived(() -> frame, true);

        assertEquals(1, captured.size());
        assertEquals(WebSocketFrame.Opcode.TEXT, captured.get(0).getOpcode());
        assertEquals("hello", captured.get(0).getTextContent());
        assertTrue(captured.get(0).isFromClient());
    }

    @Test
    void fragmentedClientFrameIsReassembledAcrossCallbacks() {
        WebSocketHandshakeFilter filter = new WebSocketHandshakeFilter(wsUpgradeRequest, ctx, listeners);
        byte[] full = buildMaskedTextFrame("hello");

        // Deliver byte 0 first (FIN+opcode), then the rest
        byte[] part1 = Arrays.copyOfRange(full, 0, 1);
        byte[] part2 = Arrays.copyOfRange(full, 1, full.length);

        filter.webSocketFrameReceived(() -> part1, true);
        assertEquals(0, captured.size(), "Partial frame must not emit a notification yet");

        filter.webSocketFrameReceived(() -> part2, true);
        assertEquals(1, captured.size(), "Complete frame must be notified after second chunk");
        assertEquals("hello", captured.get(0).getTextContent());
        assertTrue(captured.get(0).isFromClient());
    }

    @Test
    void fragmentedServerFrameIsReassembledAcrossCallbacks() {
        WebSocketHandshakeFilter filter = new WebSocketHandshakeFilter(wsUpgradeRequest, ctx, listeners);
        byte[] full = buildUnmaskedTextFrame("world");

        byte[] part1 = Arrays.copyOfRange(full, 0, 1);
        byte[] part2 = Arrays.copyOfRange(full, 1, full.length);

        filter.webSocketFrameReceived(() -> part1, false);
        assertEquals(0, captured.size(), "Partial frame must not emit a notification yet");

        filter.webSocketFrameReceived(() -> part2, false);
        assertEquals(1, captured.size(), "Complete frame must be notified after second chunk");
        assertEquals("world", captured.get(0).getTextContent());
        assertTrue(captured.get(0).isFromServer());
    }

    @Test
    void clientAndServerDecodersAreSeparate() {
        WebSocketHandshakeFilter filter = new WebSocketHandshakeFilter(wsUpgradeRequest, ctx, listeners);

        // Send first byte of a client frame
        byte[] clientFull = buildMaskedTextFrame("c");
        filter.webSocketFrameReceived(() -> Arrays.copyOfRange(clientFull, 0, 1), true);

        // Send a complete server frame — must not interfere with the buffered client fragment
        byte[] serverFull = buildUnmaskedTextFrame("s");
        filter.webSocketFrameReceived(() -> serverFull, false);

        // Only the complete server frame should have been notified so far
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).isFromServer());

        // Now complete the client frame
        filter.webSocketFrameReceived(() -> Arrays.copyOfRange(clientFull, 1, clientFull.length), true);
        assertEquals(2, captured.size());
        assertTrue(captured.get(1).isFromClient());
    }

    @Test
    void noNotificationWhenListenersAreEmpty() {
        List<WebSocketListener> empty = new ArrayList<>();
        WebSocketHandshakeFilter filter = new WebSocketHandshakeFilter(wsUpgradeRequest, ctx, empty);
        byte[] frame = buildMaskedTextFrame("x");

        filter.webSocketFrameReceived(() -> frame, true);

        assertTrue(captured.isEmpty());
    }

    // ---- helpers ----

    private static byte[] buildMaskedTextFrame(String text) {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        byte[] mask = {0x37, 0x42, 0x11, 0x22};
        byte[] frame = new byte[2 + 4 + payload.length];
        frame[0] = (byte) 0x81;
        frame[1] = (byte) (0x80 | payload.length);
        frame[2] = mask[0]; frame[3] = mask[1]; frame[4] = mask[2]; frame[5] = mask[3];
        for (int i = 0; i < payload.length; i++) frame[6 + i] = (byte) (payload[i] ^ mask[i % 4]);
        return frame;
    }

    private static byte[] buildUnmaskedTextFrame(String text) {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        byte[] frame = new byte[2 + payload.length];
        frame[0] = (byte) 0x81;
        frame[1] = (byte) payload.length;
        System.arraycopy(payload, 0, frame, 2, payload.length);
        return frame;
    }
}
