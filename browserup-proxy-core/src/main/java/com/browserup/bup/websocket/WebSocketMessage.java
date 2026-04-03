package com.browserup.bup.websocket;

import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a captured WebSocket frame (text, binary, or control frame).
 *
 * @since 3.3.0
 */
public class WebSocketMessage {

    public enum Direction {
        CLIENT_TO_SERVER,
        SERVER_TO_CLIENT
    }

    public enum Type {
        TEXT, BINARY, CLOSE, PING, PONG
    }

    private final String url;
    private final Direction direction;
    private final Type type;
    private final byte[] content;
    private final Date timestamp;

    public WebSocketMessage(String url, Direction direction, Type type, byte[] content) {
        this.url = url;
        this.direction = direction;
        this.type = type;
        this.content = content;
        this.timestamp = new Date();
    }

    public String url() {
        return url;
    }

    public Direction direction() {
        return direction;
    }

    public Type type() {
        return type;
    }

    public byte[] content() {
        return content;
    }

    public Date timestamp() {
        return timestamp;
    }

    /**
     * Returns the frame payload decoded as UTF-8 text. Only meaningful for {@link Type#TEXT} frames.
     */
    public String textContent() {
        return new String(content, UTF_8);
    }

    @Override
    public String toString() {
        return String.format("WebSocketMessage{url='%s', direction=%s, type=%s, contentLength=%d, timestamp=%s}",
                url, direction, type, content.length, timestamp);
    }
}
