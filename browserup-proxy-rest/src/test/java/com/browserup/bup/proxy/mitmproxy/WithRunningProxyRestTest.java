package com.browserup.bup.proxy.mitmproxy;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.MitmProxyManager;
import com.browserup.bup.proxy.bricks.ProxyResource;
import com.browserup.bup.proxy.guice.ConfigModule;
import com.browserup.bup.proxy.guice.JettyModule;
import com.browserup.bup.util.BrowserUpProxyUtil;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.sitebricks.SitebricksModule;
import org.awaitility.Awaitility;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;

public class WithRunningProxyRestTest {
    private static final Logger LOG = LoggerFactory.getLogger(MitmProxyManager.class);

    protected MitmProxyManager proxyManager;
    protected MitmProxyServer proxy;
    protected Server restServer;

    protected String[] getArgs() {
        return new String[]{"--port", "0"};
    }

    protected int mockServerPort;
    protected int mockServerHttpsPort;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(0).httpsPort(0));

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new ConfigModule(getArgs()), new JettyModule(), new SitebricksModule() {
            @Override
            protected void configureSitebricks() {
                scan(ProxyResource.class.getPackage());
            }
        });

        proxyManager = injector.getInstance(MitmProxyManager.class);

        LOG.debug("Starting BrowserUp Proxy version " + BrowserUpProxyUtil.getVersionString());

        new Thread(() -> startRestServer(injector)).start();

        LOG.debug("Waiting till BrowserUp Rest server is started");

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> restServer != null && restServer.isStarted());

        LOG.debug("BrowserUp Rest server is started successfully");

        LOG.debug("Waiting till BrowserUp Proxy server is started");

        proxy = proxyManager.create(0);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> proxyManager.get().size() > 0);

        LOG.debug("BrowserUp Proxy server is started successfully");

        mockServerPort = wireMockRule.port();
        mockServerHttpsPort = wireMockRule.httpsPort();

        waitForProxyServer();
    }

    private void waitForProxyServer() {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            try {
                HttpURLConnection conn = requestToRestServer("/proxy", Collections.emptyMap());
                int status = conn.getResponseCode();
                conn.disconnect();
                return status == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    protected int getRestServerPort() {
        return ((ServerConnector) restServer.getConnectors()[0]).getLocalPort();
    }

    protected HttpURLConnection requestToRestServer(String path, Map<String, String> queryParams) throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://localhost:" + getRestServerPort() + path);
        if (queryParams != null && !queryParams.isEmpty()) {
            urlBuilder.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) urlBuilder.append("&");
                urlBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                urlBuilder.append("=");
                urlBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                first = false;
            }
        }
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        return conn;
    }

    protected HttpURLConnection sendGetToProxyServer(String path, Map<String, String> queryParams) throws IOException {
        return requestToRestServer(path, queryParams);
    }

    protected HttpURLConnection sendGetToProxyServer(String path) throws IOException {
        return requestToRestServer(path, Collections.emptyMap());
    }

    protected String readResponseBody(HttpURLConnection conn) throws IOException {
        InputStream is;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            is = conn.getErrorStream();
        }
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        }
    }

    protected void requestToTargetServer(String url, String expectedResponse) throws IOException {
        URL targetUrl = new URL("http://localhost:" + mockServerPort + "/" + url);
        Proxy proxyAddr = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", proxy.getPort()));
        HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection(proxyAddr);
        conn.setRequestMethod("GET");
        String body = readResponseBody(conn);
        assertEquals(expectedResponse, body);
        conn.disconnect();
    }

    protected void requestToTargetServer(String url) throws IOException {
        URL targetUrl = new URL("http://localhost:" + mockServerPort + "/" + url);
        Proxy proxyAddr = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", proxy.getPort()));
        HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection(proxyAddr);
        conn.setRequestMethod("GET");
        try { conn.getResponseCode(); } catch (IOException ignored) { }
        conn.disconnect();
    }

    protected static Map<String, String> toStringMap(Object... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), String.valueOf(keyValues[i + 1]));
        }
        return map;
    }

    @After
    public void tearDown() throws Exception {
        LOG.debug("Stopping proxy servers");
        for (MitmProxyServer proxyServer : proxyManager.get()) {
            try {
                proxyManager.delete(proxyServer.getPort());
            } catch (Exception ex) {
                LOG.error("Error while stopping proxy servers", ex);
            }
        }

        if (restServer != null) {
            LOG.debug("Stopping rest proxy server");
            try {
                restServer.stop();
            } catch (Exception ex) {
                LOG.error("Error while stopping rest proxy server", ex);
            }
        }
    }

    private void startRestServer(Injector injector) {
        restServer = injector.getInstance(Server.class);
        GuiceServletContextListener contextListener = new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return injector;
            }
        };
        try {
            restServer.start();
            contextListener.contextInitialized(
                    new ServletContextEvent(((ServletContextHandler) restServer.getHandler()).getServletContext()));
            restServer.join();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void mockTargetServerResponse(String url, String responseBody) {
        mockTargetServerResponse(url, responseBody, 0);
    }

    protected void mockTargetServerResponse(String url, String responseBody, int delayMilliseconds) {
        stubFor(get(urlEqualTo("/" + url)).willReturn(
                aResponse().withStatus(200)
                        .withBody(responseBody)
                        .withHeader("Content-Type", "text/plain")
                        .withFixedDelay(delayMilliseconds)));
    }
}
