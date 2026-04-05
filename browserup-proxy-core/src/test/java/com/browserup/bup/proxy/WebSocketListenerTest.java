package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.WebSocketFrame;
import com.browserup.bup.filters.WebSocketListener;
import com.browserup.bup.util.HttpMessageInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WebSocket frame observation via {@link WebSocketListener}.
 */
class WebSocketListenerTest {
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Pattern SEC_WS_KEY_PATTERN =
            Pattern.compile("Sec-WebSocket-Key:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private BrowserUpProxy proxy;
    private ServerSocket wsServerSocket;
    private Thread wsServerThread;

    @BeforeEach
    void setUp() throws IOException {
        wsServerSocket = new ServerSocket(0);
        wsServerThread = new Thread(this::runEchoServer, "ws-echo-server");
        wsServerThread.setDaemon(true);
        wsServerThread.start();

        proxy = new BrowserUpProxyServer();
        proxy.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (proxy != null) proxy.abort();
        wsServerSocket.close();
    }

    @Test
    void listenerReceivesClientAndServerFrames() throws Exception {
        List<WebSocketFrame> captured = new CopyOnWriteArrayList<>();
        List<HttpMessageInfo> capturedInfo = new CopyOnWriteArrayList<>();
        CountDownLatch serverFrameLatch = new CountDownLatch(1);
        CountDownLatch clientFrameLatch = new CountDownLatch(1);

        proxy.addWebSocketListener((frame, messageInfo) -> {
            captured.add(frame);
            capturedInfo.add(messageInfo);
            if (frame.isFromClient()) clientFrameLatch.countDown();
            else serverFrameLatch.countDown();
        });

        int serverPort = wsServerSocket.getLocalPort();

        try (Socket socket = new Socket("localhost", proxy.getPort())) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Perform the WebSocket upgrade handshake
            String key = "dGhlIHNhbXBsZSBub25jZQ==";
            String upgradeRequest = "GET http://localhost:" + serverPort + "/ws HTTP/1.1\r\n"
                    + "Host: localhost:" + serverPort + "\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: " + key + "\r\n"
                    + "Sec-WebSocket-Version: 13\r\n"
                    + "\r\n";
            out.write(upgradeRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String response = readHttpHeaders(in);
            assertTrue(response.startsWith("HTTP/1.1 101"), "Expected 101 but got: " + response.split("\r\n")[0]);

            // Send a text frame
            out.write(buildMaskedTextFrame("hello"));
            out.flush();

            // Read the echo back (to ensure the round-trip completed)
            readWebSocketTextFrame(in);

            assertTrue(clientFrameLatch.await(5, TimeUnit.SECONDS), "Listener should receive client frame");
            assertTrue(serverFrameLatch.await(5, TimeUnit.SECONDS), "Listener should receive server frame");
        }

        // Verify client-to-server frame
        WebSocketFrame clientFrame = captured.stream()
                .filter(WebSocketFrame::isFromClient).findFirst().orElse(null);
        assertNotNull(clientFrame, "Should have captured a client frame");
        assertEquals(WebSocketFrame.Opcode.TEXT, clientFrame.getOpcode());
        assertEquals("hello", clientFrame.getTextContent());
        assertTrue(clientFrame.isFromClient());
        assertFalse(clientFrame.isFromServer());

        // Verify server-to-client frame (echo)
        WebSocketFrame serverFrame = captured.stream()
                .filter(WebSocketFrame::isFromServer).findFirst().orElse(null);
        assertNotNull(serverFrame, "Should have captured a server frame");
        assertEquals(WebSocketFrame.Opcode.TEXT, serverFrame.getOpcode());
        assertEquals("hello", serverFrame.getTextContent());
        assertTrue(serverFrame.isFromServer());

        // Verify message info carries the upgrade URL
        assertFalse(capturedInfo.isEmpty());
        assertTrue(capturedInfo.get(0).getUrl().contains("/ws"),
                "MessageInfo URL should reference the WebSocket endpoint");
    }

    @Test
    void listenerCanBeRemoved() throws Exception {
        List<WebSocketFrame> captured = new CopyOnWriteArrayList<>();
        CountDownLatch initialLatch = new CountDownLatch(1);

        WebSocketListener listener = (frame, info) -> {
            captured.add(frame);
            initialLatch.countDown();
        };
        proxy.addWebSocketListener(listener);
        proxy.removeWebSocketListener(listener);

        int serverPort = wsServerSocket.getLocalPort();

        try (Socket socket = new Socket("localhost", proxy.getPort())) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            String key = "dGhlIHNhbXBsZSBub25jZQ==";
            out.write(("GET http://localhost:" + serverPort + "/ws HTTP/1.1\r\n"
                    + "Host: localhost:" + serverPort + "\r\n"
                    + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: " + key + "\r\nSec-WebSocket-Version: 13\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.flush();

            readHttpHeaders(in);

            out.write(buildMaskedTextFrame("test"));
            out.flush();
            readWebSocketTextFrame(in);
        }

        assertFalse(initialLatch.await(500, TimeUnit.MILLISECONDS),
                "Removed listener should not have been called");
        assertTrue(captured.isEmpty(), "Removed listener should not capture any frames");
    }

    // ---- Echo server ----

    private void runEchoServer() {
        try (Socket client = wsServerSocket.accept()) {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            String request = readHttpHeaders(in);
            Matcher m = SEC_WS_KEY_PATTERN.matcher(request);
            if (!m.find()) return;
            String acceptKey = computeAcceptKey(m.group(1).trim());

            out.write(("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.flush();

            echoFrame(in, out);
        } catch (Exception ignored) {}
    }

    // ---- WebSocket helpers ----

    private static String readHttpHeaders(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int b = in.read();
            if (b == -1) break;
            sb.append((char) b);
            if (sb.toString().endsWith("\r\n\r\n")) break;
        }
        return sb.toString();
    }

    private static String computeAcceptKey(String clientKey) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] hash = sha1.digest((clientKey + WS_GUID).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

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

    private static String readWebSocketTextFrame(InputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        boolean masked = (b2 & 0x80) != 0;
        int length = b2 & 0x7F;
        byte[] maskKey = null;
        if (masked) { maskKey = new byte[]{(byte)in.read(),(byte)in.read(),(byte)in.read(),(byte)in.read()}; }
        byte[] payload = new byte[length];
        int off = 0;
        while (off < length) { int r = in.read(payload, off, length - off); if (r == -1) break; off += r; }
        if (masked && maskKey != null) for (int i = 0; i < payload.length; i++) payload[i] ^= maskKey[i % 4];
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static void echoFrame(InputStream in, OutputStream out) throws IOException {
        int b0 = in.read(), b1 = in.read();
        boolean masked = (b1 & 0x80) != 0;
        int length = b1 & 0x7F;
        byte[] maskKey = new byte[4];
        if (masked) { for (int i = 0; i < 4; i++) maskKey[i] = (byte) in.read(); }
        byte[] payload = new byte[length];
        int off = 0;
        while (off < length) { int r = in.read(payload, off, length - off); if (r == -1) break; off += r; }
        if (masked) for (int i = 0; i < payload.length; i++) payload[i] ^= maskKey[i % 4];
        out.write(b0); out.write(length); out.write(payload); out.flush();
    }
}
