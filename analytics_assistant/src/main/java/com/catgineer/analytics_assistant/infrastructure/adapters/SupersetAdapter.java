package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.infrastructure.ports.VisualisationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
@Profile("!mock")
public class SupersetAdapter implements VisualisationProvider {

    private static final Logger logger = LoggerFactory.getLogger(SupersetAdapter.class);
    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;
    private String accessToken;

    @Value("${SUPERSET_BASE_URL}")
    private String supersetBaseUrl;

    @Value("${SUPERSET_USERNAME}")
    private String username;

    @Value("${SUPERSET_PASSWORD}")
    private String password;

    @Value("${SUPERSET_DATABASE_ID}") // The ID of the DB connection in Superset
    private String databaseId;

    @Value("${SUPERSET_TABLE_NAME}") // The table name to overwrite/create
    private String tableName;

    public SupersetAdapter(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(supersetBaseUrl).build();
        logger.info("SupersetAdapter initialized targeting database ID: {} and table: {}", databaseId, tableName);
    }

    // --- Private Deterministic Logic ---

    private String internalAuthenticate() {
        logger.info("Authenticating with Superset for user: {}", username);

        JsonNode response = webClient.post()
                .uri("/api/v1/security/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", username,
                        "password", password,
                        "provider", "db"
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(10));

        if (response == null || !response.has("access_token")) {
            throw new RuntimeException("Failed to obtain access token from Superset");
        }

        this.accessToken = response.get("access_token").asText();
        return this.accessToken;
    }

    private Boolean internalUploadCsv(String csvData) {
        if (this.accessToken == null) {
            internalAuthenticate();
        }

        logger.info("Uploading CSV data directly to Superset table: {}", tableName);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        // Convert the string to a ByteArrayResource to simulate a file upload
        builder.part("file", new ByteArrayResource(csvData.getBytes()))
               .filename(tableName + ".csv")
               .contentType(MediaType.TEXT_PLAIN);
        
        builder.part("table_name", tableName);
        builder.part("database_id", databaseId);
        builder.part("if_exists", "replace"); // This ensures we overwrite the data

        webClient.post()
                .uri("/api/v1/database/{id}/import_csv", databaseId)
                .headers(h -> h.setBearerAuth(this.accessToken))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(30));

        logger.info("Successfully uploaded and replaced dataset in Superset.");
        return true;
    }

    // --- Public API ---

    @Override
    public Mono<Try<Boolean>> updateDataset(String csvData) {
        return SafeRunner.futureSafe(() -> internalUploadCsv(csvData));
    }
}
