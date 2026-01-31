package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class WebDataSourceAdapter implements DataSourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebDataSourceAdapter.class);
    private final WebClient webClient;

    public WebDataSourceAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    private String internalFetch(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Target URL cannot be null or empty");
        }
        logger.info("Fetching raw data from URL: {}", url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(20));
    }

    @Override
    public Mono<Try<String>> fetchFrom(String url) {
        return SafeRunner.futureSafe(() -> {
            return internalFetch(url);
        });
    }
}
