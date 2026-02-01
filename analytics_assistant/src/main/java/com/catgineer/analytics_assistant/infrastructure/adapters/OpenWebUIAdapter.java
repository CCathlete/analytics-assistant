package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.domain.model.ChartDataSet;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import tools.jackson.databind.JsonNode;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Profile("!mock")
public class OpenWebUIAdapter implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenWebUIAdapter.class);
    private RestClient restClient;
    private String authToken;

    @Value("${OPENWEBUI_API_BASEURL}")
    private String openWebUIApiBaseUrl;

    @Value("${SCP_REMOTE_USER}")
    private String scpRemoteUser;

    @Value("${SCP_REMOTE_HOST}")
    private String scpRemoteHost;

    @Value("${SCP_REMOTE_PATH}")
    private String scpRemotePath;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .baseUrl(openWebUIApiBaseUrl)
                .build();
        logger.info("OpenWebUIAdapter initialized for URL: {}", openWebUIApiBaseUrl);
    }

    private Boolean internalAuthenticate(String username, String password) {
        logger.info("Attempting login for user: {}", username);
        JsonNode res = restClient.post()
                .uri("/api/v1/auth/login")
                .body(Map.of("email", username, "password", password))
                .retrieve()
                .body(JsonNode.class);

        if (res == null || res.path("token").isMissingNode()) {
            logger.error("Auth failed: No token returned for {}", username);
            return false;
        }

        this.authToken = res.get("token").asString("");
        logger.info("Auth successful for {}", username);
        return true;
    }

    private String internalSendPrompt(String prompt, List<String> contextData) {
        logger.info("Requesting AI completion. Context lines: {}", contextData.size());
        if (authToken == null) throw new IllegalStateException("Missing auth token");

        return restClient.post()
                .uri("/chat")
                .headers(h -> h.setBearerAuth(authToken))
                .body(Map.of("prompt", prompt, "context", String.join(" ", contextData)))
                .retrieve()
                .body(String.class);
    }

    private ChartDataSet internalExtractDataSet(String prompt, String aiResponse) {
        logger.info("Extracting dataset from AI response.");
        Pattern pattern = Pattern.compile("(?s)```(?:csv)?\\n(.*?)\\n```");
        Matcher matcher = pattern.matcher(aiResponse);
        String csv = matcher.find() ? matcher.group(1).trim() : aiResponse.trim();

        if (csv.isEmpty()) {
            logger.error("Failed to find CSV content in AI response");
            throw new IllegalStateException("Empty CSV content");
        }

        List<ChartData> points = Arrays.stream(csv.split("\\n"))
                .filter(line -> !line.isBlank())
                .map(line -> {
                    String[] p = line.split(",");
                    if (p.length < 2) throw new IllegalArgumentException("Bad CSV row: " + line);
                    return new ChartData(p[0].trim(), Double.parseDouble(p[1].trim()));
                })
                .collect(Collectors.toList());

        logger.info("Successfully parsed {} rows into ChartDataSet", points.size());
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
    public Mono<Try<Boolean>> validatePrompt(String prompt) {
        return SafeRunner.futureSafe(() -> {
            boolean ok = prompt != null && !prompt.isBlank();
            if (!ok) logger.warn("Blank prompt rejected");
            return ok;
        });
    }

    @Override
    public Mono<Try<Boolean>> validateAIResponse(String aiResponse) {
        return SafeRunner.futureSafe(() -> {
            boolean ok = aiResponse != null && !aiResponse.contains("ERROR");
            if (!ok) logger.warn("AI response validation failed");
            return ok;
        });
    }

    @Override
    public Mono<Try<ChartDataSet>> extractChartDataSet(String prompt, String aiResponse) {
        return SafeRunner.futureSafe(() -> internalExtractDataSet(prompt, aiResponse));
    }

    @Override
    public Mono<Try<Boolean>> embedData(String data) {
        return SafeRunner.futureSafe(() -> {
            logger.info("Triggering SCP embedding via Virtual Thread");
            return internalEmbedViaScp(data);
        });
    }

    private Boolean internalEmbedViaScp(String data) throws Exception {
        Path tempFile = Files.createTempFile("embed_", ".txt");
        try {
            Files.writeString(tempFile, data);
            ProcessBuilder pb = new ProcessBuilder("scp", "-o", "StrictHostKeyChecking=no", tempFile.toString(), 
                String.format("%s@%s:%s", scpRemoteUser, scpRemoteHost, scpRemotePath));
            return pb.start().waitFor() == 0;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
