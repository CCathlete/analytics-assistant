package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.control.Try;
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
    public Try<String> fetchFrom(String url) {
        return SafeRunner.safe(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                throw new RuntimeException("HTTP request failed with status code: " + response.statusCode());
            }
        });
    }
}
