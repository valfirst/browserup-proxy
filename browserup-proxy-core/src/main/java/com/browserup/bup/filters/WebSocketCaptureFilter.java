package com.browserup.bup.filters;

import com.browserup.bup.websocket.WebSocketCapturingChannelHandler;
import com.browserup.bup.websocket.WebSocketListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.browserup.bup.websocket.WebSocketMessage.Direction.CLIENT_TO_SERVER;
import static com.browserup.bup.websocket.WebSocketMessage.Direction.SERVER_TO_CLIENT;

/**
 * HTTP filter that detects a WebSocket upgrade (101 Switching Protocols) and injects
 * {@link WebSocketCapturingChannelHandler} instances into both the client-side and
 * server-side Netty pipelines before LittleProxy replaces them with raw-byte pipe handlers.
 *
 * <p>Because LittleProxy only removes specific, named handlers when switching to WebSocket
 * pipe mode (see {@code ClientToProxyConnection.switchToWebSocketProtocol}), our handlers
 * survive the switch and continue to receive raw bytes from which WebSocket frames are parsed.
 *
 * <p>One instance of this filter is created per HTTP request, so each WebSocket connection gets
 * its own filter (and its own pair of capture handlers).
 *
 * @since 3.3.0
 */
public class WebSocketCaptureFilter extends HttpsAwareFiltersAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketCaptureFilter.class);

    /**
     * LittleProxy handler name used as the insertion point. Both client and server connection
     * pipelines use "handler" as the main handler name.
     */
    private static final String LITTLE_PROXY_MAIN_HANDLER = "handler";

    /** Unique suffix per installed handler pair to avoid name collisions on the same channel. */
    private static final AtomicLong handlerIdCounter = new AtomicLong();

    private final List<WebSocketListener> listeners;

    /** Saved from {@link #proxyToServerConnectionSucceeded}. */
    private volatile ChannelHandlerContext serverCtx;

    /**
     * True when the original request is a WebSocket upgrade (has {@code Connection: Upgrade} and
     * {@code Upgrade: websocket} headers). Evaluated eagerly in the constructor while the headers
     * are still present, because LittleProxy strips them as hop-by-hop headers in
     * {@code modifyRequestHeadersToReflectProxying} before calling {@link #proxyToServerRequest}.
     */
    private final boolean isWebSocketUpgrade;

    public WebSocketCaptureFilter(HttpRequest originalRequest,
                                  ChannelHandlerContext ctx,
                                  List<WebSocketListener> listeners) {
        super(originalRequest, ctx);
        this.listeners = listeners;
        // Must be captured here: LittleProxy mutates the same HttpRequest object later and
        // strips Connection + Upgrade before proxyToServerRequest is called.
        this.isWebSocketUpgrade = ProxyUtils.isSwitchingToWebSocketProtocol(originalRequest);
    }

    /**
     * LittleProxy strips {@code Connection} and {@code Upgrade} from the outgoing request as
     * hop-by-hop headers (RFC 2616 §13.5.1), which breaks the WebSocket upgrade handshake.
     * This method restores them so the origin server sees a valid upgrade request.
     */
    @Override
    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
        if (isWebSocketUpgrade && httpObject instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) httpObject;
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.UPGRADE);
            request.headers().set(HttpHeaderNames.UPGRADE, "websocket");
        }
        return null;
    }

    /**
     * LittleProxy also strips {@code Connection} and {@code Upgrade} from the 101 response before
     * forwarding it to the browser (same hop-by-hop stripping in
     * {@code modifyResponseHeadersToReflectProxying}). Without these headers the browser's
     * WebSocket client rejects the handshake per RFC 6455 §4.1.
     * This method restores them so the browser accepts the upgrade.
     */
    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        if (isWebSocketUpgrade && httpObject instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) httpObject;
            if (response.status() == HttpResponseStatus.SWITCHING_PROTOCOLS) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.UPGRADE);
                response.headers().set(HttpHeaderNames.UPGRADE, "websocket");
            }
        }
        return httpObject;
    }

    @Override
    public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
        this.serverCtx = serverCtx;
        super.proxyToServerConnectionSucceeded(serverCtx);
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) httpObject;
            if (ProxyUtils.isSwitchingToWebSocketProtocol(response)) {
                installCaptureHandlers();
            }
        }
        return super.serverToProxyResponse(httpObject);
    }

    /**
     * Adds a {@link WebSocketCapturingChannelHandler} to both the client channel and the server
     * channel, positioned just before LittleProxy's main handler ("handler"). After LittleProxy
     * switches the pipelines to pipe mode, these handlers remain in place to capture WebSocket
     * frame bytes.
     */
    private void installCaptureHandlers() {
        String wsUrl = buildWebSocketUrl();
        long id = handlerIdCounter.incrementAndGet();

        // Client channel: captures frames sent by the browser (client → server direction)
        ChannelPipeline clientPipeline = ctx.channel().pipeline();
        if (clientPipeline.get(LITTLE_PROXY_MAIN_HANDLER) != null) {
            try {
                clientPipeline.addBefore(
                        LITTLE_PROXY_MAIN_HANDLER,
                        "bup-ws-client-" + id,
                        new WebSocketCapturingChannelHandler(wsUrl, CLIENT_TO_SERVER, listeners));
                log.debug("Installed WebSocket capture handler (client side) for {}", wsUrl);
            } catch (Exception e) {
                log.warn("Failed to install WebSocket capture handler on client pipeline", e);
            }
        }

        // Server channel: captures frames sent by the server (server → client direction)
        ChannelHandlerContext savedServerCtx = this.serverCtx;
        if (savedServerCtx != null) {
            ChannelPipeline serverPipeline = savedServerCtx.channel().pipeline();
            if (serverPipeline.get(LITTLE_PROXY_MAIN_HANDLER) != null) {
                try {
                    serverPipeline.addBefore(
                            LITTLE_PROXY_MAIN_HANDLER,
                            "bup-ws-server-" + id,
                            new WebSocketCapturingChannelHandler(wsUrl, SERVER_TO_CLIENT, listeners));
                    log.debug("Installed WebSocket capture handler (server side) for {}", wsUrl);
                } catch (Exception e) {
                    log.warn("Failed to install WebSocket capture handler on server pipeline", e);
                }
            }
        }
    }

    /**
     * Returns the WebSocket URL derived from the HTTP upgrade request.
     * Converts {@code http://} to {@code ws://} and {@code https://} to {@code wss://}.
     */
    String buildWebSocketUrl() {
        String httpUrl = getFullUrl(originalRequest);
        if (httpUrl.startsWith("https://")) {
            return "wss://" + httpUrl.substring("https://".length());
        }
        if (httpUrl.startsWith("http://")) {
            return "ws://" + httpUrl.substring("http://".length());
        }
        return httpUrl;
    }
}
