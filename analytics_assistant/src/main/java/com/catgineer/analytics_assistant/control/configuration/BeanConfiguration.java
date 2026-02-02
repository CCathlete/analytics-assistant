package com.catgineer.analytics_assistant.control.configuration;

import com.catgineer.analytics_assistant.domain.services.AIService;
import com.catgineer.analytics_assistant.infrastructure.adapters.OpenWebUIAdapter;
import com.catgineer.analytics_assistant.infrastructure.adapters.SupersetAdapter;
import com.catgineer.analytics_assistant.infrastructure.adapters.WebDataSourceAdapter;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import com.catgineer.analytics_assistant.infrastructure.ports.DataSourceProvider;
import com.catgineer.analytics_assistant.infrastructure.ports.VisualisationProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

@Configuration
public class BeanConfiguration {

    @Bean
    public RestClient.Builder restClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder;
    }

    @Bean
    public HikariDataSource dataSource(
            @Value("${DB_HOST}") String host,
            @Value("${DB_PORT}") String port,
            @Value("${DB_NAME}") String dbName,
            @Value("${DB_USER}") String user,
            @Value("${DB_PASSWORD}") String password
    ) {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Connection Pool limit.
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setPoolName("PostgresPool-VT-Enabled");
        
        // Ensure we don't leak connections during I/O wait.
        config.setConnectionTimeout(30000); 

        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate pgConnection(HikariDataSource dataSource){
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public DataSourceProvider dataSourceAdapter(RestClient.Builder restClientBuilder) {
        return new WebDataSourceAdapter(restClientBuilder);
    }

    // --- Infrastructure ---

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
    public VisualisationProvider visualisationProvider(
            RestClient.Builder builder,
            JdbcTemplate dbConnection,
            @Value("${SUPERSET_BASE_URL}") String baseUrl,
            @Value("${SUPERSET_USERNAME}") String userName,
            @Value("${SUPERSET_PASSWORD}") String password 
    ){
        return new SupersetAdapter(
            builder,
            dbConnection,
            baseUrl,
            userName,
            password
        );
    }

    // --- Domain Services ---

    @Bean
    public AIService aiService(AIProvider aiProvider) {
        return new AIService(aiProvider);
    }

    // --- Application Services ---

}
