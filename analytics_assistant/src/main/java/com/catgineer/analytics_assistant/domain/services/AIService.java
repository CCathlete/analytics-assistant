package com.catgineer.analytics_assistant.domain.services;

import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.infrastructure.ports.AIProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

import java.util.List;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final AIProvider aiProvider;

    public AIService(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public Flux<Try<ChartData>> processPromptForCharts(String prompt, List<String> rawData) {
        logger.info("Initiating chart data processing for prompt: [{}]", prompt);

        return aiProvider.validatePrompt(prompt)
            .flatMap(res -> Match(res).<Mono<Try<String>>>of(
                Case($Success($(true)), _unused -> {
                    logger.debug("Prompt validation successful.");
                    return aiProvider.sendPromptToAI(prompt, rawData);
                }),
                Case($Success($(false)), _unused -> {
                    logger.warn("Prompt validation failed: criteria not met.");
                    return Mono.just(Try.failure(new IllegalArgumentException("Invalid prompt")));
                }),
                Case($Failure($()), ex -> {
                    logger.error("Error during prompt validation: {}", ex.getMessage());
                    return Mono.just(Try.failure(ex));
                })
            ))
            .flatMap(res -> Match(res).<Mono<Try<ChartData>>>of(
                Case($Success($()), aiResponse -> {
                    logger.debug("AI Response received, length: {}", aiResponse.length());
                    return aiProvider.validateAIResponse(aiResponse)
                        .flatMap(validTry -> Match(validTry).<Mono<Try<ChartData>>>of(
                            Case($Success($(true)), _unused -> {
                                logger.debug("AI Response validated. Extracting chart data...");
                                return aiProvider.extractChartData(aiResponse).single();
                            }),
                            Case($Success($(false)), _unused -> {
                                logger.warn("AI Response validation failed.");
                                return Mono.just(Try.failure(new IllegalStateException("AI Response invalid")));
                            }),
                            Case($Failure($()), ex -> {
                                logger.error("Error during AI response validation: {}", ex.getMessage());
                                return Mono.just(Try.failure(ex));
                            })
                        ));
                }),
                Case($Failure($()), ex -> {
                    logger.error("AI Provider failed to return response: {}", ex.getMessage());
                    return Mono.just(Try.failure(ex));
                })
            ))
            .flatMapMany(res -> Match(res).<Flux<Try<ChartData>>>of(
                Case($Success($()), chartData -> {
                    logger.info("Successfully extracted ChartData.");
                    return Flux.just(Try.success(chartData));
                }),
                Case($Failure($()), ex -> {
                    logger.error("Failed to extract final ChartData: {}", ex.getMessage());
                    return Flux.just(Try.failure(ex));
                })
            ));
    }

    public Mono<Try<Boolean>> authenticate(String username, String password) {
        logger.info("Authenticating user: [{}]", username);
        return aiProvider.authenticate(username, password)
            .map(res -> Match(res).<Try<Boolean>>of(
                Case($Success($()), ok -> {
                    if (ok) logger.info("Authentication successful for user: {}", username);
                    else logger.warn("Authentication failed for user: {}", username);
                    return Try.success(ok);
                }),
                Case($Failure($()), ex -> {
                    logger.error("Authentication process error for user {}: {}", username, ex.getMessage());
                    return Try.failure(ex);
                })
            ));
    }
}
