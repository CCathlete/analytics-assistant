package com.catgineer.analytics_assistant.application.services;

import com.catgineer.analytics_assistant.domain.services.DataFetchingService;
import io.vavr.control.Either;

public class AnalyticsUseCase {

    private final DataFetchingService dataFetchingService;

    public AnalyticsUseCase(DataFetchingService dataFetchingService) {
        this.dataFetchingService = dataFetchingService;
    }

    public Either<Throwable, String> getAnalyticsData(String sourceUrl) {
        // Here you could add more application-specific logic,
        // like validation, authorization, combining data from multiple sources, etc.
        return dataFetchingService.fetchDataFrom(sourceUrl);
    }
}
