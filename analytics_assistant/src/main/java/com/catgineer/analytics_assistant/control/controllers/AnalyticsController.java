package com.catgineer.analytics_assistant.control.controllers;

import com.catgineer.analytics_assistant.application.services.AnalyticsUseCase;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.*;
import static io.vavr.Patterns.*; // For $Success and $Failure

@RestController
public class AnalyticsController {

    private final AnalyticsUseCase analyticsUseCase;

    public AnalyticsController(AnalyticsUseCase analyticsUseCase) {
        this.analyticsUseCase = analyticsUseCase;
    }

    @GetMapping("/api/analytics")
    public ResponseEntity<?> getAnalytics() { // Use ResponseEntity<?> to allow List<String>
        Try<List<String>> result = analyticsUseCase.getAnalyticsData();

        return Match(result).of(
                Case($Failure($()), error -> ResponseEntity.badRequest().body(error.getMessage())),
                Case($Success($()), successList -> ResponseEntity.ok(successList.toJavaList())) // Return List<String> as JSON array
        );
    }
}
