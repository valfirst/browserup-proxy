package com.browserup.bup.proxy.bricks;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.exception.ProxyExistsException;
import com.browserup.bup.exception.ProxyPortsExhaustedException;
import com.browserup.bup.filters.JavascriptRequestResponseFilter;
import com.browserup.bup.mitmproxy.MitmProxyProcessManager.MitmProxyLoggingLevel;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.bup.proxy.MitmProxyManager;
import com.browserup.bup.proxy.auth.AuthType;
import com.google.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

import de.sstoehr.harreader.model.Har;

@Path("/proxy")
public class ProxyResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyResource.class);

    private final MitmProxyManager proxyManager;

    @Inject
    public ProxyResource(MitmProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProxies() {
        LOG.info("GET /");
        Collection<ProxyDescriptor> proxyList = proxyManager.get().stream()
                .map(proxy -> new ProxyDescriptor(proxy.getPort()))
                .collect(toList());
        return Response.ok(new ProxyListDescriptor(proxyList), MediaType.APPLICATION_JSON_TYPE).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response newProxy(
            @QueryParam("httpProxy") String httpProxy,
            @QueryParam("httpNonProxyHosts") String httpNonProxyHosts,
            @QueryParam("proxyUsername") String proxyUsername,
            @QueryParam("proxyPassword") String proxyPassword,
            @QueryParam("proxyHTTPS") String proxyHTTPS,
            @QueryParam("bindAddress") String paramBindAddr,
            @QueryParam("serverBindAddress") String paramServerBindAddr,
            @QueryParam("port") String portParam,
            @QueryParam("useEcc") String useEccString,
            @QueryParam("mitmProxyLoggingLevel") String loggingLevel,
            @QueryParam("trustAllServers") String trustAllServersString) {
        LOG.info("POST /");
        String systemProxyHost = System.getProperty("http.proxyHost");
        String systemProxyPort = System.getProperty("http.proxyPort");
        String systemNonProxyHosts = System.getProperty("http.nonProxyHosts");

        boolean upstreamProxyHttps = "true".equals(proxyHTTPS);

        Hashtable<String, String> options = new Hashtable<>();

        // If the upstream proxy is specified via query params that should override any default system level proxy.
        String upstreamHttpProxy = null;
        if (httpProxy != null) {
            upstreamHttpProxy = httpProxy;
        } else if ((systemProxyHost != null) && (systemProxyPort != null)) {
            upstreamHttpProxy = String.format("%s:%s", systemProxyHost, systemProxyPort);
        }

        // If the upstream proxy is defined, we should add the non proxy hosts (proxy exceptions) as well.
        List<String> upstreamNonProxyHosts = null;
        if (upstreamHttpProxy != null) {

            // override system non proxy hosts with the provided ones
            if (httpNonProxyHosts != null) {
                upstreamNonProxyHosts = Arrays.asList(httpNonProxyHosts.split("\\|"));
            } else if (systemNonProxyHosts != null) {
                upstreamNonProxyHosts = Arrays.asList(systemNonProxyHosts.split("\\|"));
            }
        }

        Integer paramPort = portParam == null ? null : Integer.parseInt(portParam);

        boolean useEcc = Boolean.parseBoolean(useEccString);

        MitmProxyLoggingLevel level = MitmProxyLoggingLevel.info;
        if (StringUtils.isNotEmpty(loggingLevel)) {
            level = MitmProxyLoggingLevel.valueOf(loggingLevel);
        }

        boolean trustAllServers = Boolean.parseBoolean(trustAllServersString);

        LOG.debug("POST proxy instance on bindAddress `{}` & port `{}` & serverBindAddress `{}`",
                paramBindAddr, paramPort, paramServerBindAddr);
        MitmProxyServer proxy;
        try {
            proxy = proxyManager.create(upstreamHttpProxy, upstreamProxyHttps, upstreamNonProxyHosts, proxyUsername, proxyPassword, paramPort, paramBindAddr, paramServerBindAddr, useEcc, trustAllServers, level);
        } catch (ProxyExistsException ex) {
            return Response.status(455).entity(new ProxyDescriptor(ex.getPort())).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (ProxyPortsExhaustedException ex) {
            return Response.status(456).build();
        } catch (Exception ex) {
            StringWriter s = new StringWriter();
            ex.printStackTrace(new PrintWriter(s));
            return Response.status(550).entity(s.toString()).type(MediaType.TEXT_PLAIN_TYPE).build();
        }
        return Response.ok(new ProxyDescriptor(proxy.getPort()), MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/{port}/har")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHar(@PathParam("port") int port, @QueryParam("cleanHar") String cleanHar) {
        LOG.info("GET /{}/har", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean clean = "true".equals(cleanHar);
        Har har = proxy.getHar(clean);

        return Response.ok(har, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @PUT
    @Path("/{port}/har")
    @Produces(MediaType.APPLICATION_JSON)
    public Response newHar(@PathParam("port") int port,
            @QueryParam("initialPageRef") String initialPageRef,
            @QueryParam("initialPageTitle") String initialPageTitle,
            @QueryParam("captureHeaders") String captureHeaders,
            @QueryParam("captureContent") String captureContent,
            @QueryParam("captureBinaryContent") String captureBinaryContent,
            @QueryParam("captureCookies") String captureCookies) {
        LOG.info("PUT /{}/har", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Har oldHar = proxy.newHar(initialPageRef, initialPageTitle);

        Set<CaptureType> captureTypes = new HashSet<>();
        if (Boolean.parseBoolean(captureHeaders)) {
            captureTypes.addAll(CaptureType.getHeaderCaptureTypes());
        }
        if (Boolean.parseBoolean(captureContent)) {
            captureTypes.addAll(CaptureType.getAllContentCaptureTypes());
        }
        if (Boolean.parseBoolean(captureBinaryContent)) {
            captureTypes.addAll(CaptureType.getBinaryContentCaptureTypes());
        }
        proxy.setHarCaptureTypes(captureTypes);

        if (Boolean.parseBoolean(captureCookies)) {
            proxy.enableHarCaptureTypes(CaptureType.getCookieCaptureTypes());
        }

        if (oldHar != null) {
            return Response.ok(oldHar, MediaType.APPLICATION_JSON_TYPE).build();
        } else {
            return Response.noContent().build();
        }
    }

    @PUT
    @Path("/{port}/har/pageRef")
    public Response setPage(@PathParam("port") int port,
            @QueryParam("pageRef") String pageRef,
            @QueryParam("pageTitle") String pageTitle) {
        LOG.info("PUT /{}/har/pageRef", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.newPage(pageRef, pageTitle);

        return Response.ok().build();
    }

    @POST
    @Path("/{port}/har/commands/endPage")
    public Response endPage(@PathParam("port") int port) {
        LOG.info("POST /{}/commands/endPage", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.endPage();

        return Response.ok().build();
    }

    @POST
    @Path("/{port}/har/commands/endHar")
    public Response endHar(@PathParam("port") int port) {
        LOG.info("POST /{}/commands/endHar", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.endHar();

        return Response.ok().build();
    }

    @GET
    @Path("/{port}/blocklist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBlocklist(@PathParam("port") int port) {
        LOG.info("GET /{}/blocklist", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(proxy.getBlocklist(), MediaType.APPLICATION_JSON_TYPE).build();
    }

    @PUT
    @Path("/{port}/blocklist")
    public Response blocklist(@PathParam("port") int port,
            @QueryParam("regex") String regex,
            @QueryParam("status") String status,
            @QueryParam("method") String method) {
        LOG.info("PUT /{}/blocklist", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        int responseCode = parseResponseCode(status);
        proxy.blocklistRequests(regex, responseCode, method);

        return Response.ok().build();
    }

    @DELETE
    @Path("/{port}/blocklist")
    public Response clearBlocklist(@PathParam("port") int port) {
        LOG.info("DELETE /{}/blocklist", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.clearBlocklist();
        return Response.ok().build();
    }

    @GET
    @Path("/{port}/allowlist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllowlist(@PathParam("port") int port) {
        LOG.info("GET /{}/allowlist", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(proxy.getAllowlistUrls(), MediaType.APPLICATION_JSON_TYPE).build();
    }

    @PUT
    @Path("/{port}/allowlist")
    public Response allowlist(@PathParam("port") int port,
            @QueryParam("regex") String regex,
            @QueryParam("status") String status) {
        LOG.info("PUT /{}/allowlist", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        int responseCode = parseResponseCode(status);
        proxy.allowlistRequests(Arrays.asList(regex.split(",")), responseCode);

        return Response.ok().build();
    }

    @DELETE
    @Path("/{port}/allowlist")
    public Response clearAllowlist(@PathParam("port") int port) {
        LOG.info("DELETE /{}/allowlist", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.disableAllowlist();
        return Response.ok().build();
    }

    @POST
    @Path("/{port}/auth/basic/{domain}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response autoBasicAuth(@PathParam("port") int port, @PathParam("domain") String domain, Map<String, String> credentials) {
        LOG.info("POST /{}/auth/basic/{}", port, domain);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.autoAuthorization(domain, credentials.get("username"), credentials.get("password"), AuthType.BASIC);

        return Response.ok().build();
    }

    @POST
    @Path("/{port}/headers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateHeaders(@PathParam("port") int port, Map<String, String> headers) {
        LOG.info("POST /{}/headers", port);

        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        headers.forEach(proxy::addHeader);
        return Response.ok().build();
    }

    @POST
    @Path("/{port}/filter/request")
    public Response addRequestFilter(@PathParam("port") int port, String body) throws IOException, ScriptException {
        LOG.info("POST /{}/filter/request", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        JavascriptRequestResponseFilter requestResponseFilter = new JavascriptRequestResponseFilter();

        requestResponseFilter.setRequestFilterScript(body);

        proxy.addRequestFilter(requestResponseFilter);

        return Response.ok().build();
    }

    @POST
    @Path("/{port}/filter/response")
    public Response addResponseFilter(@PathParam("port") int port, String body) throws IOException, ScriptException {
        LOG.info("POST /{}/filter/response", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        JavascriptRequestResponseFilter requestResponseFilter = new JavascriptRequestResponseFilter();

        requestResponseFilter.setResponseFilterScript(body);

        proxy.addResponseFilter(requestResponseFilter);

        return Response.ok().build();
    }

    @PUT
    @Path("/{port}/limit")
    public Response limit(@PathParam("port") int port,
            @QueryParam("upstreamKbps") String upstreamKbps,
            @QueryParam("upstreamBps") String upstreamBps,
            @QueryParam("downstreamKbps") String downstreamKbps,
            @QueryParam("downstreamBps") String downstreamBps,
            @QueryParam("latency") String latency,
            @QueryParam("upstreamMaxKB") String upstreamMaxKB,
            @QueryParam("downstreamMaxKB") String downstreamMaxKB,
            @QueryParam("payloadPercentage") String payloadPercentage,
            @QueryParam("maxBitsPerSecond") String maxBitsPerSecond,
            @QueryParam("enable") String enable) {
        LOG.info("PUT /{}/limit", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (upstreamKbps != null) {
            try {
                long upstreamBytesPerSecond = Long.parseLong(upstreamKbps) * 1024;
                proxy.setWriteBandwidthLimit(upstreamBytesPerSecond);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid upstreamKbps value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        if (upstreamBps != null) {
            try {
                proxy.setWriteBandwidthLimit(Integer.parseInt(upstreamBps));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid upstreamBps value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        if (downstreamKbps != null) {
            try {
                long downstreamBytesPerSecond = Long.parseLong(downstreamKbps) * 1024;
                proxy.setReadBandwidthLimit(downstreamBytesPerSecond);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid downstreamKbps value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        if (downstreamBps != null) {
            try {
                proxy.setReadBandwidthLimit(Integer.parseInt(downstreamBps));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid downstreamBps value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        if (latency != null) {
            try {
                proxy.setLatency(Long.parseLong(latency), TimeUnit.MILLISECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid latency value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        if (upstreamMaxKB != null) {
            LOG.warn("upstreamMaxKB no longer supported");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (downstreamMaxKB != null) {
            LOG.warn("downstreamMaxKB no longer supported");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (payloadPercentage != null) {
            LOG.warn("payloadPercentage no longer supported");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (maxBitsPerSecond != null) {
            LOG.warn("maxBitsPerSecond no longer supported");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (enable != null) {
            LOG.warn("enable no longer supported. Limits, if set, will always be enabled.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }

    @PUT
    @Path("/{port}/timeout")
    public Response timeout(@PathParam("port") int port,
            @QueryParam("requestTimeout") String requestTimeout,
            @QueryParam("readTimeout") String readTimeout,
            @QueryParam("connectionTimeout") String connectionTimeout,
            @QueryParam("dnsCacheTimeout") String dnsCacheTimeout) {
        LOG.info("PUT /{}/timeout", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (requestTimeout != null) {
            try {
                proxy.setRequestTimeout(Integer.parseInt(requestTimeout), TimeUnit.MILLISECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid requestTimeout value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        if (readTimeout != null) {
            try {
                proxy.setIdleConnectionTimeout(Integer.parseInt(readTimeout), TimeUnit.MILLISECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid readTimeout value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        if (connectionTimeout != null) {
            try {
                proxy.setConnectTimeout(Integer.parseInt(connectionTimeout), TimeUnit.MILLISECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid connectionTimeout value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        if (dnsCacheTimeout != null) {
            try {
                proxy.getHostNameResolver().setPositiveDNSCacheTimeout(Integer.parseInt(dnsCacheTimeout), TimeUnit.SECONDS);
                proxy.getHostNameResolver().setNegativeDNSCacheTimeout(Integer.parseInt(dnsCacheTimeout), TimeUnit.SECONDS);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid dnsCacheTimeout value");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/{port}")
    public Response delete(@PathParam("port") int port) {
        LOG.info("DELETE /{}", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxyManager.delete(port);
        return Response.ok().build();
    }

    @POST
    @Path("/{port}/hosts")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remapHosts(@PathParam("port") int port, Map<String, String> mappings) {
        LOG.info("POST /{}/hosts", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        mappings.forEach((key, value) -> {
            proxy.getHostNameResolver().remapHost(key, value);
            proxy.getHostNameResolver().setNegativeDNSCacheTimeout(0, TimeUnit.SECONDS);
            proxy.getHostNameResolver().setPositiveDNSCacheTimeout(0, TimeUnit.SECONDS);
            proxy.getHostNameResolver().clearDNSCache();
        });

        return Response.ok().build();
    }


    @PUT
    @Path("/{port}/wait")
    public Response wait(@PathParam("port") int port,
            @QueryParam("quietPeriodInMs") String quietPeriodInMs,
            @QueryParam("timeoutInMs") String timeoutInMs) {
        LOG.info("PUT /{}/wait", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.waitForQuiescence(Long.parseLong(quietPeriodInMs), Long.parseLong(timeoutInMs), TimeUnit.MILLISECONDS);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{port}/dns/cache")
    public Response clearDnsCache(@PathParam("port") int port) {
        LOG.info("DELETE /{}/dns/cache", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        proxy.getHostNameResolver().clearDNSCache();

        return Response.ok().build();
    }

    @PUT
    @Path("/{port}/rewrite")
    public Response rewriteUrl(@PathParam("port") int port,
            @QueryParam("matchRegex") String matchRegex,
            @QueryParam("replace") String replace) {
        LOG.info("PUT /{}/rewrite", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.rewriteUrl(matchRegex, replace);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{port}/rewrite")
    public Response clearRewriteRules(@PathParam("port") int port) {
        LOG.info("DELETE /{}/rewrite", port);
        MitmProxyServer proxy = proxyManager.get(port);
        if (proxy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        proxy.clearRewriteRules();
        return Response.ok().build();
    }

    @PUT
    @Path("/{port}/retry")
    public Response retryCount(@PathParam("port") int port) {
        LOG.warn("/port/retry API is no longer supported");
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    private int parseResponseCode(String response) {
        int responseCode = 200;
        if (response != null) {
            try {
                responseCode = Integer.parseInt(response);
            } catch (NumberFormatException e) {
            }
        }
        return responseCode;
    }

    public static class ProxyDescriptor {
        private int port;

        public ProxyDescriptor() {
        }

        public ProxyDescriptor(int port) {
            this.port = port;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class ProxyListDescriptor {
        private Collection<ProxyDescriptor> proxyList;

        public ProxyListDescriptor() {
        }

        public ProxyListDescriptor(Collection<ProxyDescriptor> proxyList) {
            this.proxyList = proxyList;
        }

        public Collection<ProxyDescriptor> getProxyList() {
            return proxyList;
        }

        public void setProxyList(Collection<ProxyDescriptor> proxyList) {
            this.proxyList = proxyList;
        }
    }
}
