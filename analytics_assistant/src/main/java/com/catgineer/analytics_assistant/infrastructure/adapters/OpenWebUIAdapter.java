package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Profile("!mock")
public class OpenWebUIAdapter implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenWebUIAdapter.class);
    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;
    private String authToken;

    @Value("${OPENWEBUI_API_BASEURL}")
    private String openWebUIApiBaseUrl;

    @Value("${SCP_REMOTE_USER}")
    private String scpRemoteUser;

    @Value("${SCP_REMOTE_HOST}")
    private String scpRemoteHost;

    @Value("${SCP_REMOTE_PATH}")
    private String scpRemotePath;

    public OpenWebUIAdapter(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(openWebUIApiBaseUrl).build();
        logger.info("OpenWebUIAdapter (SCP mode) initialized for host: {}", scpRemoteHost);
    }

    // --- Private Deterministic Logic ---

    private Boolean internalAuthenticate(String username, String password) {
        logger.info("Authenticating for AI services: {}", username);
        
        return webClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", username, "password", password))
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> {
                    this.authToken = (String) res.get("token");
                    return true;
                })
                .block(Duration.ofSeconds(10));
    }

    private String internalSendPrompt(String prompt, List<String> contextData) {
        return webClient.post()
                .uri("/chat")
                .headers(h -> h.setBearerAuth(authToken))
                .bodyValue(Map.of("prompt", prompt, "context", String.join(" ", contextData)))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));
    }

    private Boolean internalEmbedViaScp(String data) throws Exception {
        Path tempFile = Files.createTempFile("aa_embedding_" + UUID.randomUUID(), ".txt");
        String remoteDestination = String.format("%s@%s:%s/%s", 
                scpRemoteUser, scpRemoteHost, scpRemotePath, tempFile.getFileName());

        try {
            Files.writeString(tempFile, data);
            logger.info("Transferring data via SCP to: {}", remoteDestination);

            ProcessBuilder pb = new ProcessBuilder(
                    "scp", 
                    "-o", "StrictHostKeyChecking=no", 
                    tempFile.toString(), 
                    remoteDestination
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("SCP transfer failed with exit code: " + exitCode);
            }

            logger.info("SCP transfer completed successfully.");
            return true;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String internalExtractCsv(String aiResponse) {
        logger.info("Extracting CSV content from AI response.");
        
        if (aiResponse == null || aiResponse.isBlank()) {
            throw new IllegalArgumentException("Cannot extract data from empty response");
        }

        Pattern pattern = Pattern.compile("(?s)```(?:csv)?\\n(.*?)\\n```");
        Matcher matcher = pattern.matcher(aiResponse);

        String cleanCsv;
        if (matcher.find()) {
            cleanCsv = matcher.group(1).trim();
            logger.debug("CSV found within markdown blocks.");
        } else {
            cleanCsv = aiResponse.trim();
            logger.debug("No markdown blocks found, using raw response.");
        }

        if (cleanCsv.isEmpty()) {
            throw new IllegalStateException("Extracted CSV is empty");
        }

        return cleanCsv;
    }

    // --- Public API ---

    @Override
    public Mono<Try<Boolean>> authenticate(String username, String password) {
        return SafeRunner.futureSafe(() -> internalAuthenticate(username, password));
    }

    @Override
    public Mono<Try<String>> sendPromptToAI(String prompt, List<String> contextData) {
        return SafeRunner.futureSafe(() -> internalSendPrompt(prompt, contextData));
    }

    @Override
    public Mono<Try<Boolean>> embedData(String data) {
        return SafeRunner.futureSafe(() -> internalEmbedViaScp(data));
    }

    @Override
    public Mono<Try<Boolean>> validatePrompt(String prompt) {
        return SafeRunner.futureSafe(() -> {
            if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("Empty prompt");
            return true;
        });
    }

    @Override
    public Mono<Try<Boolean>> validateAIResponse(String aiResponse) {
        return SafeRunner.futureSafe(() -> aiResponse != null && !aiResponse.isBlank());
    }

    @Override
    public Flux<Try<String>> extractChartData(String aiResponse) {
        return SafeRunner.futureStream(() -> internalExtractCsv(aiResponse));
    }
}
