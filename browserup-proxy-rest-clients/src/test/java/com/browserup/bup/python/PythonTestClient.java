package com.browserup.bup.python;

import com.browserup.bup.WithRunningProxyRestTest;
import org.awaitility.Awaitility;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class PythonTestClient extends WithRunningProxyRestTest {
    private static final Logger LOG = LoggerFactory.getLogger(PythonTestClient.class);

    private GenericContainer<?> container;

    @Override
    public String getUrlPath() {
        return "har/entries";
    }

    @After
    public void shutDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    public void connectToProxySuccessfully() throws Exception {
        String urlToCatch = "test";
        String urlNotToCatch = "missing";
        String responseBody = "success";

        mockTargetServerResponse(urlToCatch, responseBody);
        mockTargetServerResponse(urlNotToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);
        requestToTargetServer(urlNotToCatch, responseBody);

        int restPort = ((ServerConnector) restServer.getConnectors()[0]).getLocalPort();
        Testcontainers.exposeHostPorts(restPort);
        Testcontainers.exposeHostPorts(proxy.getPort());

        File dockerfile = new File("./src/test/python/Dockerfile");
        container = new GenericContainer<>(
                new ImageFromDockerfile()
                        .withDockerfile(Paths.get(dockerfile.getPath())))
                .withEnv("PROXY_REST_HOST", "host.testcontainers.internal")
                .withEnv("PROXY_REST_PORT", String.valueOf(restPort))
                .withEnv("PROXY_PORT", String.valueOf(proxy.getPort()));

        container.start();

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !container.isRunning());

        LOG.info("Docker log: " + container.getLogs());

        Assert.assertEquals("Expected python-client container exit code to be 0", 0,
                (int) container.getCurrentContainerInfo().getState().getExitCode());
    }

    @Test
    public void failsToConnectToProxy() throws Exception {
        String urlToCatch = "test";
        String urlNotToCatch = "missing";
        String responseBody = "success";
        int invalidProxyPort = 8;

        mockTargetServerResponse(urlToCatch, responseBody);
        mockTargetServerResponse(urlNotToCatch, responseBody);

        proxyManager.get().iterator().next().newHar();

        requestToTargetServer(urlToCatch, responseBody);
        requestToTargetServer(urlNotToCatch, responseBody);

        int restPort = ((ServerConnector) restServer.getConnectors()[0]).getLocalPort();
        Testcontainers.exposeHostPorts(restPort);
        Testcontainers.exposeHostPorts(proxy.getPort());

        File dockerfile = new File("./src/test/python/Dockerfile");
        container = new GenericContainer<>(
                new ImageFromDockerfile()
                        .withDockerfile(Paths.get(dockerfile.getPath())))
                .withEnv("PROXY_REST_HOST", "host.testcontainers.internal")
                .withEnv("PROXY_REST_PORT", String.valueOf(restPort))
                .withEnv("PROXY_PORT", String.valueOf(invalidProxyPort));

        container.start();

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !container.isRunning());

        LOG.info("Docker log: " + container.getLogs());

        Assert.assertEquals("Expected python-client container exit code to be 1", 1,
                (int) container.getCurrentContainerInfo().getState().getExitCode());
    }
}
