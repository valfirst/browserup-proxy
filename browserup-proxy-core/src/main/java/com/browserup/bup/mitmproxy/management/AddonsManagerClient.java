package com.browserup.bup.mitmproxy.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AddonsManagerClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final int port;
    private final String host = "localhost";

    public AddonsManagerClient(int port) {
        this.port = port;
    }

    public <T> T putRequestToAddonsManager(String addOnPath,
                                        String operation,
                                        List<Pair<String, String>> queryParams,
                                        HttpRequest.BodyPublisher requestBody,
                                        String contentType,
                                        Class<T> responseClass) {
        return requestToAddonsManager(addOnPath, operation, queryParams, "PUT", requestBody, contentType,
                responseClass);
    }

    public <T> T getRequestToAddonsManager(String addOnPath,
                                        String operation,
                                        List<Pair<String, String>> queryParams,
                                        Class<T> responseClass) {
        return requestToAddonsManager(addOnPath, operation, queryParams, "GET", null, null, responseClass);
    }

    public <T> T requestToAddonsManager(String addOnPath,
                                        String operation,
                                        List<Pair<String, String>> queryParams,
                                        String method,
                                        HttpRequest.BodyPublisher requestBody,
                                        String contentType,
                                        Class<T> responseClass) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(buildRequestUrl(addOnPath, operation, queryParams));

        if (requestBody != null) {
            requestBuilder.method(method, requestBody).header("Content-Type", contentType);
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Failed to request manager API", ex);
        }

        try {
            if (responseClass.equals(Void.class)) return null;

            if (responseClass.equals(String.class)) return (T) new String(response.body(), StandardCharsets.UTF_8);

            return new ObjectMapper().readerFor(responseClass).readValue(Objects.requireNonNull(response.body()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse response from manager API", e);
        }
    }

    private URI buildRequestUrl(String addOnPath, String operation, List<Pair<String, String>> queryParams) {
        String uri = String.format("http://%s:%d/%s/%s", host, port, addOnPath, operation);
        if (!queryParams.isEmpty()) {
            String query = queryParams.stream()
                    .map(pair -> buildUriQueryPart(pair.getKey(), pair.getValue()))
                    .collect(Collectors.joining("&"));
            uri = uri + "?" + query;
        }
        return URI.create(uri);
    }

    private static String buildUriQueryPart(String name, String value) {
        StringBuilder queryPart = new StringBuilder(URLEncoder.encode(name, StandardCharsets.UTF_8));
        if (value != null) {
            queryPart.append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return queryPart.toString();
    }
}
