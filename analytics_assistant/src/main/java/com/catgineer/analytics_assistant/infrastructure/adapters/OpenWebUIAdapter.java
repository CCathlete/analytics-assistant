package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import io.vavr.control.Try;
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
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@Profile("!mock") // This bean will be active unless the "mock" profile is active
public class OpenWebUIAdapter implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenWebUIAdapter.class);
    private final WebClient webClient;

    @Value("${openwebui.api.baseurl}")
    private String openWebUIApiBaseUrl;

    @Value("${scp.remote.user}")
    private String scpRemoteUser;

    @Value("${scp.remote.host}")
    private String scpRemoteHost;

    @Value("${scp.remote.path}")
    private String scpRemotePath;

    public OpenWebUIAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(openWebUIApiBaseUrl).build();
    }

    // --- Private blocking methods ---

    private Boolean blockingValidatePrompt(String prompt) throws Exception {
        logger.info("Real (blocking): Validating prompt: '{}'", prompt);
        // Simulate a blocking validation call
        Thread.sleep(50); // Simulate latency
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
        Thread.sleep(30); // Simulate latency
        if (aiResponse == null || !aiResponse.contains("response")) {
            throw new IllegalArgumentException("Invalid AI response format");
        }
        return true;
    }

    private List<ChartData> blockingExtractChartData(String aiResponse) throws Exception {
        logger.info("Real (blocking): Extracting chart data from AI response (first 50 chars): '{}'", aiResponse.substring(0, Math.min(aiResponse.length(), 50)));
        Thread.sleep(70); // Simulate latency
        // Simulate parsing - if the AI response contained chart data, parse it.
        if (!aiResponse.contains("chartData")) {
            throw new IllegalStateException("AI response did not contain chart data (mock check)");
        }
        // In a real scenario, this would parse JSON using ObjectMapper
        return List.of(
                new ChartData("Real Item X", ThreadLocalRandom.current().nextDouble(100, 200)),
                new ChartData("Real Item Y", ThreadLocalRandom.current().nextDouble(200, 300))
        );
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

    // --- Embed Data (SCP) ---

    private Boolean blockingEmbedData(String data) throws Exception {
        Path tempFile = null;
        try {
            // Create a temporary file
            String filename = "embedding_data_" + UUID.randomUUID() + ".txt";
            tempFile = Files.createTempFile(filename, ".tmp");
            Files.write(tempFile, data.getBytes());
            logger.info("Created temporary file for embedding: {}", tempFile);

            // Construct SCP command
            String scpCommand = String.format("scp %s %s@%s:%s",
                    tempFile.toAbsolutePath(),
                    scpRemoteUser,
                    scpRemoteHost,
                    scpRemotePath);

            logger.info("Executing SCP command: {}", scpCommand);

            // Execute SCP command
            Process process = Runtime.getRuntime().exec(scpCommand);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("SCP transfer successful for file: {}", tempFile);
                return true;
            } else {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                logger.error("SCP transfer failed for file: {}. Exit code: {}. Error: {}", tempFile, exitCode, errorOutput);
                throw new IOException("SCP transfer failed with exit code: " + exitCode + ". Error: " + errorOutput);
            }
        } finally {
            // Clean up temporary file
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

    @Override
    public Mono<Try<Boolean>> embedData(String data) {
        return SafeRunner.futureSafe(() -> blockingEmbedData(data));
    }
}
