package com.browserup.bup.proxy.mitmproxy;

import com.browserup.bup.assertion.model.AssertionResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertNotNull(assertion, "Expected to get non null assertion result");
    }

    protected static void assertAssertionPassed(AssertionResult assertion) {
        assertTrue(assertion.getPassed(), "Expected assertion to pass");
        assertFalse(assertion.getFailed(), "Expected assertion to pass");
    }

    protected static void assertAssertionFailed(AssertionResult assertion) {
        assertFalse(assertion.getPassed(), "Expected assertion to fail");
        assertTrue(assertion.getFailed(), "Expected assertion to fail");
    }
}
