package com.catgineer.analytics_assistant.domain.services;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartDataSet;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

import java.util.List;

public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final AIProvider aiProvider;

    public AIService(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    private ChartDataSet internalGenerateChartData(String prompt, String modelName) throws Exception {
        logger.info("Starting deterministic AI generation sequence with model: {}", modelName);

        Try<Boolean> validation = aiProvider.validatePrompt(prompt).toFuture().get();
        if (validation.isFailure() || !validation.get()) {
            logger.warn("Prompt validation rejected: {}", prompt);
            throw new IllegalArgumentException("Prompt validation failed");
        }

        logger.debug("Dispatching prompt to AI Provider for CSV generation");
        Try<String> aiResponse = aiProvider.sendPromptToAI(modelName, prompt, List.of()).toFuture().get();
        String csvContent = aiResponse.getOrElseThrow(() -> new RuntimeException("AI provider returned no content"));

        logger.debug("Validating AI response structure");
        Try<Boolean> responseValidation = aiProvider.validateAIResponse(csvContent).toFuture().get();
        if (responseValidation.isFailure() || !responseValidation.get()) {
            logger.error("AI response failed structural validation");
            throw new IllegalStateException("Malformed AI response");
        }

        logger.info("Extracting ChartDataSet from AI response");
        Try<ChartDataSet> extractionResult = aiProvider.extractChartDataSet(prompt, csvContent).toFuture().get();
        
        return extractionResult.getOrElseThrow(() -> {
            logger.error("Failed to map AI response to ChartDataSet");
            return new RuntimeException("Data extraction failed");
        });
    }

    private Boolean internalAuthenticate(String username, String password) throws Exception {
        logger.info("Executing synchronous authentication for: {}", username);
        return aiProvider.authenticate(username, password)
                .toFuture()
                .get()
                .getOrElseThrow(() -> new RuntimeException("Authentication provider unreachable"));
    }

    private Boolean internalEmbedData(String data) throws Exception {
        logger.info("Initiating data embedding into knowledge base");
        Try<Boolean> result = aiProvider.embedData(data).toFuture().get();
        
        return Match(result).of(
            Case($Success($(true)), () -> {
                logger.info("Data embedded successfully.");
                return true;
            }),
            Case($Failure($()), () -> { 
                throw new RuntimeException("Embedding process failed"); 
            })
        );
    }

    public Mono<Try<ChartDataSet>> generateChartData(String prompt, String modelName) {
        logger.info("Public Entry: Orchestrating chart data generation for prompt: {}", prompt);
        return SafeRunner.futureSafe(() -> internalGenerateChartData(prompt, modelName));
    }

    public Mono<Try<Boolean>> authenticate(String username, String password) {
        logger.info("Public Entry: Initiating authentication for user: {}", username);
        return SafeRunner.futureSafe(() -> internalAuthenticate(username, password));
    }

    public Mono<Try<Boolean>> embedData(String data) {
        return SafeRunner.futureSafe(() -> internalEmbedData(data));
    }
}
