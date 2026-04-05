package com.browserup.bup.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
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
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that WebSocket connections can be established through the proxy.
 */
class WebSocketTest {
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Pattern SEC_WS_KEY_PATTERN =
            Pattern.compile("Sec-WebSocket-Key:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private BrowserUpProxy proxy;
    private ServerSocket wsServerSocket;
    private Thread wsServerThread;
    private final AtomicBoolean serverReceivedUpgrade = new AtomicBoolean(false);
    private final CountDownLatch serverHandshakeDone = new CountDownLatch(1);

    @BeforeEach
    void setUp() throws IOException {
        wsServerSocket = new ServerSocket(0);
        wsServerThread = new Thread(this::runWebSocketServer, "ws-test-server");
        wsServerThread.setDaemon(true);
        wsServerThread.start();

        proxy = new BrowserUpProxyServer();
        proxy.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (proxy != null) {
            proxy.abort();
        }
        wsServerSocket.close();
    }

    @Test
    void webSocketUpgradeSucceedsThroughProxy() throws Exception {
        int serverPort = wsServerSocket.getLocalPort();
        int proxyPort = proxy.getPort();

        try (Socket socket = new Socket("localhost", proxyPort)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            String key = "dGhlIHNhbXBsZSBub25jZQ==";
            String request = "GET http://localhost:" + serverPort + "/ws HTTP/1.1\r\n"
                    + "Host: localhost:" + serverPort + "\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: " + key + "\r\n"
                    + "Sec-WebSocket-Version: 13\r\n"
                    + "\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Read response headers
            String response = readHttpHeaders(in);

            assertTrue(serverReceivedUpgrade.get(),
                    "Server should have received the WebSocket upgrade request with Upgrade header");
            assertTrue(response.startsWith("HTTP/1.1 101"),
                    "Expected 101 Switching Protocols but got: " + response.split("\r\n")[0]);
            assertTrue(response.toLowerCase().contains("upgrade: websocket"),
                    "Expected Upgrade: websocket in 101 response");

            assertTrue(serverHandshakeDone.await(5, TimeUnit.SECONDS),
                    "Server should have completed the WebSocket handshake");

            // Send a WebSocket text frame ("hello") and read the echo back
            byte[] frame = buildTextFrame("hello");
            out.write(frame);
            out.flush();

            String echo = readWebSocketTextFrame(in);
            assertEquals("hello", echo, "Expected echo of the sent WebSocket message");
        }
    }

    private void runWebSocketServer() {
        try (Socket client = wsServerSocket.accept()) {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            // Read upgrade request
            String request = readHttpHeaders(in);

            Matcher matcher = SEC_WS_KEY_PATTERN.matcher(request);
            if (!matcher.find()) {
                return;
            }
            String clientKey = matcher.group(1).trim();

            boolean hasUpgradeHeader = request.toLowerCase().contains("upgrade: websocket");
            serverReceivedUpgrade.set(hasUpgradeHeader);

            // Compute Sec-WebSocket-Accept
            String acceptKey = computeAcceptKey(clientKey);

            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + acceptKey + "\r\n"
                    + "\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
            serverHandshakeDone.countDown();

            // Echo back the first WebSocket frame (client frames are masked)
            echoWebSocketFrame(in, out);
        } catch (Exception e) {
            // server closed or test ended
        }
    }

    private static String readHttpHeaders(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int prev = -1;
        // Read until \r\n\r\n
        while (true) {
            int b = in.read();
            if (b == -1) break;
            sb.append((char) b);
            String s = sb.toString();
            if (s.endsWith("\r\n\r\n")) break;
        }
        return sb.toString();
    }

    private static String computeAcceptKey(String clientKey) throws Exception {
        String combined = clientKey + WS_GUID;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] sha1 = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sha1);
    }

    /**
     * Builds a masked WebSocket text frame for client-to-server communication.
     */
    private static byte[] buildTextFrame(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        byte[] mask = {0x37, 0x42, 0x11, 0x22};
        byte[] frame = new byte[2 + 4 + payload.length];
        frame[0] = (byte) 0x81; // FIN + text opcode
        frame[1] = (byte) (0x80 | payload.length); // MASK + length
        frame[2] = mask[0];
        frame[3] = mask[1];
        frame[4] = mask[2];
        frame[5] = mask[3];
        for (int i = 0; i < payload.length; i++) {
            frame[6 + i] = (byte) (payload[i] ^ mask[i % 4]);
        }
        return frame;
    }

    /**
     * Reads a masked WebSocket text frame, unmasks it, and returns the text.
     */
    private static String readWebSocketTextFrame(InputStream in) throws IOException {
        int b0 = in.read(); // FIN + opcode
        int b1 = in.read(); // MASK + length
        boolean masked = (b1 & 0x80) != 0;
        int length = b1 & 0x7F;

        byte[] maskKey = null;
        if (masked) {
            maskKey = new byte[4];
            for (int i = 0; i < 4; i++) {
                maskKey[i] = (byte) in.read();
            }
        }

        byte[] payload = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(payload, offset, length - offset);
            if (read == -1) break;
            offset += read;
        }

        if (masked && maskKey != null) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i % 4];
            }
        }

        return new String(payload, StandardCharsets.UTF_8);
    }

    /**
     * Reads one masked WebSocket frame from the client and sends it back unmasked.
     */
    private static void echoWebSocketFrame(InputStream in, OutputStream out) throws IOException {
        int b0 = in.read();
        int b1 = in.read();
        boolean masked = (b1 & 0x80) != 0;
        int length = b1 & 0x7F;

        byte[] maskKey = new byte[4];
        if (masked) {
            for (int i = 0; i < 4; i++) {
                maskKey[i] = (byte) in.read();
            }
        }

        byte[] payload = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(payload, offset, length - offset);
            if (read == -1) break;
            offset += read;
        }

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i % 4];
            }
        }

        // Send back unmasked (server-to-client frames are not masked)
        out.write(b0);  // same opcode
        out.write(length); // no mask bit
        out.write(payload);
        out.flush();
    }
}
