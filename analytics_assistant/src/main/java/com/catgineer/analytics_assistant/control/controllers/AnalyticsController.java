package com.catgineer.analytics_assistant.control.controllers;

import com.catgineer.analytics_assistant.application.services.AnalyticsUseCase;
import io.vavr.control.Either;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsUseCase analyticsUseCase;

    public AnalyticsController(AnalyticsUseCase analyticsUseCase) {
        this.analyticsUseCase = analyticsUseCase;
    }

    @GetMapping("/api/analytics")
    public ResponseEntity<String> getAnalytics(@RequestParam String url) {
        Either<Throwable, String> result = analyticsUseCase.getAnalyticsData(url);

        return result.fold(
                left -> ResponseEntity.badRequest().body(left.getMessage()),
                ResponseEntity::ok
        );
    }
}
