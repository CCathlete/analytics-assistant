package com.catgineer.analytics_assistant.application.services;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartDataSet;
import com.catgineer.analytics_assistant.domain.services.AIService;
import com.catgineer.analytics_assistant.domain.services.VisualisationService;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

@Service
public class GenerateChartFromPrompt {

    private static final Logger logger = LoggerFactory.getLogger(GenerateChartFromPrompt.class);

    private final AIService aiService;
    private final VisualisationService visualisationService;

    public GenerateChartFromPrompt(
            AIService aiService,
            VisualisationService visualisationService) {
        this.aiService = aiService;
        this.visualisationService = visualisationService;
    }

    private Integer internalExecuteFlow(String prompt, String modelName, Integer targetDatasetId) throws Exception {
        logger.info("Processing prompt for model: {}", modelName);

        // Step 1: Get CSV-structured data from AI
        Try<ChartDataSet> extractionResult = aiService.generateChartData(prompt, modelName)
                .toFuture()
                .get();

        return Match(extractionResult).<Integer>of(
            Case($Success($()), dataSet -> {
                logger.info("AI generated {} data points. Syncing to visualization layer.", dataSet.dataPoints().size());
                
                // Step 2 & 3: Overwrite Table and Refresh Superset
                Try<Integer> syncResult = visualisationService.syncDataToVisualisation(dataSet, targetDatasetId)
                        .toFuture()
                        .get();

                return syncResult.getOrElseThrow(() -> {
                    logger.error("Failed to sync generated data to visualization layer");
                    return new RuntimeException("Sync failure");
                });
            }),
            Case($Failure($()), ex -> {
                logger.error("AI failed to generate CSV data: {}", ex.getMessage());
                throw new RuntimeException("AI generation failed", ex);
            })
        );
    }

    public Mono<Try<Integer>> execute(String prompt, String modelName, Integer targetDatasetId) {
        logger.info("Initiating chart generation pipeline for user prompt");
        return SafeRunner.futureSafe(() -> internalExecuteFlow(prompt, modelName, targetDatasetId));
    }
}
