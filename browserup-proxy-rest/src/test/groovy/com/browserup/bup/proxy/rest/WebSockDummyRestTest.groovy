/*
 * Modifications Copyright (c) 2019 BrowserUp, Inc.
 */

package com.browserup.bup.proxy.rest

import com.neovisionaries.ws.client.ProxySettings
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import groovyx.net.http.Method
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.HttpProxy
import org.eclipse.jetty.client.ProxyConfiguration
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.After
import org.junit.Test
import spark.Spark

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class WebSockDummyRestTest extends WithRunningProxyRestTest {
    private static final String URL_PATH = 'har/entries'

    @Test
    void test() {
        Spark.initExceptionHandler({e ->
            System.out.println("Uh-oh")
        })
        Spark.webSocket("/echo", EchoWebSocketServerHandler.class)
        //Spark.get("/hello", { req, res -> "Hello World" })

        Spark.init()

        proxyManager.get()[0].newHar()

//        targetServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
//            uri.path = "/hello"
//            response.failure = { resp, reader ->
//                System.out.println()
//            }
//            response.success = { resp, reader ->
//                System.out.println()
//            }
//        }

//        WebSocketFactory factory = new WebSocketFactory()
//        ProxySettings settings = factory.getProxySettings()
//        settings.setServer("http://localhost:${proxy.port}")
//
//        com.neovisionaries.ws.client.WebSocket ws = factory.createSocket("ws://localhost:4567/echo")
//
//        ws.addListener(new WebSocketAdapter() {
//            @Override
//            void onTextMessage(com.neovisionaries.ws.client.WebSocket websocket, String text) throws Exception {
//                System.out.println()
//            }
//        })
//
//        ws.connect();
//
//        ws.sendText("Hello.")




        HttpClient httpClient = new HttpClient()

        ProxyConfiguration proxyConfig = httpClient.getProxyConfiguration()
        HttpProxy proxy1 = new HttpProxy("localhost", proxy.port)
       // proxyConfig.getProxies().add(proxy1)

        httpClient.start()
        WebSocketClient client= new WebSocketClient(httpClient)


        client.setConnectTimeout(4000)
        client.setMaxIdleTimeout(4000)
        client.setMaxBinaryMessageBufferSize(1000000)
        client.start()
        
        try {

            String dest = "ws://localhost:4567/echo"
            ClientWebSocketHandler socket = new ClientWebSocketHandler()
            URI echoUri = new URI(dest)

            ClientUpgradeRequest request = new ClientUpgradeRequest();

            Future<Session> fut = client.connect(socket, echoUri, request)
            def res = fut.get(60000, TimeUnit.SECONDS)
            System.out.println()

        } catch (Throwable t) {
            t.printStackTrace();
        }

        //ws.disconnect()

        proxyRestServerClient.request(Method.GET, ContentType.TEXT_PLAIN) { req ->
            uri.query = [urlPattern: '.*']
            uri.path = "/proxy/${proxy.port}/har/entries"
            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
            response.success = { resp, reader ->
                assertTrue(true)
            }
        }

        Spark.awaitStop()


    }

    @After
    void tearDown() {
        Spark.stop()
    }

    @WebSocket
    public static class EchoWebSocketServerHandler {

        // Store sessions if you want to, for example, broadcast a message to all users
        private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

        @OnWebSocketConnect
        public void connected(Session session) {
            sessions.add(session);
        }

        @OnWebSocketClose
        public void closed(Session session, int statusCode, String reason) {
            sessions.remove(session);
        }

        @OnWebSocketMessage
        public void handleTextMessage(Session session, String message) throws IOException {
            System.out.println("New Text Message Received");
            session.getRemote().sendString(message + " back");
        }

        @OnWebSocketMessage
        public void handleBinaryMessage(Session session, byte[] buffer, int offset, int length) throws IOException {
            System.out.println("New Binary Message Received");
            session.getRemote().sendBytes(ByteBuffer.wrap(buffer));
        }

    }

    @WebSocket
    public static class ClientWebSocketHandler {

        private final CountDownLatch closeLatch = new CountDownLatch(1);

        @OnWebSocketConnect
        public void onConnect(Session session) throws IOException {

            System.out.println("Sending message: Hello server");
            session.getRemote().sendString("Hello server");
        }

        @OnWebSocketMessage
        public void onMessage(String message) {
            System.out.println("Message from Server: " + message);
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            System.out.println("WebSocket Closed. Code:" + statusCode);
        }

        public boolean awaitClose(int duration, TimeUnit unit)
                throws InterruptedException {
            return this.closeLatch.await(duration, unit);
        }

    }
}
