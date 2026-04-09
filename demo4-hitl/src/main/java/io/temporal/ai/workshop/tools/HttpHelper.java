// ABOUTME: Simple HTTP GET utility shared by tool implementations.
// Keeps tool classes focused on their domain logic.

package io.temporal.ai.workshop.tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class HttpHelper {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private HttpHelper() {} 

    public static String get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP GET failed for " + url + " — status " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP GET failed for " + url, e);
        }
    }
}
