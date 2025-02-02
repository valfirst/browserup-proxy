package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.tuple.Pair.of;

public class ProxyManager {
    private static final String SUCCESSFUL_HEALTH_CHECK_BODY = "OK";

    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyProcessManager mitmProxyManager;

    private long connectionIdleTimeoutSeconds = -1;
    private long dnsResolutionDelayMs = -1;
    private String upstreamProxyCredentials = "";
    private InetSocketAddress upstreamProxyAddress;
    private boolean useHttpsUpstreamProxy = false;
    private List<String> upstreamNonProxyHosts = new ArrayList<>();

    public ProxyManager(AddonsManagerClient addonsManagerClient, MitmProxyProcessManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public void setTrustAll(Boolean trustAll) {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "proxy_manager",
                        "trust_all",
                        List.of(
                            of("trustAll", valueOf(trustAll))
                        ),
                        Void.class);
    }

    public void setConnectionIdleTimeout(Long idleTimeoutSeconds) {
        this.connectionIdleTimeoutSeconds = idleTimeoutSeconds;

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "proxy_manager",
                        "set_connection_timeout_idle",
                        List.of(
                            of("idleSeconds", valueOf(idleTimeoutSeconds))
                        ),
                        Void.class);
    }

    public void setDnsResolvingDelayMs(Long delayMs) {
        this.dnsResolutionDelayMs = delayMs;

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "proxy_manager",
                        "set_dns_resolving_delay_ms",
                        List.of(
                            of("delayMs", valueOf(delayMs))
                        ),
                        Void.class);
    }

    public void setChainedProxyAuthorization(String chainedProxyCredentials) {
        this.upstreamProxyCredentials = chainedProxyCredentials;

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "proxy_manager",
                        "set_upstream_proxy_authorization",
                        List.of(
                            of("credentials", valueOf(chainedProxyCredentials))
                        ),
                        Void.class);
    }

    public String getUpstreamProxyCredentials() {
        return upstreamProxyCredentials;
    }

    public long getConnectionIdleTimeoutSeconds() {
        return connectionIdleTimeoutSeconds;
    }

    public long getDnsResolutionDelayMs() {
        return dnsResolutionDelayMs;
    }

    public void setChainedHttpsProxy(boolean useHttpsUpstreamProxy) {
        this.useHttpsUpstreamProxy = useHttpsUpstreamProxy;
    }

    public void setChainedProxy(InetSocketAddress chainedProxyAddress) {
        this.upstreamProxyAddress = chainedProxyAddress;
    }

    public InetSocketAddress getUpstreamProxyAddress() {
        return upstreamProxyAddress;
    }

    public boolean isUseHttpsUpstreamProxy() {
        return useHttpsUpstreamProxy;
    }

    public List<String> getUpstreamNonProxyHosts() {
        return upstreamNonProxyHosts;
    }

    public void setChainedProxyNonProxyHosts(List<String> upstreamNonProxyHosts) {
        this.upstreamNonProxyHosts = upstreamNonProxyHosts;

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "proxy_manager",
                        "set_chained_proxy_non_proxy_hosts",
                        List.of(
                            of("nonProxyHosts", valueOf(upstreamNonProxyHosts))
                        ),
                        Void.class);
    }

    public Boolean callHealthCheck() {
        String result;
        try {
            result = addonsManagerClient.
                    getRequestToAddonsManager(
                            "proxy_manager",
                            "health_check",
                            List.of(
                                of("nonProxyHosts", valueOf(upstreamNonProxyHosts))
                            ),
                            String.class);
        } catch (Exception ex) {
            return false;
        }
        return SUCCESSFUL_HEALTH_CHECK_BODY.equals(result);

    }
}
