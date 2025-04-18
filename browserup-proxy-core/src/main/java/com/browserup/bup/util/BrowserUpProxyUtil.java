package com.browserup.bup.util;

import com.google.common.base.Suppliers;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import com.browserup.bup.mitm.exception.UncheckedIOException;

import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarLog;
import de.sstoehr.harreader.model.HarPage;
import de.sstoehr.harreader.model.HarTiming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * General utility class for functionality and classes used mostly internally by BrowserUp Proxy.
 */
public class BrowserUpProxyUtil {
    private static final Logger log = LoggerFactory.getLogger(BrowserUpProxyUtil.class);

    /**
     * Classpath resource containing this build's version string.
     */
    private static final String VERSION_CLASSPATH_RESOURCE = "/com/browserup/bup/version";

    /**
     * Default value if the version string cannot be read.
     */
    private static final String UNKNOWN_VERSION_STRING = "UNKNOWN-VERSION";

    /**
     * Singleton version string loader.
     */
    private static final Supplier<String> version = Suppliers.memoize(BrowserUpProxyUtil::readVersionFileOnClasspath);

    /**
     * Copies {@link de.sstoehr.harreader.model.HarEntry} and {@link HarPage} references from the specified har to a
     * new har copy, up to and including the specified pageRef. Does not perform a "deep copy", so any subsequent
     * modification to the entries or pages will be reflected in the copied har.
     *
     * @param har existing har to copy
     * @param pageRef last page ID to copy
     * @return copy of a {@link Har} with entries and pages from the original har, or null if the input har is null
     */
    public static Har copyHarThroughPageRef(Har har, String pageRef) {
        if (har == null) {
            return null;
        }

        if (har.getLog() == null) {
            return new Har();
        }

        // collect the page refs that need to be copied to new har copy.
        Set<String> pageRefsToCopy = new HashSet<>();

        for (HarPage page : har.getLog().getPages()) {
            pageRefsToCopy.add(page.getId());

            if (pageRef.equals(page.getId())) {
                break;
            }
        }

        HarLog logCopy = new HarLog();

        // copy every entry and page in the HarLog that matches a pageRefToCopy. since getEntries() and getPages() return
        // lists, we are guaranteed that we will iterate through the pages and entries in the proper order
        har.getLog().getEntries().stream()
                .filter(entry -> pageRefsToCopy.contains(entry.getPageref()))
                .forEach(entry -> logCopy.getEntries().add(entry));

        har.getLog().getPages().stream()
                .filter(page -> pageRefsToCopy.contains(page.getId()))
                .forEach(page -> logCopy.getPages().add(page));

        Har harCopy = new Har();
        harCopy.setLog(logCopy);

        return harCopy;
    }

    /**
     * Returns the version of BrowserUp Proxy, e.g. "2.1.0".
     *
     * @return BUP version string
     */
    public static String getVersionString() {
        return version.get();
    }

    /**
     * Reads the version of this build from the classpath resource specified by {@link #VERSION_CLASSPATH_RESOURCE}.
     *
     * @return version string from the classpath version resource
     */
    private static String readVersionFileOnClasspath() {
        String versionString;
        try {
            versionString = ClasspathResourceUtil.classpathResourceToString(VERSION_CLASSPATH_RESOURCE, StandardCharsets.UTF_8);
        } catch (UncheckedIOException e) {
            log.debug("Unable to load version from classpath resource: {}", VERSION_CLASSPATH_RESOURCE, e);
            return UNKNOWN_VERSION_STRING;
        }

        if (versionString.isEmpty()) {
            log.debug("Version file on classpath was empty or could not be read. Resource: {}", VERSION_CLASSPATH_RESOURCE);
            return UNKNOWN_VERSION_STRING;
        }

        return versionString;
    }

    public static InetSocketAddress inetSocketAddressFromString(String hostAndPort) throws URISyntaxException {
        // from https://stackoverflow.com/questions/2345063/java-common-way-to-validate-and-convert-hostport-to-inetsocketaddress

        // WORKAROUND: add any scheme to make the resulting URI valid.
        URI uri = new URI("my://" + hostAndPort); // may throw URISyntaxException
        String host = uri.getHost();
        int port = uri.getPort();

        if (uri.getHost() == null || uri.getPort() == -1) {
            throw new URISyntaxException(uri.toString(),
                "URI must have host and port parts");
        }

        // validation succeeded
        return new InetSocketAddress(host, port);
    }

    public static int getTotalElapsedTimeInMillis(HarTiming timings) {
        // getSsl() time purposely omitted here, because it is included in the getConnect() time
        return (timings.getBlocked() != -1 ? timings.getBlocked() : 0)
                + (timings.getDns() != -1 ? timings.getDns() : 0)
                + (timings.getConnect() != -1 ? timings.getConnect() : 0)
                + (timings.getSend() !=  null && timings.getSend() != -1 ? timings.getSend() : 0)
                + (timings.getWait() !=  null && timings.getWait() != -1 ? timings.getWait() : 0)
                + (timings.getReceive() !=  null && timings.getReceive() != -1 ? timings.getReceive() : 0);
    }
}
