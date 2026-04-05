package com.browserup.bup.filters;

import com.browserup.bup.util.HttpMessageInfo;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

/**
 * Handles WebSocket connections through the proxy.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Restores the {@code Upgrade} and {@code Connection} headers for WebSocket upgrade requests.
 *       LittleProxy strips them as hop-by-hop headers before the {@code proxyToServerRequest} hook
 *       fires; this filter re-adds them so the upstream server receives a valid handshake.</li>
 *   <li>Notifies registered {@link WebSocketListener}s when WebSocket frames are forwarded,
 *       after the connection has been upgraded.</li>
 * </ol>
 */
public class WebSocketHandshakeFilter extends HttpsAwareFiltersAdapter {
    private static final Logger log = LoggerFactory.getLogger(WebSocketHandshakeFilter.class);

    private final boolean isWebSocketUpgrade;
    private final List<WebSocketListener> listeners;

    public WebSocketHandshakeFilter(HttpRequest originalRequest, ChannelHandlerContext ctx,
                                    List<WebSocketListener> listeners) {
        super(originalRequest, ctx);
        this.isWebSocketUpgrade = isWebSocketUpgradeRequest(originalRequest);
        this.listeners = listeners;
    }

    @Override
    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
        if (isWebSocketUpgrade && httpObject instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) httpObject;
            request.headers().set(HttpHeaderNames.UPGRADE, "websocket");
            request.headers().set(HttpHeaderNames.CONNECTION, "Upgrade");
        }
        return null;
    }

    @Override
    public void webSocketFrameReceived(Supplier<byte[]> frameBytesSource, boolean fromClient) {
        if (listeners.isEmpty()) {
            return;
        }
        byte[] frameBytes = frameBytesSource.get();

        // Client-to-server frames are masked per RFC 6455; server-to-client frames are not.
        // WebSocket13FrameDecoder handles masking/unmasking when expectMaskedFrames matches.
        boolean expectMaskedFrames = fromClient;
        EmbeddedChannel decoderChannel = new EmbeddedChannel(
                new WebSocket13FrameDecoder(expectMaskedFrames, false, Integer.MAX_VALUE));
        try {
            decoderChannel.writeInbound(Unpooled.wrappedBuffer(frameBytes));
            io.netty.handler.codec.http.websocketx.WebSocketFrame nettyFrame;
            while ((nettyFrame = decoderChannel.readInbound()) != null) {
                try {
                    WebSocketFrame frame = WebSocketFrame.fromNettyFrame(nettyFrame, fromClient);
                    notifyListeners(frame);
                } finally {
                    nettyFrame.release();
                }
            }
        } finally {
            decoderChannel.finish();
        }
    }

    private void notifyListeners(WebSocketFrame frame) {
        HttpMessageInfo messageInfo = new HttpMessageInfo(
                originalRequest, ctx, isHttps(), getFullUrl(originalRequest), getOriginalUrl());
        for (WebSocketListener listener : listeners) {
            try {
                listener.onWebSocketFrame(frame, messageInfo);
            } catch (RuntimeException e) {
                log.warn("WebSocketListener threw exception", e);
            }
        }
    }

    private static boolean isWebSocketUpgradeRequest(HttpRequest request) {
        return request.headers().contains(HttpHeaderNames.CONNECTION, "Upgrade", true)
                && request.headers().contains(HttpHeaderNames.UPGRADE, "websocket", true);
    }
}
