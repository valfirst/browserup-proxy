package com.browserup.bup.filters;

import com.browserup.bup.util.HttpMessageInfo;

/**
 * Listener for WebSocket frames passing through the proxy.
 *
 * <p>Implement this interface and register it via
 * {@link com.browserup.bup.BrowserUpProxy#addWebSocketListener(WebSocketListener)} to observe
 * WebSocket traffic. The listener is read-only — frames cannot be modified or suppressed.
 *
 * <p>A new listener instance is used across all WebSocket connections; use
 * {@link HttpMessageInfo} to identify which connection/URL a frame belongs to.
 */
@FunctionalInterface
public interface WebSocketListener {

    /**
     * Called when a WebSocket frame is received and is about to be forwarded.
     *
     * @param frame   the parsed WebSocket frame; see {@link WebSocketFrame} for opcode and payload
     * @param messageInfo context about the original HTTP upgrade request
     */
    void onWebSocketFrame(WebSocketFrame frame, HttpMessageInfo messageInfo);
}
