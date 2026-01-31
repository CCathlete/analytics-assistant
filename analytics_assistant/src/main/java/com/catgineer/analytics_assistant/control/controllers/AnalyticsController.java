package com.catgineer.analytics_assistant.control.controllers;

import com.catgineer.analytics_assistant.application.services.AnalyticsUseCase;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static io.vavr.API.*;
import static io.vavr.Patterns.*; // For $Success and $Failure

@RestController
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsUseCase analyticsUseCase;

    public AnalyticsController(AnalyticsUseCase analyticsUseCase) {
        this.analyticsUseCase = analyticsUseCase;
    }

    @GetMapping("/api/analytics")
    public Mono<ResponseEntity<?>> getAnalytics(@RequestParam String prompt) {
        logger.info("Received request for analytics with prompt: {}", prompt);
        return analyticsUseCase.getAnalyticsData(prompt)
                .collectList() // Collect Flux<Try<ChartData>> into Mono<List<Try<ChartData>>>
                .map(tryChartDataList -> {
                    List<ChartData> successfulCharts = tryChartDataList.stream()
                            .filter(Try::isSuccess)
                            .map(Try::get)
                            .collect(Collectors.toList());

                    List<String> errors = tryChartDataList.stream()
                            .filter(Try::isFailure)
                            .map(Try::getCause)
                            .map(Throwable::getMessage)
                            .collect(Collectors.toList());

                    if (!errors.isEmpty()) {
                        logger.warn("Errors encountered during analytics data processing: {}", errors);
                        return ResponseEntity.badRequest().body("Errors occurred: " + String.join("; ", errors));
                    } else if (successfulCharts.isEmpty()) {
                        logger.info("No chart data generated for prompt: {}", prompt);
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No chart data generated.");
                    } else {
                        logger.info("Successfully generated {} chart data entries for prompt: {}", successfulCharts.size(), prompt);
                        return ResponseEntity.ok().body(successfulCharts);
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Unexpected error in getAnalytics: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + error.getMessage()));
                });
    }

    @PostMapping("/api/auth")
    public Mono<ResponseEntity<?>> authenticate(@RequestBody AuthRequest authRequest) {
        logger.info("Received authentication request for user: {}", authRequest.getUsername());
        return analyticsUseCase.authenticate(authRequest.getUsername(), authRequest.getPassword())
                .map(tryAuth -> {
                    if (tryAuth.isSuccess() && tryAuth.get()) {
                        logger.info("Authentication successful for user: {}", authRequest.getUsername());
                        return ResponseEntity.ok().body("Authentication successful");
                    } else {
                        logger.warn("Authentication failed for user: {}. Reason: {}", authRequest.getUsername(),
                                tryAuth.isFailure() ? tryAuth.getCause().getMessage() : "Invalid credentials");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Unexpected error during authentication for user {}: {}", authRequest.getUsername(), error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during authentication."));
                });
    }
