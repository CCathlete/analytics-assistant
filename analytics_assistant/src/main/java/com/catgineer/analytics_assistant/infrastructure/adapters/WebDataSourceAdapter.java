package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;

public class WebDataSourceAdapter implements DataSourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebDataSourceAdapter.class);
    private final RestClient restClient;

    public WebDataSourceAdapter(RestClient.Builder restClientBuilder) {
        // We use the builder to allow for global configurations if needed later
        this.restClient = restClientBuilder.build();
    }

    private String internalFetch(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Target URL cannot be null or empty");
        }
        
        logger.info("Fetching raw data from URL: {}", url);
        
        // RestClient execution is synchronous. 
        // On a Virtual Thread, this is as efficient as await.
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }

    @Override
    public Mono<Try<String>> fetchFrom(String url) {
        // SafeRunner keeps our monadic contract intact for the caller
        return SafeRunner.futureSafe(() -> internalFetch(url));
    }
}
