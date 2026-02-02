package com.catgineer.analytics_assistant.control.controllers;

import com.catgineer.analytics_assistant.application.services.GenerateChartFromPrompt;
import com.catgineer.analytics_assistant.domain.services.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

record AuthRequest(String username, String password) {}
record ChartRequest(String prompt, String modelName, List<String> sourceUrls, Integer targetDatasetId) {}
record ChartResponse(Integer datasetId, String supersetUrl) {}

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final GenerateChartFromPrompt generateChartService;
    private final AIService aiService;
    
    private final String supersetBaseUrl;

    public AnalyticsController(
            GenerateChartFromPrompt generateChartService, 
            AIService aiService,
            @Value("${SUPERSET_BASE_URL}") String supersetBaseUrl
    ) {
        this.generateChartService = generateChartService;
        this.aiService = aiService;
        this.supersetBaseUrl = supersetBaseUrl;
    }

    @PostMapping("/auth")
    public Mono<ResponseEntity<String>> login(@RequestBody AuthRequest request) {
        return aiService.authenticate(request.username(), request.password())
            .map(authTry -> Match(authTry).of(
                Case($Success($(true)), b -> ResponseEntity.ok("Authenticated")),
                Case($Success($(false)), b -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Credentials")),
                Case($Failure($()), ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            ));
    }

    @PostMapping("/charts/generate")
    public Mono<ResponseEntity<ChartResponse>> generateChart(@RequestBody ChartRequest request) {
        return generateChartService.execute(
                request.prompt(), 
                request.modelName(), 
                request.sourceUrls(), 
                request.targetDatasetId()
            )
            .map(resultTry -> Match(resultTry).of(
                Case($Success($()), id -> {
                    // Using the injected ENV var for superset.
                    String url = String.format("%s/explore/?dataset_id=%d&standalone=true", supersetBaseUrl, id);
                    return ResponseEntity.ok(new ChartResponse(id, url));
                }),
                Case($Failure($()), ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            ));
    }
}
