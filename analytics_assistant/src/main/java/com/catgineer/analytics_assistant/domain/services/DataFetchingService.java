package com.catgineer.analytics_assistant.domain.services;

import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.control.Either;

public class DataFetchingService {

    private final DataSourceProvider dataSourceProvider;

    public DataFetchingService(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    public Either<Throwable, String> fetchDataFrom(String url) {
        if (url == null || url.isBlank()) {
            return Either.left(new IllegalArgumentException("URL cannot be null or empty"));
        }
        return dataSourceProvider.fetchFrom(url);
    }
}
