package com.catgineer.analytics_assistant.control.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(DotenvInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        logger.info("Initializing Dotenv and loading .env file...");
        try {
            // Load .env file from the project root
            Dotenv dotenv = Dotenv.configure()
                                  .directory(System.getProperty("user.dir")) // Look in current working directory
                                  .ignoreIfMalformed()
                                  .ignoreIfMissing()
                                  .load();

            Map<String, Object> envVars = new HashMap<>();
            dotenv.entries().forEach(entry -> envVars.put(entry.getKey(), entry.getValue()));

            if (!envVars.isEmpty()) {
                ConfigurableEnvironment environment = applicationContext.getEnvironment();
                environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", envVars));
                logger.info("Successfully loaded {} properties from .env file.", envVars.size());
            } else {
                logger.warn(".env file found but no properties loaded or .env file was missing/malformed.");
            }

        } catch (Exception e) {
            logger.error("Error loading .env file: {}", e.getMessage(), e);
        }
    }
}
