package com.browserup.bup.websocket;

/**
 * Listener for WebSocket messages intercepted by the proxy.
 * <p>
 * Implementations are called from Netty IO threads — keep them non-blocking.
 * Register via {@link com.browserup.bup.BrowserUpProxy#addWebSocketListener}.
 *
 * @since 3.3.0
 */
@FunctionalInterface
public interface WebSocketListener {
    /**
     * Called for every captured WebSocket frame (text, binary, close, ping, pong).
     *
     * @param message the captured frame
     */
    void onMessage(WebSocketMessage message);
}
