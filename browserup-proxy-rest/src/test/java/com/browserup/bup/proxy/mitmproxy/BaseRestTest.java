package com.browserup.bup.proxy.mitmproxy;

import com.browserup.bup.assertion.model.AssertionResult;

import static org.junit.Assert.*;

public abstract class BaseRestTest extends WithRunningProxyRestTest {
    protected static final int TARGET_SERVER_RESPONSE_DELAY = 500;
    protected static final int TARGET_SERVER_SLOW_RESPONSE_DELAY = 1000;
    protected static final int SUCCESSFUL_ASSERTION_TIME_WITHIN = TARGET_SERVER_RESPONSE_DELAY + 100;
    protected static final int FAILED_ASSERTION_TIME_WITHIN = TARGET_SERVER_RESPONSE_DELAY - 100;
    protected static final int MILLISECONDS_BETWEEN_REQUESTS = 50;

    protected abstract String getUrlPath();

    protected String getFullUrlPath() {
        return "/proxy/" + proxy.getPort() + "/" + getUrlPath();
    }

    protected static void assertAssertionNotNull(AssertionResult assertion) {
        assertNotNull("Expected to get non null assertion result", assertion);
    }

    protected static void assertAssertionPassed(AssertionResult assertion) {
        assertTrue("Expected assertion to pass", assertion.getPassed());
        assertFalse("Expected assertion to pass", assertion.getFailed());
    }

    protected static void assertAssertionFailed(AssertionResult assertion) {
        assertFalse("Expected assertion to fail", assertion.getPassed());
        assertTrue("Expected assertion to fail", assertion.getFailed());
    }
}
