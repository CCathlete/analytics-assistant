package com.catgineer.analytics_assistant.application.services;

import com.catgineer.analytics_assistant.configuration.AppConfigData;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyticsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsUseCase.class);

    private final DataSourceProvider dataSourceProvider;
    private final AppConfigData appConfigData;

    public AnalyticsUseCase(DataSourceProvider dataSourceProvider, AppConfigData appConfigData) {
        this.dataSourceProvider = dataSourceProvider;
        this.appConfigData = appConfigData;
    }

    public Try<List<String>> getAnalyticsData() {
        if (appConfigData.getSourceUrls() == null || appConfigData.getSourceUrls().isEmpty()) {
            logger.warn("No source URLs configured in app_config.json. Returning empty data.");
            return Try.success(List.empty());
        }

        logger.info("Fetching analytics data from {} configured URLs.", appConfigData.getSourceUrls().size());

        List<Try<String>> results = List.ofAll(appConfigData.getSourceUrls())
                .map(url -> dataSourceProvider.fetchFrom(url)
                        .onSuccess(data -> logger.info("Successfully fetched data from URL: {}. Data length: {}", url, data.length()))
                        .onFailure(error -> logger.error("Failed to fetch data from URL: {}. Error: {}", url, error.getMessage(), error))
                );

        // Sequence turns List<Try<String>> into Try<List<String>>
        return Try.sequence(results)
                .onSuccess(dataList -> logger.info("Successfully fetched and collected data from all {} URLs.", dataList.size()))
                .onFailure(error -> logger.error("One or more data fetches failed. Error: {}", error.getMessage(), error));
    }
}
