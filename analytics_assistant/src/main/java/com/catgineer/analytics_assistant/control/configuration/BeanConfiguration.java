package com.catgineer.analytics_assistant.control.configuration;

import com.catgineer.analytics_assistant.application.services.AnalyticsUseCase;
import com.catgineer.analytics_assistant.infrastructure.adapters.WebDataSourceAdapter;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public DataSourceProvider dataSourceProvider() {
        return new WebDataSourceAdapter();
    }

    @Bean
    public AnalyticsUseCase analyticsUseCase(DataSourceProvider dataSourceProvider) {
        return new AnalyticsUseCase(dataSourceProvider);
    }
}
