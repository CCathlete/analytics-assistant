package com.catgineer.analytics_assistant.control.configuration;

import com.catgineer.analytics_assistant.application.services.AnalyticsUseCase;
import com.catgineer.analytics_assistant.domain.services.AIService;
import com.catgineer.analytics_assistant.infrastructure.adapters.MockOpenWebUIAdapter;
import com.catgineer.analytics_assistant.infrastructure.adapters.OpenWebUIAdapter; // Added import for real adapter
import com.catgineer.analytics_assistant.infrastructure.adapters.WebDataSourceAdapter;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile; // Added import
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BeanConfiguration {

    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) { // Injects a builder provided by Spring Boot
        return webClientBuilder.build();
    }

    @Bean
    public DataSourceProvider dataSourceProvider(WebClient webClient) {
        return new WebDataSourceAdapter(webClient);
    }

    @Bean
    @Profile("mock") // This bean will be active when "mock" profile is active
    public AIProvider mockAiProvider(WebClient webClient) {
        return new MockOpenWebUIAdapter(webClient);
    }

    @Bean
    @Profile("!mock") // This bean will be active when "mock" profile is NOT active
    public AIProvider realAiProvider(WebClient.Builder webClientBuilder) {
        return new OpenWebUIAdapter(webClientBuilder);
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
