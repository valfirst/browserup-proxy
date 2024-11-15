package com.browserup.bup.mitmproxy.management;

import com.browserup.bup.mitmproxy.MitmProxyProcessManager;
import com.browserup.bup.proxy.CaptureType;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarLog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.tuple.Pair.of;

public class HarCaptureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarCaptureManager.class);

    private final AddonsManagerClient addonsManagerClient;
    private final MitmProxyProcessManager mitmProxyManager;
    private EnumSet<CaptureType> lastCaptureTypes = EnumSet.noneOf(CaptureType.class);

    public HarCaptureManager(AddonsManagerClient addonsManagerClient, MitmProxyProcessManager mitmProxyManager) {
        this.addonsManagerClient = addonsManagerClient;
        this.mitmProxyManager = mitmProxyManager;
    }

    public Har getHar() {
        return getHar(false);
    }

    public Har getHar(Boolean cleanHar) {
        if (!mitmProxyManager.isRunning()) return null;

        HarResponse response = addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "get_har",
                        List.of(
                            of("cleanHar", valueOf(cleanHar))
                        ),
                        HarResponse.class);
        LOGGER.info("Parsing HAR from file: {}", response.path);
        return parseHar(response.path);
    }

    public Har newHar() {
        return newHar(null, null);
    }

    public Har newHar(String pageRef) {
        return newHar(pageRef, null);
    }

    public Har newHar(String pageRef, String pageTitle) {
        if (!mitmProxyManager.isRunning()) return null;

        HarResponse response = addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "new_har",
                        List.of(
                            of("pageRef", pageRef),
                            of("pageTitle", pageTitle)
                        ),
                        HarResponse.class);
        return parseHar(response.path);
    }

    public Har endHar() {
        if (!mitmProxyManager.isRunning()) return null;

        HarResponse response = addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "end_har",
                        emptyList(),
                        HarResponse.class);
        return parseHar(response.path);
    }

    public Har newPage() {
        return newPage(null, null);
    }

    public Har newPage(String pageRef) {
        return newPage(pageRef, null);
    }

    public Har newPage(String pageRef, String pageTitle) {
        if (!mitmProxyManager.isRunning()) return null;

        HarResponse response = addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "new_page",
                        List.of(
                            of("pageRef", valueOf(pageRef)),
                            of("pageTitle", valueOf(pageTitle))
                        ),
                        HarResponse.class);
        return parseHar(response.path);
    }

    public void endPage() {
        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "end_page",
                        emptyList(),
                        Void.class);
    }

    private Har parseHar(String filePath) {
        File harFile = new File(filePath);

        try {
            Har har = new ObjectMapper().readerFor(Har.class).readValue(harFile);

            // mitmproxy writes HAR which does not follow specification: some mandatory fields are not initialized
            // thus it is needed to go through the object and patch to make sure it matches specification
            Optional.ofNullable(har).map(Har::getLog).map(HarLog::getEntries).ifPresent(es -> es.forEach(e -> {
                de.sstoehr.harreader.model.HarResponse response = e.getResponse();
                if (response.getRedirectURL() == null) {
                    response.setRedirectURL("");
                }
            }));

            return har;
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read HAR file: " + harFile.getAbsolutePath(), e);
        }
    }

    public void setHarCaptureTypes(EnumSet<CaptureType> captureTypes) {
        lastCaptureTypes = captureTypes;

        if (!mitmProxyManager.isRunning()) return;

        addonsManagerClient.
                getRequestToAddonsManager(
                        "har",
                        "set_har_capture_types",
                        List.of(
                                of("captureTypes", valueOf(captureTypes))
                        ),
                        Void.class);
    }

    public EnumSet<CaptureType> getLastCaptureTypes() {
        return lastCaptureTypes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HarResponse {
        private String path;

        public HarResponse() {}

        public HarResponse(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
