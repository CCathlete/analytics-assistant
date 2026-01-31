package com.catgineer.analytics_assistant.control.controllers;

import com.catgineer.analytics_assistant.application.services.AnalyticsUseCase;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import io.vavr.API;
import io.vavr.control.Try;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static io.vavr.API.*;
import static io.vavr.Patterns.*; // For $Success and $Failure

@RestController
public class AnalyticsController {

    private final AnalyticsUseCase analyticsUseCase;

    public AnalyticsController(AnalyticsUseCase analyticsUseCase) {
        this.analyticsUseCase = analyticsUseCase;
    }

    @GetMapping("/api/analytics")
    public Mono<ResponseEntity<?>> getAnalytics(@RequestParam String prompt) {
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
                        return ResponseEntity.badRequest().body("Errors occurred: " + String.join("; ", errors));
                    } else if (successfulCharts.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No chart data generated.");
                    } else {
                        return ResponseEntity.ok().body(successfulCharts);
                    }
                })
                .onErrorResume(error -> {
                    // This catches errors from the upstream reactive pipeline (e.g., from analyticsUseCase.getAnalyticsData itself)
                    // Log the error for debugging
                    // logger.error("Unexpected error in getAnalytics: {}", error.getMessage(), error); // Need a logger here if desired
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + error.getMessage()));
                });
    }
}
