package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.domain.model.ChartDataSet;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import tools.jackson.databind.JsonNode;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

record ChatMessage(String role, String content) {}
record ChatCompletionRequest(String model, List<ChatMessage> messages) {}

public class OpenWebUIAdapter implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenWebUIAdapter.class);
    private final RestClient restClient;
    private final RestClient bridgeClient; // Client for the Android Python bridge(embedding).
    private final String baseUrl;

    public OpenWebUIAdapter(
            RestClient.Builder restClientBuilder, 
            String baseUrl, 
            String apiKey,
            String bridgeUrl // Injected from EMBEDDING_NODE_URL.
    ) {
        
        this.baseUrl = baseUrl;

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        this.bridgeClient = restClientBuilder
                .baseUrl(bridgeUrl)
                .build();
        
        logger.info("OpenWebUIAdapter initialized. Bridge Target: {}", bridgeUrl);
    }

    private Boolean internalAuthenticate(String username, String password) {
        logger.info("Verifying credentials for: {}", username);
        JsonNode res = restClient.post()
                .uri("/api/v1/auth/login")
                .body(Map.of("email", username, "password", password))
                .retrieve()
                .body(JsonNode.class);

        boolean success = res != null && !res.path("token").isMissingNode();
        if (success) logger.info("Auth success.");
        else logger.warn("Auth failure.");
        return success;
    }

    private String internalSendPrompt(String model, String prompt, List<String> contextData) {
        final String urlSuffix = "/api/chat/completions";
        
        ChatCompletionRequest requestBody = new ChatCompletionRequest(
            model, 
            List.of(new ChatMessage("user", prompt))
        );

        logger.info("Dispatching AI Request to {}/{}", baseUrl, urlSuffix);
        logger.info("Request body: {}", requestBody);

        return restClient.post()
                .uri(urlSuffix)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody) // Jackson handles the record conversion
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    logger.error("OpenWebUI rejected request with status: {}", res.getStatusCode());
                })
                .body(String.class);
    }

    private Boolean internalEmbedViaBridge(String data) {
        // final int LIMIT = 150 * 1024; // 150KB limit
        final int LIMIT = 700; // 700B limit
        final String sourceId = UUID.randomUUID().toString().substring(0, 8);
        final int totalLength = data.length();
        final int totalChunks = (int) Math.ceil((double) totalLength / LIMIT);

        logger.info("Content size: {} bytes. Splitting into {} chunks for Source ID: {}", 
                    totalLength, totalChunks, sourceId);

        boolean allSuccessful = true;

        for (int i = 0; i < totalChunks; i++) {
            int start = i * LIMIT;
            int end = Math.min(start + LIMIT, totalLength);
            String chunk = data.substring(start, end);

            // Naming convention: embed_[sourceId]_[chunkIndex]_of_[total].txt
            String fileName = String.format("embed_%s_%d_of_%d.txt", sourceId, i + 1, totalChunks);
            
            logger.info("Pushing chunk {}/{} to Android bridge: {}", i + 1, totalChunks, fileName);

            var response = bridgeClient.post()
                    .uri("/" + fileName)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(chunk)
                    .retrieve()
                    .toBodilessEntity();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Chunk {} failed with status: {}", i + 1, response.getStatusCode());
                allSuccessful = false;
                break; // Stop if one chunk fails to maintain integrity
            }
        }

        if (allSuccessful) {
            logger.info(
                "All {} chunks for Source {} successfully pushed to Android node.",
                totalChunks, sourceId);
        }
        
        return allSuccessful;
    }

    private ChartDataSet internalExtractDataSet(String prompt, String aiResponse) {
        logger.info("Parsing AI response.");
        Pattern pattern = Pattern.compile("(?s)```(?:csv)?\\n(.*?)\\n```");
        Matcher matcher = pattern.matcher(aiResponse);
        String csv = matcher.find() ? matcher.group(1).trim() : aiResponse.trim();

        if (csv.isEmpty()) throw new IllegalStateException("CSV content missing");

        List<ChartData> points = Arrays.stream(csv.split("\\n"))
                .filter(line -> line.contains(","))
                .map(line -> {
                    String[] p = line.split(",");
                    return new ChartData(p[0].trim(), Double.parseDouble(p[1].trim()));
                })
                .collect(Collectors.toList());

        return new ChartDataSet(prompt, points);
    }

    @Override
    public Mono<Try<Boolean>> authenticate(String username, String password) {
        return SafeRunner.futureSafe(() -> internalAuthenticate(username, password));
    }

    @Override
    public Mono<Try<String>> sendPromptToAI(String model, String prompt, List<String> contextData) {
        return SafeRunner.futureSafe(() -> internalSendPrompt(model, prompt, contextData));
    }

    @Override
    public Mono<Try<ChartDataSet>> extractChartDataSet(String prompt, String aiResponse) {
        return SafeRunner.futureSafe(() -> internalExtractDataSet(prompt, aiResponse));
    }

    @Override
    public Mono<Try<Boolean>> validatePrompt(String prompt) {
        return SafeRunner.futureSafe(() -> prompt != null && !prompt.isBlank());
    }

    @Override
    public Mono<Try<Boolean>> validateAIResponse(String aiResponse) {
        return SafeRunner.futureSafe(() -> aiResponse != null && !aiResponse.contains("error"));
    }

    @Override
    public Mono<Try<Boolean>> embedData(String data) {
        return SafeRunner.futureSafe(() -> internalEmbedViaBridge(data));
    }
}
