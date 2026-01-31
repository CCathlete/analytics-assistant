package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockOpenWebUIAdapter implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockOpenWebUIAdapter.class);
    private final WebClient webClient; // WebClient is not strictly used in mock but kept for consistent constructor

    public MockOpenWebUIAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    // --- Private blocking methods ---

    private Boolean blockingValidatePrompt(String prompt) throws Exception {
        logger.info("Mock (blocking): Validating prompt: '{}'", prompt);
        Thread.sleep(50); // Simulate latency
        if (prompt != null && prompt.contains("harmful")) {
            throw new IllegalArgumentException("Prompt contains harmful content");
        }
        return true;
    }

    private String blockingSendPromptToAI(String prompt, List<String> contextData) throws Exception {
        logger.info("Mock (blocking): Sending prompt to AI: '{}' with {} context data entries.", prompt, contextData.size());
        Thread.sleep(100); // Simulate latency

        if (contextData.isEmpty()) {
            throw new IllegalStateException("No context data provided for AI");
        }
        String mockJsonResponse = String.format("""
            {
              "status": "success",
              "response": "Here is the data for your prompt '%s'. " +
                          "Context data items: %d. " +
                          "Values for chart: %.2f, %.2f, %.2f",
              "chartData": [
                { "label": "Item A", "value": %.2f },
                { "label": "Item B", "value": %.2f },
                { "label": "Item C", "value": %.2f }
              ]
            }
            """, prompt, contextData.size(), ThreadLocalRandom.current().nextDouble(10, 50), ThreadLocalRandom.current().nextDouble(50, 90), ThreadLocalRandom.current().nextDouble(20, 70),
            ThreadLocalRandom.current().nextDouble(10, 50), ThreadLocalRandom.current().nextDouble(50, 90), ThreadLocalRandom.current().nextDouble(20, 70));
        return mockJsonResponse;
    }

    private Boolean blockingValidateAIResponse(String aiResponse) throws Exception {
        logger.info("Mock (blocking): Validating AI response (first 50 chars): '{}'", aiResponse.substring(0, Math.min(aiResponse.length(), 50)));
        Thread.sleep(30); // Simulate latency
        if (aiResponse == null || !aiResponse.contains("status")) {
            throw new IllegalArgumentException("Invalid AI response format");
        }
        return true;
    }

    private List<ChartData> blockingExtractChartData(String aiResponse) throws Exception {
        logger.info("Mock (blocking): Extracting chart data from AI response (first 50 chars): '{}'", aiResponse.substring(0, Math.min(aiResponse.length(), 50)));
        Thread.sleep(70); // Simulate latency
        if (!aiResponse.contains("chartData")) {
            throw new IllegalStateException("AI response did not contain chart data (mock check)");
        }
        return List.of(
                new ChartData("Mock Chart A", ThreadLocalRandom.current().nextDouble(10, 50)),
                new ChartData("Mock Chart B", ThreadLocalRandom.current().nextDouble(50, 90)),
                new ChartData("Mock Chart C", ThreadLocalRandom.current().nextDouble(20, 70))
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

    @Override
    public Mono<Try<Boolean>> embedData(String data) {
        logger.info("Mock: Embedding data (length: {})", data.length());
        return Mono.just(Try.success(true));
    }
}
