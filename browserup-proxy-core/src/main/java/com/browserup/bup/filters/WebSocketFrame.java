package com.browserup.bup.filters;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a single WebSocket frame observed at the proxy.
 *
 * <p>The frame payload is already unmasked: client-to-server frames in the WebSocket protocol
 * are transmitted masked, but this class always exposes the decoded payload.
 */
public class WebSocketFrame {

    /**
     * WebSocket opcodes defined in RFC 6455.
     */
    public enum Opcode {
        CONTINUATION, TEXT, BINARY, CLOSE, PING, PONG, UNKNOWN
    }

    private final boolean fromClient;
    private final Opcode opcode;
    private final byte[] payload;

    private WebSocketFrame(boolean fromClient, Opcode opcode, byte[] payload) {
        this.fromClient = fromClient;
        this.opcode = opcode;
        this.payload = payload;
    }

    /**
     * Creates a {@code WebSocketFrame} from a decoded Netty WebSocket frame.
     * Copies the payload out of the (reference-counted) Netty buffer immediately,
     * so the caller may safely release the Netty frame afterward.
     *
     * @param nettyFrame decoded Netty frame (not yet released)
     * @param fromClient true if the frame was sent by the client
     */
    static WebSocketFrame fromNettyFrame(
            io.netty.handler.codec.http.websocketx.WebSocketFrame nettyFrame,
            boolean fromClient) {

        Opcode opcode;
        if (nettyFrame instanceof TextWebSocketFrame) opcode = Opcode.TEXT;
        else if (nettyFrame instanceof BinaryWebSocketFrame) opcode = Opcode.BINARY;
        else if (nettyFrame instanceof CloseWebSocketFrame) opcode = Opcode.CLOSE;
        else if (nettyFrame instanceof PingWebSocketFrame) opcode = Opcode.PING;
        else if (nettyFrame instanceof PongWebSocketFrame) opcode = Opcode.PONG;
        else if (nettyFrame instanceof ContinuationWebSocketFrame) opcode = Opcode.CONTINUATION;
        else opcode = Opcode.UNKNOWN;

        ByteBuf content = nettyFrame.content();
        byte[] payload = new byte[content.readableBytes()];
        content.getBytes(content.readerIndex(), payload);

        return new WebSocketFrame(fromClient, opcode, payload);
    }

    /**
     * Returns true if this frame was sent by the client (towards the server).
     */
    public boolean isFromClient() {
        return fromClient;
    }

    /**
     * Returns true if this frame was sent by the server (towards the client).
     */
    public boolean isFromServer() {
        return !fromClient;
    }

    /**
     * The opcode of this WebSocket frame.
     */
    public Opcode getOpcode() {
        return opcode;
    }

    /**
     * The unmasked payload bytes of this frame.
     */
    public byte[] getBinaryContent() {
        return payload.clone();
    }

    /**
     * Returns the payload decoded as a UTF-8 string. Intended for {@link Opcode#TEXT} frames.
     */
    public String getTextContent() {
        return new String(payload, UTF_8);
    }
}
