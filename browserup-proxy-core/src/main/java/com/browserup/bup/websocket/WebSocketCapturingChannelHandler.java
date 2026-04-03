package com.browserup.bup.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Netty channel handler injected into the LittleProxy pipeline after a WebSocket upgrade (101).
 * It sniffs raw WebSocket frames from the byte stream, notifies registered listeners, and passes
 * the unmodified bytes downstream so the proxy pipe is not affected.
 * <p>
 * One instance handles one direction: either client→server (fromClient=true) or server→client
 * (fromClient=false). LittleProxy uses {@code ProxyConnectionPipeHandler} to forward raw bytes
 * after the upgrade, so this handler must forward {@link ByteBuf} objects unchanged with
 * {@code ctx.fireChannelRead}.
 *
 * @since 3.3.0
 */
public class WebSocketCapturingChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketCapturingChannelHandler.class);

    /**
     * WebSocket opcodes
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6455">RFC 6455</a>
     */
    private static final class OpCode {
        private static final int CONTINUATION = 0x0;
        private static final int TEXT = 0x1;
        private static final int BINARY = 0x2;
        private static final int CLOSE = 0x8;
        private static final int PING = 0x9;
        private static final int PONG = 0xA;
    }

    private static final class PayloadLength {
        private static final int L126 = 126;
        private static final int L127 = 127;
    }

    private final String url;
    private final WebSocketMessage.Direction direction;
    private final List<WebSocketListener> listeners;

    /** Raw-byte accumulation buffer: holds bytes until a full frame can be parsed. */
    private ByteBuf accumulation;

    /** Message type of the current fragmented message being reassembled (null = none in progress). */
    private WebSocketMessage.@Nullable Type fragmentMessageType = null;
    /** Payload accumulator for fragmented messages. */
    private ByteArrayOutputStream fragmentBuffer;

    public WebSocketCapturingChannelHandler(String url,
                                            WebSocketMessage.Direction direction,
                                            List<WebSocketListener> listeners) {
        this.url = url;
        this.direction = direction;
        this.listeners = listeners;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        accumulation = ctx.alloc().buffer(512);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        releaseAccumulation();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        releaseAccumulation();
        ctx.fireChannelInactive();
    }

    private void releaseAccumulation() {
        if (accumulation != null) {
            accumulation.release();
            accumulation = null;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf && accumulation != null) {
            ByteBuf buf = (ByteBuf) msg;
            // Copy incoming bytes into our accumulator without advancing the original reader index
            accumulation.writeBytes(buf, buf.readerIndex(), buf.readableBytes());
            parseFrames();
        }
        // Always forward the original message unchanged so the pipe handler works correctly
        ctx.fireChannelRead(msg);
    }

    /**
     * Tries to parse as many complete WebSocket frames as possible from the accumulation buffer.
     * Leaves incomplete data in the buffer for the next {@link #channelRead} call.
     */
    private void parseFrames() {
        while (accumulation.readableBytes() >= 2) {
            int startIndex = accumulation.readerIndex();

            byte byte0 = accumulation.getByte(startIndex);
            byte byte1 = accumulation.getByte(startIndex + 1);

            boolean finalFragment = (byte0 & 0x80) != 0;
            int opcode = byte0 & 0x0F;
            boolean masked = (byte1 & 0x80) != 0;
            int payloadLen7 = byte1 & 0x7F;

            // Header size: 2 base bytes + optional extended length + optional mask key
            int extLenBytes = getExtendedLength(payloadLen7);
            int headerSize = 2 + extLenBytes + (masked ? 4 : 0);

            if (accumulation.readableBytes() < headerSize) {
                break; // need more bytes to read the full header
            }

            long payloadLength = decodeActualPayloadLength(payloadLen7, startIndex);
            if (payloadLength < 0 || payloadLength > Integer.MAX_VALUE) {
                log.warn("WebSocket frame has unsupported payload length {}; skipping capture for this connection", payloadLength);
                releaseAccumulation();
                return;
            }

            long totalFrameSize = headerSize + payloadLength;
            if (accumulation.readableBytes() < totalFrameSize) {
                break; // frame is not yet complete
            }

            byte[] payload = extractPayloadBytes((int) payloadLength, masked, startIndex, extLenBytes, headerSize);

            // Advance past this frame
            accumulation.readerIndex(startIndex + (int) totalFrameSize);

            dispatchFrame(opcode, finalFragment, payload);
        }

        // Reclaim memory for consumed bytes
        if (accumulation != null) {
            accumulation.discardReadBytes();
        }
    }

    private int getExtendedLength(int payloadLen7) {
        switch (payloadLen7) {
            case PayloadLength.L126:
                return 2;
            case PayloadLength.L127:
                return 8;
            default:
                return 0;
        }
    }

    /**
     * Payload length:  7 bits, 7+16 bits, or 7+64 bits.
     * @param payloadLen7 if 0-125, that is the payload length.
     *                    If 126, the following 2 bytes interpreted as a 16-bit unsigned integer are the payload length.
     *                    If 127, the following 8 bytes interpreted as a 64-bit unsigned integer are the payload length.
     *
     * @return The length of the "Payload data", in bytes
     */
    private long decodeActualPayloadLength(int payloadLen7, int startIndex) {
        switch (payloadLen7) {
            case PayloadLength.L126:
                return accumulation.getUnsignedShort(startIndex + 2);
            case PayloadLength.L127:
                return accumulation.getLong(startIndex + 2);
            default:
                return payloadLen7;
        }
    }

    /**
     * Extract payload bytes (unmasked)
     */
    private byte @NonNull [] extractPayloadBytes(int payloadLength, boolean masked, int startIndex, int extLenBytes, int headerSize) {
        byte[] payload = new byte[payloadLength];
        if (masked) {
            int maskOffset = startIndex + 2 + extLenBytes;
            byte[] maskKey = new byte[4];
            accumulation.getBytes(maskOffset, maskKey);

            int payloadOffset = maskOffset + 4;
            accumulation.getBytes(payloadOffset, payload);
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i & 3];
            }
        } else {
            accumulation.getBytes(startIndex + headerSize, payload);
        }
        return payload;
    }

    /**
     * Handles a single decoded frame. Reassembles fragmented messages before notifying listeners.
     */
    private void dispatchFrame(int opcode, boolean finalFragment, byte[] payload) {
        switch (opcode) {
            case OpCode.CLOSE:
                deliver(WebSocketMessage.Type.CLOSE, payload);
                break;
            case OpCode.PING:
                deliver(WebSocketMessage.Type.PING, payload);
                break;
            case OpCode.PONG:
                deliver(WebSocketMessage.Type.PONG, payload);
                break;
            case OpCode.TEXT:
                readFragment(WebSocketMessage.Type.TEXT, finalFragment, payload);
                break;
            case OpCode.BINARY:
                readFragment(WebSocketMessage.Type.BINARY, finalFragment, payload);
                break;
            case OpCode.CONTINUATION:
                readContinuationFragment(finalFragment, payload);
                break;
            default:
                log.debug("Ignoring unknown WebSocket opcode 0x{}", Integer.toHexString(opcode));
                break;
        }
    }

    private void readFragment(WebSocketMessage.Type type, boolean finalFragment, byte[] payload) {
        if (finalFragment) {
            // Single complete frame
            deliver(type, payload);
        } else {
            // First fragment of a multi-frame message
            fragmentMessageType = type;
            fragmentBuffer = new ByteArrayOutputStream();
            fragmentBuffer.write(payload, 0, payload.length);
        }
    }

    private void readContinuationFragment(boolean finalFragment, byte[] payload) {
        if (fragmentBuffer == null) {
            log.warn("Received continuation frame without a first frame: '{}'", new String(payload, UTF_8));
        }
        else {
            fragmentBuffer.write(payload, 0, payload.length);
            if (finalFragment) {
                // Final fragment: reassemble and deliver
                deliver(fragmentMessageType, fragmentBuffer.toByteArray());
                fragmentMessageType = null;
                fragmentBuffer = null;
            }
        }
    }

    private void deliver(WebSocketMessage.Type type, byte[] content) {
        if (!listeners.isEmpty()) {
            WebSocketMessage message = new WebSocketMessage(url, direction, type, content);

            for (WebSocketListener listener : listeners) {
                try {
                    listener.onMessage(message);
                } catch (Exception e) {
                    log.warn("WebSocketListener threw an exception", e);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("Exception in WebSocket capture handler", cause);
        ctx.fireExceptionCaught(cause);
    }
}
