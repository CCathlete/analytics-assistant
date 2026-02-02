package com.catgineer.analytics_assistant.application.services;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.SourceData;
import com.catgineer.analytics_assistant.domain.services.AIService;
import com.catgineer.analytics_assistant.domain.services.DataSourceService;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

public class IngestSources {
    private static final Logger logger = LoggerFactory.getLogger(IngestSources.class);
    private final DataSourceService dataSourceService;
    private final AIService aiService;

    public IngestSources(DataSourceService dataSourceService, AIService aiService) {
        this.dataSourceService = dataSourceService;
        this.aiService = aiService;
    }

    private Boolean internalIngest(List<String> urls) throws Exception {
        logger.info("Starting ingestion for {} sources", urls.size());

        List<Try<SourceData>> ingestionResults = dataSourceService.fetchMultipleSources(urls)
                .collectList()
                .toFuture()
                .get();

        List<Try<Boolean>> embeddingResults = ingestionResults.stream()
                .map(sourceResult -> Match(sourceResult).of(
                        Case($Success($()), (SourceData data) -> {
                            logger.info("Stage [Data Fetching] success for {}. Triggering embedding.", data.sourceUrl());
                            
                            Try<Try<Boolean>> embedOp = Try.of(() -> 
                                aiService.embedData(data.content()).toFuture().get()
                            );

                            return Match(embedOp).of(
                                Case($Success($()), res -> {
                                    logger.info("Stage [Android Embedding] success for {}", data.sourceUrl());
                                    return res;
                                }),
                                Case($Failure($()), ex -> {
                                    logger.error("Stage [Android Embedding] failed for {}", data.sourceUrl());
                                    return Try.<Boolean>failure(ex);
                                })
                            );
                        }),
                        Case($Failure($()), ex -> {
                            logger.error("Stage [Data Fetching] failed: {}", ex.getMessage());
                            return Try.<Boolean>failure(ex);
                        })
                )).toList();

        return embeddingResults.stream().allMatch(Try::isSuccess);
    }

    public Mono<Try<Boolean>> execute(List<String> urls) {
        return SafeRunner.futureSafe(() -> internalIngest(urls));
    }
}
