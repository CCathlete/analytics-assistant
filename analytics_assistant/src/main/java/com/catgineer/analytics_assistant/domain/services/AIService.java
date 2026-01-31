package com.catgineer.analytics_assistant.domain.services;

import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final AIProvider aiProvider;

    public AIService(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public Flux<Try<ChartData>> processPromptForCharts(String prompt, List<String> rawData) {
        logger.info("Starting AI processing for prompt: '{}' with {} raw data entries.", prompt, rawData.size());

        // Step 1: Validate the prompt
        return aiProvider.validatePrompt(prompt)
                .flatMapMany(tryValidation -> {
                    if (tryValidation.isFailure()) {
                        logger.warn("Prompt validation failed: {}", tryValidation.getCause().getMessage());
                        return Flux.just(Try.failure(tryValidation.getCause()));
                    }
                    if (!tryValidation.get()) { // Assuming validatePrompt returns Try.success(false) for non-harmful but invalid
                        return Flux.just(Try.failure(new IllegalArgumentException("Prompt is invalid.")));
                    }
                    logger.debug("Prompt validated successfully.");
                    // Step 2: Send prompt to AI
                    return aiProvider.sendPromptToAI(prompt, rawData);
                })
                .flatMapMany(tryAIResponse -> {
                    if (tryAIResponse.isFailure()) {
                        logger.warn("Sending prompt to AI failed: {}", tryAIResponse.getCause().getMessage());
                        return Flux.just(Try.failure(tryAIResponse.getCause()));
                    }
                    String aiResponse = tryAIResponse.get();
                    logger.debug("Received AI response (first 50 chars): {}", aiResponse.substring(0, Math.min(aiResponse.length(), 50)));

                    // Step 3: Validate AI response
                    return aiProvider.validateAIResponse(aiResponse)
                            .flatMapMany(tryResponseValidation -> {
                                if (tryResponseValidation.isFailure()) {
                                    logger.warn("AI response validation failed: {}", tryResponseValidation.getCause().getMessage());
                                    return Flux.just(Try.failure(tryResponseValidation.getCause()));
                                }
                                if (!tryResponseValidation.get()) {
                                    return Flux.just(Try.failure(new IllegalArgumentException("AI response is invalid.")));
                                }
                                logger.debug("AI response validated successfully.");
                                // Step 4: Extract chart data
                                return aiProvider.extractChartData(aiResponse);
                            });
                })
                .doOnComplete(() -> logger.info("Finished AI processing for prompt: '{}'.", prompt))
                .doOnError(e -> logger.error("Error during AI processing pipeline for prompt '{}': {}", prompt, e.getMessage(), e));
    }
}
