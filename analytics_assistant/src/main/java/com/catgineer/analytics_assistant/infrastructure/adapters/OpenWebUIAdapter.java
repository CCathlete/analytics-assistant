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
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OpenWebUIAdapter implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenWebUIAdapter.class);
    private final RestClient restClient;
    private final RestClient bridgeClient; // Client for the Android Python bridge(embedding).

    public OpenWebUIAdapter(
            RestClient.Builder restClientBuilder, 
            String baseUrl, 
            String apiKey,
            String bridgeUrl // Injected from EMBEDDING_NODE_URL.
    ) {
        
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

    private String internalSendPrompt(String prompt, List<String> contextData) {
        logger.info("Sending AI prompt.");
        return restClient.post()
                .uri("/api/chat/completed")
                .body(Map.of(
                    "model", "gpt-4o",
                    "messages", List.of(
                        Map.of("role", "system", "content", "Return CSV format only."),
                        Map.of("role", "user", "content", prompt + "\nContext: " + String.join("\n", contextData))
                    )
                ))
                .retrieve()
                .body(String.class);
    }

    private Boolean internalEmbedViaBridge(String data) {
        String fileName = "embed_" + UUID.randomUUID() + ".txt";
        logger.info("Pushing data to Android bridge: {}", fileName);
        
        var response = bridgeClient.post()
                .uri("/" + fileName)
                .contentType(MediaType.TEXT_PLAIN)
                .body(data)
                .retrieve()
                .toBodilessEntity();

        boolean success = response.getStatusCode().is2xxSuccessful();
        if (success) logger.info("Data successfully pushed to Android node.");
        else logger.error("Bridge transfer failed with status: {}", response.getStatusCode());
        
        return success;
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
    public Mono<Try<String>> sendPromptToAI(String prompt, List<String> contextData) {
        return SafeRunner.futureSafe(() -> internalSendPrompt(prompt, contextData));
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
