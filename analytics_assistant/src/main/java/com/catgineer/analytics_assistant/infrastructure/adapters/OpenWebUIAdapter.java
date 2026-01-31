package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders; // Not strictly used for setting headers, but useful for constants
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@Profile("!mock") // This bean will be active unless the "mock" profile is active
public class OpenWebUIAdapter implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenWebUIAdapter.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper(); // Initialize ObjectMapper

    private String authToken; // Field to store the authentication token

    @Value("${openwebui.api.baseurl}")
    private String openWebUIApiBaseUrl;

    @Value("${scp.remote.user}")
    private String scpRemoteUser;

    @Value("${scp.remote.host}")
    private String scpRemoteHost;

    @Value("${scp.remote.path}")
    private String scpRemotePath;

    @Value("${openwebui.kb.default-id}")
    private String openWebUIKbDefaultId;

    public OpenWebUIAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(openWebUIApiBaseUrl).build();
    }

    // --- Private blocking methods ---

    private Boolean blockingValidatePrompt(String prompt) throws Exception {
        logger.info("Real (blocking): Validating prompt: '{}'", prompt);
        // This method should perform actual validation logic, not simulated latency.
        if (prompt != null && prompt.contains("malware")) {
            throw new IllegalArgumentException("Prompt contains malware content");
        }
        return true;
    }

    private String blockingSendPromptToAI(String prompt, List<String> contextData) throws Exception {
        logger.info("Real (blocking): Sending prompt to AI: '{}' with {} context data entries.", prompt, contextData.size());
        String requestBody = String.format("{\"prompt\": \"%s\", \"context\": \"%s\"}", prompt, String.join(" ", contextData));
        
        // This is where WebClient's non-blocking nature is deliberately 'blocked' to fit the pattern
        String aiResponse = webClient.post()
                .uri("/chat") // Placeholder for actual chat endpoint
                .headers(h -> h.setBearerAuth(authToken)) // Add Authorization header
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10)); // BLOCKING CALL

        if (aiResponse == null) {
            throw new IllegalStateException("AI response was null after blocking call.");
        }
        logger.debug("Real (blocking): AI raw response: {}", aiResponse);
        return aiResponse;
    }

    private Boolean blockingValidateAIResponse(String aiResponse) throws Exception {
        logger.info("Real (blocking): Validating AI response (first 50 chars): '{}'", aiResponse.substring(0, Math.min(aiResponse.length(), 50)));
        // This method should perform actual validation logic, not simulated latency.
        if (aiResponse == null || !aiResponse.contains("response")) {
            throw new IllegalArgumentException("Invalid AI response format");
        }
        return true;
    }

    private List<ChartData> blockingExtractChartData(String aiResponse) throws Exception {
        logger.info("Real (blocking): Extracting chart data from AI response (first 50 chars): '{}'", aiResponse.substring(0, Math.min(aiResponse.length(), 50)));
        // This method should perform actual data extraction, not simulated latency.
        if (!aiResponse.contains("chartData")) {
            throw new IllegalStateException("AI response did not contain chart data (mock check)");
        }
        // In a real scenario, this would parse JSON using ObjectMapper
        return List.of(
                new ChartData("Real Item X", ThreadLocalRandom.current().nextDouble(100, 200)),
                new ChartData("Real Item Y", ThreadLocalRandom.current().nextDouble(200, 300))
        );
    }

    private Boolean blockingAuthenticate(String username, String password) throws Exception {
        logger.info("Real (blocking): Attempting authentication for user: {}", username);
        String requestBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", username, password); // OpenWebUI uses 'email' for username

        try {
            String responseBody = this.webClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));

            if (responseBody == null) {
                logger.warn("Real (blocking): Authentication response body was null for user: {}", username);
                return false;
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            String receivedToken = rootNode.path("token").asText();

            if (receivedToken.isEmpty()) {
                logger.warn("Real (blocking): Authentication response did not contain a token for user: {}", username);
                return false;
            }

            this.authToken = receivedToken;
            logger.info("Real (blocking): Authentication successful for user: {}. Token received (first 10 chars): {}", username, receivedToken.substring(0, Math.min(receivedToken.length(), 10)));
            return true;
        } catch (WebClientResponseException.Unauthorized e) {
            logger.warn("Real (blocking): Authentication failed for user {} - Unauthorized: {}", username, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Real (blocking): Error during authentication for user {}: {}", username, e.getMessage(), e);
            return false;
        }
    }

    private Boolean blockingEmbedData(String data) throws Exception {
        if (authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Authentication token is not available. Please authenticate first.");
        }

        Path tempFile = null;
        try {
            // 1. Create a temporary file
            String filename = "embedding_data_" + UUID.randomUUID() + ".txt";
            tempFile = Files.createTempFile(filename, ".tmp");
            Files.write(tempFile, data.getBytes());
            logger.info("Created temporary file for embedding: {}", tempFile);

            // 2. Upload File to OpenWebUI
            logger.info("Uploading file to OpenWebUI: {}", tempFile.getFileName());
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new FileSystemResource(tempFile)).filename(tempFile.getFileName().toString());

            String fileUploadResponse = webClient.post()
                    .uri("/api/v1/files/")
                    .headers(h -> h.setBearerAuth(authToken)) 
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            if (fileUploadResponse == null || fileUploadResponse.isEmpty()) {
                throw new IOException("File upload to OpenWebUI failed: no response.");
            }
            JsonNode fileUploadJson = objectMapper.readTree(fileUploadResponse);
            String fileId = fileUploadJson.path("id").asText();
            if (fileId.isEmpty()) {
                throw new IOException("File upload to OpenWebUI failed: no file ID in response.");
            }
            logger.info("File uploaded to OpenWebUI with ID: {}", fileId);

            // 3. Poll for 'completed' status
            int maxRetries = 10;
            for (int i = 0; i < maxRetries; i++) {
                String statusResponse = webClient.get()
                        .uri("/api/v1/files/{fileId}/process/status", fileId)
                        .headers(h -> h.setBearerAuth(authToken))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(10));

                if (statusResponse == null || statusResponse.isEmpty()) {
                    throw new IOException("Polling for file status failed: no response.");
                }
                JsonNode statusJson = objectMapper.readTree(statusResponse);
                String status = statusJson.path("status").asText();

                if ("completed".equals(status)) {
                    logger.info("File processing completed for file ID: {}", fileId);
                    break;
                }
                if ("failed".equals(status)) {
                    throw new IOException("File processing failed for file ID: " + fileId);
                }

                logger.debug("Waiting for embedding: {} (attempt {}), current status: {}", tempFile.getFileName(), i + 1, status);
                Thread.sleep(2000); // Wait 2 seconds
            }

            // 4. Add to Knowledge Base
            logger.info("Attaching file {} to KB {}", fileId, openWebUIKbDefaultId);
            String addToFileKbResponse = webClient.post()
                    .uri("/api/v1/knowledge/{kbId}/file/add", openWebUIKbDefaultId)
                    .headers(h -> h.setBearerAuth(authToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(String.format("{\"file_id\": \"%s\"}", fileId))
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));

            logger.info("File {} successfully added to Knowledge Base {}", fileId, openWebUIKbDefaultId);
            return true;
        } finally {
            // 5. Clean up temporary file
            if (tempFile != null && Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                    logger.info("Deleted temporary file: {}", tempFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file {}: {}", tempFile, e.getMessage());
                }
            }
        }
    }

    private Map<String, String> blockingGetAllKnowledgeBases() throws Exception {
        if (authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Authentication token is not available. Please authenticate first.");
        }

        logger.info("Real (blocking): Fetching all knowledge bases.");
        String responseBody = webClient.get()
                .uri("/api/v1/knowledge/")
                .headers(h -> h.setBearerAuth(authToken))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

        if (responseBody == null || responseBody.isEmpty()) {
            throw new IOException("Failed to fetch knowledge bases: no response.");
        }

        JsonNode rootNode = objectMapper.readTree(responseBody);
        Map<String, String> knowledgeBases = new HashMap<>();
        if (rootNode.has("items") && rootNode.get("items").isArray()) {
            for (JsonNode item : rootNode.get("items")) {
                if (item.has("name") && item.has("id")) {
                    knowledgeBases.put(item.get("name").asText(), item.get("id").asText());
                }
            }
        }
        logger.info("Real (blocking): Fetched {} knowledge bases.", knowledgeBases.size());
        return knowledgeBases;
    }

    private List<String> blockingGetKnowledgeBaseFiles(String kbId) throws Exception {
        if (authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Authentication token is not available. Please authenticate first.");
        }

        logger.info("Real (blocking): Fetching files for knowledge base ID: {}", kbId);
        String responseBody = webClient.get()
                .uri("/api/v1/knowledge/{kbId}/files", kbId)
                .headers(h -> h.setBearerAuth(authToken))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

        if (responseBody == null || responseBody.isEmpty()) {
            throw new IOException("Failed to fetch knowledge base files: no response.");
        }

        JsonNode rootNode = objectMapper.readTree(responseBody);
        List<String> files = new ArrayList<>();
        if (rootNode.has("items") && rootNode.get("items").isArray()) {
            for (JsonNode item : rootNode.get("items")) {
                if (item.has("filename")) {
                    files.add(item.get("filename").asText());
                }
            }
        }
        logger.info("Real (blocking): Fetched {} files for knowledge base ID: {}", files.size(), kbId);
        return files;
    }

    // --- Public methods using SafeRunner ---

    @Override
    public Mono<Try<Boolean>> validatePrompt(String prompt) {
        return SafeRunner.futureSafe(() -> blockingValidatePrompt(prompt));
    }

    @Override
    public Mono<Try<String>> sendPromptToAI(String prompt, List<String> contextData) {
        return SafeRunner.futureSafe(() -> blockingSendPromptToAI(prompt, contextData));
    }

    @Override
    public Mono<Try<Boolean>> validateAIResponse(String aiResponse) {
        return SafeRunner.futureSafe(() -> blockingValidateAIResponse(aiResponse));
    }

    @Override
    public Flux<Try<ChartData>> extractChartData(String aiResponse) {
        return SafeRunner.futureStreamList(() -> blockingExtractChartData(aiResponse));
    }

    @Override
    public Mono<Try<Boolean>> embedData(String data) {
        return SafeRunner.futureSafe(() -> blockingEmbedData(data));
    }

    @Override
    public Mono<Try<Boolean>> authenticate(String username, String password) {
        return SafeRunner.futureSafe(() -> blockingAuthenticate(username, password));
    }

    @Override
    public Mono<Try<Map<String, String>>> getAllKnowledgeBases() {
        return SafeRunner.futureSafe(this::blockingGetAllKnowledgeBases);
    }

    @Override
    public Mono<Try<List<String>>> getKnowledgeBaseFiles(String kbId) {
        return SafeRunner.futureSafe(() -> blockingGetKnowledgeBaseFiles(kbId));
    }
}