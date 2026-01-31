package com.catgineer.analytics_assistant.application.services;

import com.catgineer.analytics_assistant.control.configuration.AppConfigData;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.domain.services.AIService; // Changed import
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AnalyticsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsUseCase.class);

    private final DataSourceProvider dataSourceProvider;
    private final AppConfigData appConfigData;
    private final AIService aiService; // Changed type

    public AnalyticsUseCase(DataSourceProvider dataSourceProvider, AppConfigData appConfigData, AIService aiService) { // Changed parameter
        this.dataSourceProvider = dataSourceProvider;
        this.appConfigData = appConfigData;
        this.aiService = aiService; // Changed assignment
    }

    public Flux<Try<ChartData>> getAnalyticsData(String prompt) {
        if (appConfigData.getSourceUrls() == null || appConfigData.getSourceUrls().isEmpty()) {
            logger.warn("No source URLs configured in app_config.json. Returning empty data.");
            return Flux.empty();
        }

        logger.info("Fetching raw analytics data from {} configured URLs.", appConfigData.getSourceUrls().size());

        return Flux.fromIterable(appConfigData.getSourceUrls())
                .flatMap(url -> dataSourceProvider.fetchFrom(url)
                        .map(data -> {
                            logger.info("Successfully fetched data from URL: {}. Data length: {}", url, data.length());
                            return Try.success(data);
                        })
                        .onErrorResume(e -> {
                            logger.error("Failed to fetch data from URL: {}. Error: {}", url, e.getMessage(), e);
                            return Mono.just(Try.failure(e));
                        })
                )
                .collectList()
                .doOnSuccess(dataList -> logger.info("Successfully fetched and collected raw data from all {} URLs. Total collected items: {}", appConfigData.getSourceUrls().size(), dataList.size()))
                .doOnError(error -> logger.error("One or more raw data fetches failed overall. Error: {}", error.getMessage(), error))
                .flatMapMany(rawDataList -> {
                    List<String> successfulRawData = rawDataList.stream()
                            .filter(Try::isSuccess)
                            .map(Try::get)
                            .collect(Collectors.toList());

                    if (successfulRawData.isEmpty()) {
                        logger.warn("No successful raw data collected, skipping AI processing.");
                        return Flux.empty();
                    }
                    logger.info("Passing {} successful raw data entries to AIService for chart data generation with prompt: {}", successfulRawData.size(), prompt);
                    return aiService.processPromptForCharts(prompt, successfulRawData); // Changed method call
                });
    }
}
