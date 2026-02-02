package com.catgineer.analytics_assistant.control.configuration;

import com.catgineer.analytics_assistant.application.services.AnalyticsUseCase;
import com.catgineer.analytics_assistant.domain.services.AIService;
import com.catgineer.analytics_assistant.infrastructure.adapters.OpenWebUIAdapter;
import com.catgineer.analytics_assistant.infrastructure.adapters.WebDataSourceAdapter;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile; // Added import
import org.springframework.web.client.RestClient;

@Configuration
public class BeanConfiguration {

    @Bean
    public RestClient.Builder restClient(RestClient.Builder restClientBuilder) { // Injects a builder provided by Spring Boot
        return restClientBuilder;
    }

    @Bean
    public DataSourceProvider dataSourceProvider(RestClient.Builder restClientBuilder) {
        return new WebDataSourceAdapter(restClientBuilder);
    }

    @Bean
    public OpenWebUIAdapter openWebUIAdapter(
            RestClient.Builder builder,
            @Value("${OPENWEBUI_API_BASEURL}") String baseUrl,
            @Value("${OPENWEBUI_API_KEY}") String apiKey,
            @Value("${SCP_REMOTE_USER}") String scpUser,
            @Value("${SCP_REMOTE_HOST}") String scpHost,
            @Value("${SCP_REMOTE_PATH}") String scpPath
    ) {
        return new OpenWebUIAdapter(
            builder, 
            baseUrl, 
            apiKey, 
            scpUser, 
            scpHost, 
            scpPath
        );
    }

    @Bean
    public AIService aiService(AIProvider aiProvider) {
        return new AIService(aiProvider);
    }

    @Bean
    public AnalyticsUseCase analyticsUseCase(DataSourceProvider dataSourceProvider, AppConfigData appConfigData, AIService aiService) {
        return new AnalyticsUseCase(dataSourceProvider, appConfigData, aiService);
    }
}
