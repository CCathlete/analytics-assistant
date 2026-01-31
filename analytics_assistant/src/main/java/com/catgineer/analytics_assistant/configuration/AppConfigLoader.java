package com.catgineer.analytics_assistant.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@Configuration
public class AppConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(AppConfigLoader.class);
    private static final String CONFIG_FILE_PATH = "input_files/app_config.json";

    @Bean
    public AppConfigData appConfigData(ObjectMapper objectMapper) {
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            logger.error("Application configuration file not found: {}. Using empty configuration.", CONFIG_FILE_PATH);
            return new AppConfigData(); // Return empty config
        }

        try {
            AppConfigData config = objectMapper.readValue(configFile, AppConfigData.class);
            logger.info("Successfully loaded application configuration from {}", CONFIG_FILE_PATH);
            return config;
        } catch (IOException e) {
            logger.error("Error reading or parsing application configuration file: {}. Error: {}. Using empty configuration.", CONFIG_FILE_PATH, e.getMessage(), e);
            return new AppConfigData(); // Return empty config on error
        }
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
