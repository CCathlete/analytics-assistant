package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.control.Either;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WebDataSourceAdapter implements DataSourceProvider {

    private final HttpClient client;

    public WebDataSourceAdapter() {
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public Either<Throwable, String> fetchFrom(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Either.right(response.body());
            } else {
                return Either.left(new RuntimeException("HTTP request failed with status code: " + response.statusCode()));
            }
        } catch (Exception e) {
            return Either.left(e);
        }
    }
}
