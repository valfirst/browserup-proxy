package com.browserup.bup.proxy.mitmproxy.assertion.entries.content

import com.browserup.bup.assertion.model.AssertionResult
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpResponseDecorator
import org.hamcrest.Matchers
import org.junit.Ignore
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat

@Ignore
class EntriesAssertContentContainsRestTest extends BaseEntriesAssertContentRestTest {

    @Override
    String getUrlPath() {
        return 'har/entries/assertContentContains'
    }

    @Test
    void urlFilterMatchesBothAndContentContainsInBothPasses() {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    contentText: RESPONSE_COMMON_PART
            ]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesBothAndContentDoesNotContainInSomeFails() {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    contentText: FIRST_RESPONSE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get two assertion entries', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionFailed(assertionResult)

                def failedRequests = assertionResult.failedRequests

                assertThat('Expected to get one failed assertion entry', failedRequests, Matchers.hasSize(1))
                assertThat('Expected failed entry to have proper request url',
                        failedRequests.get(0).url,
                        Matchers.containsString(URL_OF_SECOND_REQUEST))
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndContentDoesNotContainInFirstFails() {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    contentText: SECOND_RESPONSE
            ]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertAssertionFailed(assertionResult)

                def failedRequests = assertionResult.failedRequests

                assertThat('Expected to get one assertion entry', failedRequests, Matchers.hasSize(1))
                assertThat('Expected assertion entry to have proper url',
                        failedRequests.get(0).url,
                        Matchers.containsString(URL_OF_FIRST_REQUEST))
            }
        }
    }

    @Test
    void urlFilterMatchesNonePasses() {
        sendRequestsToTargetServer(FIRST_RESPONSE, SECOND_RESPONSE)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    contentText: RESPONSE_NOT_TO_FIND
            ]
            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertAssertionPassed(assertionResult)
            }
        }
    }
}
