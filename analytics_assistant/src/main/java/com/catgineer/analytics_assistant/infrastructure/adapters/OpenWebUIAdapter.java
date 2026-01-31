package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors; // Added for List.of which is used in mock, but also for real parsing

@Component
@Profile("!mock") // This bean will be active unless the "mock" profile is active
public class OpenWebUIAdapter implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenWebUIAdapter.class);
    private final WebClient webClient;

    private final String openWebUIApiBaseUrl;

    public OpenWebUIAdapter(WebClient.Builder webClientBuilder) {
        this.openWebUIApiBaseUrl = "http://localhost:8080/openwebui/api"; // Placeholder
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
}
