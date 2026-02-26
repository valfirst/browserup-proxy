package com.browserup.bup;

import com.browserup.bup.proxy.MitmProxyManager;
import com.browserup.bup.proxy.bricks.ProxyResource;
import com.browserup.bup.proxy.guice.ConfigModule;
import com.browserup.bup.proxy.guice.JettyModule;
import com.browserup.bup.util.BrowserUpProxyUtil;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;

public abstract class WithRunningProxyRestTest {
    private static final Logger LOG = LoggerFactory.getLogger(MitmProxyManager.class);

    protected MitmProxyManager proxyManager;
    protected MitmProxyServer proxy;
    protected Server restServer;

    protected String[] getArgs() {
        return new String[]{"--port", "0"};
    }

    public abstract String getUrlPath();

    public String getFullUrlPath() {
        return "/proxy/" + proxy.getPort() + "/" + getUrlPath();
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

    protected void waitForProxyServer() {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            try {
                URL url = new URL("http://localhost:" + getRestServerPort() + "/proxy");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                conn.disconnect();
                return responseCode == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    protected int getRestServerPort() {
        return ((ServerConnector) restServer.getConnectors()[0]).getLocalPort();
    }

    protected void requestToTargetServer(String url, String expectedResponse) throws IOException {
        URL targetUrl = new URL("http://localhost:" + mockServerPort + "/" + url);
        Proxy httpProxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress("localhost", proxy.getPort()));
        HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection(httpProxy);
        conn.setRequestMethod("GET");
        String body = readResponseBody(conn);
        assertEquals(expectedResponse, body);
        conn.disconnect();
    }

    protected static String readResponseBody(HttpURLConnection conn) throws IOException {
        InputStream is;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            is = conn.getErrorStream();
        }
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
            return sb.toString();
        }
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
        try {
            restServer = injector.getInstance(Server.class);
            GuiceServletContextListener contextListener = new GuiceServletContextListener() {
                @Override
                protected Injector getInjector() {
                    return injector;
                }
            };
            restServer.start();
            contextListener.contextInitialized(
                    new ServletContextEvent(((ServletContextHandler) restServer.getHandler()).getServletContext()));
            try {
                restServer.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void mockTargetServerResponse(String url, String responseBody) {
        mockTargetServerResponse(url, responseBody, 0);
    }

    protected void mockTargetServerResponse(String url, String responseBody, int delayMilliseconds) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(responseBody)
                .withHeader("Content-Type", "text/plain")
                .withFixedDelay(delayMilliseconds);
        stubFor(get(urlEqualTo("/" + url)).willReturn(response));
    }
}
