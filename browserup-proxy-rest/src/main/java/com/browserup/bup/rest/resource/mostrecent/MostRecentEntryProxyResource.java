package com.browserup.bup.rest.resource.mostrecent;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.assertion.model.AssertionResult;
import com.browserup.bup.proxy.MitmProxyManager;
import com.browserup.bup.rest.validation.HttpStatusCodeConstraint;
import com.browserup.bup.rest.validation.LongPositiveConstraint;
import com.browserup.bup.rest.validation.NotBlankConstraint;
import com.browserup.bup.rest.validation.NotNullConstraint;
import com.browserup.bup.rest.validation.PatternConstraint;
import com.browserup.bup.rest.validation.PortWithExistingProxyConstraint;
import com.browserup.bup.util.HttpStatusClass;

import de.sstoehr.harreader.model.HarEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.regex.Pattern;

import static com.browserup.bup.rest.openapi.DocConstants.*;

@Path("/proxy/{port}/har/mostRecentEntry")
public class MostRecentEntryProxyResource {
    private static final String URL_PATTERN = "urlPattern";
    private static final String PORT = "port";
    private static final String CONTENT_TEXT = "contentText";
    private static final String CONTENT_PATTERN = "contentPattern";
    private static final String LENGTH = "length";
    private static final String MILLISECONDS = "milliseconds";
    private static final String HEADER_NAME = "headerName";
    private static final String HEADER_VALUE = "headerValue";
    private static final String HEADER_NAME_PATTERN = "headerNamePattern";
    private static final String HEADER_VALUE_PATTERN = "headerValuePattern";
    private static final String STATUS = "status";

    private final MitmProxyManager proxyManager;

    public MostRecentEntryProxyResource(@Context MitmProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Search the entire log for the most recent entry whose request URL matches the given url.",
    responses = {@ApiResponse(
            description = "Har Entry",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = HarEntry.class)))})
    public HarEntry mostRecentEntry(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern) {
        return proxyManager.get(port)
                .findMostRecentEntry(Pattern.compile(urlPattern))
                .orElse(null);
    }

    @GET
    @Path("/assertContentContains")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            description = "Assert that response content for the most recent request found by a given URL pattern contains specified value.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult contentContains(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(CONTENT_TEXT)
            @NotBlankConstraint(paramName = CONTENT_TEXT)
            @Parameter(required = true, description = CONTENT_TEXT_DESCRIPTION) String contentText) {

        return proxyManager.get(port).assertMostRecentResponseContentContains(Pattern.compile(urlPattern), contentText);
    }


    @GET
    @Path("/assertContentDoesNotContain")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Assert that response content for the most recent request " +
            "found by a given URL pattern doesn't contain specified value.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult contentDoesNotContain(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(CONTENT_TEXT)
            @NotBlankConstraint(paramName = CONTENT_TEXT)
            @Parameter(required = true, description = CONTENT_TEXT_DESCRIPTION) String contentText) {

        return proxyManager.get(port).assertMostRecentResponseContentDoesNotContain(Pattern.compile(urlPattern), contentText);
    }

    @GET
    @Path("/assertContentMatches")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Assert that response content for the most recent request " +
            "found by a given URL pattern matches content pattern.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult contentMatches(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(CONTENT_PATTERN)
            @NotBlankConstraint(paramName = CONTENT_PATTERN)
            @PatternConstraint(paramName = CONTENT_PATTERN)
            @Parameter(required = true, description = CONTENT_PATTERN_DESCRIPTION) String contentPattern) {

        return proxyManager.get(port).assertMostRecentResponseContentMatches(
                Pattern.compile(urlPattern),
                Pattern.compile(contentPattern));
    }

    @GET
    @Path("/assertContentLengthLessThanOrEqual")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            description = "Assert that content length of the most recent response found by url pattern does not exceed max value.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult contentLengthLessThanOrEqual(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(LENGTH)
            @NotNullConstraint(paramName = LENGTH)
            @LongPositiveConstraint(value = 0, paramName = LENGTH)
            @Parameter(required = true, description = CONTENT_LENGTH_DESCRIPTION) String length) {

        return proxyManager.get(port).assertMostRecentResponseContentLengthLessThanOrEqual(
                Pattern.compile(urlPattern),
                Long.parseLong(length));
    }

    @GET
    @Path("/assertResponseTimeLessThanOrEqual")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Assert that the response time for the most recent request " +
            "found by a given URL pattern is less than or equal to a given number of milliseconds.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult responseTimeLessThanOrEqual(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(MILLISECONDS)
            @LongPositiveConstraint(value = 0, paramName = MILLISECONDS)
            @Parameter(required = true, description = MILLISECONDS_DESCRIPTION) String milliseconds) {

        return proxyManager.get(port).assertMostRecentResponseTimeLessThanOrEqual(
                Pattern.compile(urlPattern),
                Long.parseLong(milliseconds));
    }

    @GET
    @Path("/assertResponseHeaderContains")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Assert that if the most recent response found by url pattern has header with specified name " +
            "- it's value must contain specified text.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult responseHeaderContains(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(HEADER_NAME)
            @Parameter(description = HEADER_NAME_DESCRIPTION) String headerName,

            @QueryParam(HEADER_VALUE)
            @NotBlankConstraint(paramName = HEADER_VALUE)
            @Parameter(required = true, description = HEADER_VALUE_DESCRIPTION) String headerValue) {

        return proxyManager.get(port).assertMostRecentResponseHeaderContains(
                Pattern.compile(urlPattern),
                headerName, headerValue);
    }

    @GET
    @Path("/assertResponseHeaderDoesNotContain")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Assert that if the most recent response found by url pattern has header with specified name" +
            "- it's value must not contain specified text.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult responseHeaderDoesNotContain(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(HEADER_NAME)
            @Parameter(description = HEADER_NAME_DESCRIPTION) String headerName,

            @QueryParam(HEADER_VALUE)
            @NotBlankConstraint(paramName = HEADER_VALUE)
            @Parameter(required = true, description = HEADER_VALUE_DESCRIPTION) String headerValue) {

        return proxyManager.get(port).assertMostRecentResponseHeaderDoesNotContain(
                Pattern.compile(urlPattern),
                headerName, headerValue);
    }

    @GET
    @Path("/assertResponseHeaderMatches")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Assert that if the most recent response found by url pattern has header with name " +
            "found by name pattern - it's value should match value pattern.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult responseHeaderMatches(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(required = true, description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(HEADER_NAME_PATTERN)
            @PatternConstraint(paramName = HEADER_NAME_PATTERN)
            @Parameter(required = true, description = HEADER_NAME_PATTERN_DESCRIPTION) String headerNamePattern,

            @QueryParam(HEADER_VALUE_PATTERN)
            @NotBlankConstraint(paramName = HEADER_VALUE_PATTERN)
            @PatternConstraint(paramName = HEADER_VALUE_PATTERN)
            @Parameter(required = true, description = HEADER_VALUE_PATTERN_DESCRIPTION) String headerValuePattern) {

        return proxyManager.get(port).assertMostRecentResponseHeaderMatches(
                Pattern.compile(urlPattern),
                headerNamePattern != null ? Pattern.compile(headerNamePattern) : null,
                Pattern.compile(headerValuePattern));
    }

    @GET
    @Path("/assertStatusEquals")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "In case url patter is provided assert that the most recent response " +
            "found by url pattern has specified status, otherwise " +
            "assert that the most recent response has specified status.",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult statusEquals(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(description = URL_PATTERN_DESCRIPTION) String urlPattern,

            @QueryParam(STATUS)
            @NotNullConstraint(paramName = STATUS)
            @HttpStatusCodeConstraint(paramName = STATUS)
            @Parameter(required = true, description = STATUS_DESCRIPTION) String status) {

        MitmProxyServer proxyServer = proxyManager.get(port);
        int intStatus = Integer.parseInt(status);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(intStatus) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), intStatus);
    }

    @GET
    @Path("/assertStatusInformational")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "In case url pattern is provided assert that the most recent response " +
            "found by url pattern has status belonging to INFORMATIONAL class (1xx), otherwise " +
            "assert that the most recent response has status belonging to INFORMATIONAL class (1xx).",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult statusInformational(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(description = URL_PATTERN_DESCRIPTION) String urlPattern) {

        MitmProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.INFORMATIONAL) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.INFORMATIONAL);
    }

    @GET
    @Path("/assertStatusSuccess")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "In case url pattern is provided assert that the most recent response " +
            "found by url pattern has status belonging to SUCCESS class (2xx), otherwise " +
            "assert that the most recent response has status belonging to SUCCESS class (2xx).",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult statusSuccess(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(description = URL_PATTERN_DESCRIPTION) String urlPattern) {

        MitmProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SUCCESS) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.SUCCESS);
    }

    @GET
    @Path("/assertStatusRedirection")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "In case url pattern is provided assert that the most recent response " +
            "found by url pattern has status belonging to REDIRECTION class (3xx), otherwise " +
            "assert that the most recent response has status belonging to REDIRECTION class (3xx).",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult statusRedirection(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(description = URL_PATTERN_DESCRIPTION) String urlPattern) {

        MitmProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.REDIRECTION) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.REDIRECTION);
    }

    @GET
    @Path("/assertStatusClientError")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "In case url pattern is provided assert that the most recent response " +
            "found by url pattern has status belonging to CLIENT ERROR class (4xx), otherwise " +
            "assert that the most recent response has status belonging to CLIENT ERROR class (4xx).",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult statusClientError(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(description = URL_PATTERN_DESCRIPTION) String urlPattern) {

        MitmProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.CLIENT_ERROR) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.CLIENT_ERROR);
    }

    @GET
    @Path("/assertStatusServerError")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "In case url pattern is provided assert that the most recent response " +
            "found by url pattern has status belonging to SERVER ERROR class (5xx), otherwise " +
            "assert that the most recent response has status belonging to SERVER ERROR class (5xx).",
            responses = {@ApiResponse(
                    description = "Assertion result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AssertionResult.class)))})
    public AssertionResult statusServerError(
            @PathParam(PORT)
            @NotNullConstraint(paramName = PORT)
            @PortWithExistingProxyConstraint
            @Parameter(required = true, in = ParameterIn.PATH, description = PORT_DESCRIPTION) int port,

            @QueryParam(URL_PATTERN)
            @NotBlankConstraint(paramName = URL_PATTERN)
            @PatternConstraint(paramName = URL_PATTERN)
            @Parameter(description = URL_PATTERN_DESCRIPTION) String urlPattern) {

        MitmProxyServer proxyServer = proxyManager.get(port);

        return urlPattern.isEmpty() ?
                proxyServer.assertMostRecentResponseStatusCode(HttpStatusClass.SERVER_ERROR) :
                proxyServer.assertMostRecentResponseStatusCode(Pattern.compile(urlPattern), HttpStatusClass.SERVER_ERROR);
    }
}
