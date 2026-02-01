package com.catgineer.analytics_assistant.control.configuration;

import tools.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class AppConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(AppConfigLoader.class);
    private static final String CONFIG_NAME = "app_config.json";

    private AppConfigData internalLoadConfig(ObjectMapper objectMapper) {
        logger.info("Deterministic load of {} from classpath resources", CONFIG_NAME);

        return Try.withResources(() -> new ClassPathResource(CONFIG_NAME).getInputStream())
                .of(inputStream -> objectMapper.readValue(inputStream, AppConfigData.class))
                .onSuccess(cfg -> logger.info("AppConfigData successfully initialized"))
                .onFailure(ex -> logger.error("Failed to load config from resources: {}", ex.getMessage()))
                .getOrElseThrow(() -> new IllegalStateException("Missing app_config.json in resources folder"));
    }

    @Bean
    public AppConfigData appConfigData(ObjectMapper objectMapper) {
        return internalLoadConfig(objectMapper);
    }
}
