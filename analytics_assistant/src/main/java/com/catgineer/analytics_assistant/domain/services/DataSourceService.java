package com.catgineer.analytics_assistant.domain.services;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.SourceData;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

import java.util.List;

@Service
public class DataSourceService {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceService.class);
    private final DataSourceProvider dataSourceProvider;

    public DataSourceService(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    private SourceData internalMapToSourceData(String url, String content) {
        logger.debug("Mapping raw content to SourceData for URL: {}", url);
        return new SourceData(url, content);
    }

    public Flux<Try<SourceData>> fetchSource(String url) {
        logger.info("Initiating monadic fetch for URL: {}", url);
        
        return dataSourceProvider.fetchFrom(url)
            .flatMapMany(res -> Match(res).<Flux<Try<SourceData>>>of(
                Case($Success($()), content -> 
                    SafeRunner.futureStream(() -> internalMapToSourceData(url, content))
                ),
                Case($Failure($()), ex -> {
                    logger.error("Provider failed to fetch from {}: {}", url, ex.getMessage());
                    return Flux.just(Try.failure(ex));
                })
            ));
    }

    public Flux<Try<SourceData>> fetchMultipleSources(List<String> urls) {
        logger.info("Orchestrating batch fetch for {} URLs", urls.size());
        return Flux.fromIterable(urls)
            .flatMap(this::fetchSource);
    }
}
