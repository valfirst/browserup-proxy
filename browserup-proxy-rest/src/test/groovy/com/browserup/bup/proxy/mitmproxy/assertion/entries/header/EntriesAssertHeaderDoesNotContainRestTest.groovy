package com.browserup.bup.proxy.mitmproxy.assertion.entries.header

import com.browserup.bup.assertion.model.AssertionResult
import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpResponseDecorator
import org.apache.http.HttpStatus
import org.hamcrest.Matchers
import org.junit.Ignore
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals

@Ignore
class EntriesAssertHeaderDoesNotContainRestTest extends com.browserup.bup.proxy.mitmproxy.assertion.entries.header.BaseEntriesAssertHeaderRestTest {

    @Override
    String getUrlPath() {
        return 'har/entries/assertResponseHeaderDoesNotContain'
    }

    @Test
    void urlFilterMatchesBothAndHeaderValueMissedInBothPasses() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerValue: MISSING_HEADER_VALUE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesBothAndAnyHeaderNameAndHeaderValuePresentFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_BOTH,
                    headerValue: FIRST_HEADER_VALUE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(2))
                assertAssertionFailed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndAnyHeaderNameAndHeaderValuePresentFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerValue: FIRST_HEADER_VALUE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
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
    void urlFilterMatchesFirstAndAnyHeaderNameAndHeaderValueMissedPassed() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerValue: SECOND_HEADER_VALUE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndFirstHeaderNameAndSecondHeaderValuePassed() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerName: FIRST_HEADER_NAME,
                    headerValue: SECOND_HEADER_VALUE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndFirstHeaderNameAndFirstHeaderValueContainsFails() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerName: FIRST_HEADER_NAME,
                    headerValue: FIRST_HEADER_VALUE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionFailed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesFirstAndFirstHeaderNameAndSecondHeaderValueContainsPassed() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_FIRST,
                    headerName: FIRST_HEADER_NAME,
                    headerValue: SECOND_HEADER_VALUE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertThat('Expected to get one assertion result', assertionResult.requests, Matchers.hasSize(1))
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void urlFilterMatchesNonePasses() {
        sendRequestsToTargetServer(FIRST_HEADER, SECOND_HEADER)

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    headerName: FIRST_HEADER_NAME,
                    headerValue: SECOND_HEADER_VALUE
            ]

            response.success = { HttpResponseDecorator resp ->
                def assertionResult = new ObjectMapper().readValue(resp.entity.content, AssertionResult) as AssertionResult
                assertAssertionNotNull(assertionResult)
                assertAssertionPassed(assertionResult)
            }
        }
    }

    @Test
    void getBadRequestIfHeaderValueNotProvided() {
        proxyManager.get()[0].newHar()

        sendGetToProxyServer { req ->
            uri.path = fullUrlPath
            uri.query = [
                    urlPattern: URL_PATTERN_TO_MATCH_NOTHING,
                    headerName: FIRST_HEADER_NAME
            ]

            response.failure = { resp, reader ->
                assertEquals('Expected to get bad request', resp.status, HttpStatus.SC_BAD_REQUEST)
            }
            response.success = { resp, reader ->
                throw new AssertionError('Expected to get bad request, got: ' + resp.status)
            }
        }
    }
}
