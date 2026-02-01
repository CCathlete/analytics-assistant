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

@Service
public class GenerateChartFromPrompt {

    private static final Logger logger = LoggerFactory.getLogger(GenerateChartFromPrompt.class);

    private final AIService aiService;
    private final VisualisationService visualisationService;

    public GenerateChartFromPrompt(
            AIService aiService,
            VisualisationService visualisationService
    ) {
        this.aiService = aiService;
        this.visualisationService = visualisationService;
    }

    private Integer internalExecuteFlow(String prompt, String modelName, Integer targetDatasetId) throws Exception {
        logger.info("Initiating application flow for model: {}", modelName);

        ChartDataSet dataSet = aiService.generateChartData(prompt, modelName)
                .toFuture()
                .get()
                .getOrElseThrow(() -> {
                    logger.error("AI Service failed to provide a valid ChartDataSet");
                    return new RuntimeException("AI extraction failed");
                });

        logger.info("AI dataset received. Handing over to VisualisationService.");

        return visualisationService.syncDataToVisualisation(dataSet, targetDatasetId)
                .toFuture()
                .get()
                .getOrElseThrow(() -> {
                    logger.error("Visualisation sync failed for dataset: {}", dataSet.id());
                    return new RuntimeException("Visualisation sync failed");
                });
    }

    public Mono<Try<Integer>> execute(String prompt, String modelName, Integer targetDatasetId) {
        logger.info("Orchestrating chart generation pipeline via SafeRunner");
        return SafeRunner.futureSafe(() -> internalExecuteFlow(prompt, modelName, targetDatasetId));
    }
}
