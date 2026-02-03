package com.catgineer.analytics_assistant.control.configuration;

import com.catgineer.analytics_assistant.application.services.GenerateChartFromPrompt;
import com.catgineer.analytics_assistant.application.services.IngestSources;
import com.catgineer.analytics_assistant.domain.services.AIService;
import com.catgineer.analytics_assistant.domain.services.DataSourceService;
import com.catgineer.analytics_assistant.domain.services.VisualisationService;
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

import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class BeanConfiguration {

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
    public DataSourceProvider dataSourceAdapter() {
        return new WebDataSourceAdapter(RestClient.builder());
    }

    // --- Infrastructure ---

    @Bean
    public OpenWebUIAdapter openWebUIAdapter(
            @Value("${OPENWEBUI_API_BASEURL}") String baseUrl,
            @Value("${OPENWEBUI_API_KEY}") String apiKey,
            @Value("${EMBEDDING_NODE_URL}") String bridgeUrl
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMinutes(5)); 

        RestClient.Builder builder = RestClient.builder().requestFactory(factory);

        return new OpenWebUIAdapter(
            builder, 
            baseUrl, 
            apiKey, 
            bridgeUrl
        );
    }

    @Bean
    public VisualisationProvider supersetAdapter(
            JdbcTemplate dbConnection,
            @Value("${SUPERSET_BASE_URL}") String baseUrl,
            @Value("${SUPERSET_USERNAME}") String userName,
            @Value("${SUPERSET_PASSWORD}") String password 
    ){
        return new SupersetAdapter(
            RestClient.builder(), 
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

    @Bean
    public DataSourceService dataSourceService(DataSourceProvider dataSourceAdapter) {
        return new DataSourceService(dataSourceAdapter);
    }

    @Bean
    public VisualisationService visualisationService(VisualisationProvider supersetAdapter) {
        return new VisualisationService(supersetAdapter);
    }

    // --- Application Services ---

    @Bean
    public IngestSources ingestSources(
        DataSourceService dataSourceService,
        AIService aiService
        ){
        return new IngestSources(dataSourceService, aiService);
    }

    @Bean
    public GenerateChartFromPrompt generateChartFromPrompt(
        AIService aiService,
        VisualisationService visualisationService
        ){
        return new GenerateChartFromPrompt(aiService, visualisationService);
    }

}
