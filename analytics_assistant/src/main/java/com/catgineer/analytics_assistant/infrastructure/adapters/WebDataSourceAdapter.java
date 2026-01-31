package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component // Mark as a Spring component for DI
public class WebDataSourceAdapter implements DataSourceProvider {

    private final WebClient webClient;

    // WebClient should be injected, ideally as a Bean configured in BeanConfiguration
    public WebDataSourceAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<String> fetchFrom(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("HTTP request failed with status code: " + clientResponse.statusCode() + " Body: " + errorBody))))
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to fetch data from " + url + ": " + e.getMessage(), e)));
    }
}
