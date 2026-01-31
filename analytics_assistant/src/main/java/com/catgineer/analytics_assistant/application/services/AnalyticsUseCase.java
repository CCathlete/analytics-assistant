package com.catgineer.analytics_assistant.application.services;

import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyticsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsUseCase.class);

    private final DataSourceProvider dataSourceProvider;

    public AnalyticsUseCase(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    public Try<String> getAnalyticsData(String sourceUrl) {
        logger.info("Attempting to fetch analytics data for URL: {}", sourceUrl);

        if (sourceUrl == null || sourceUrl.isBlank()) {
            logger.warn("Received null or blank URL for analytics data.");
            return Try.failure(new IllegalArgumentException("URL cannot be null or empty"));
        }

        return dataSourceProvider.fetchFrom(sourceUrl)
                .onSuccess(data -> logger.info("Successfully fetched analytics data for URL: {}. Data length: {}", sourceUrl, data.length()))
                .onFailure(error -> logger.error("Failed to fetch analytics data for URL: {}. Error: {}", sourceUrl, error.getMessage(), error));
    }
}
