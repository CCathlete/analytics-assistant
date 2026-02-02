package com.catgineer.analytics_assistant.application.services;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartDataSet;
import com.catgineer.analytics_assistant.domain.model.SourceData;
import com.catgineer.analytics_assistant.domain.services.AIService;
import com.catgineer.analytics_assistant.domain.services.DataSourceService;
import com.catgineer.analytics_assistant.domain.services.VisualisationService;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

@Service
public class GenerateChartFromPrompt {

    private static final Logger logger = LoggerFactory.getLogger(GenerateChartFromPrompt.class);
    private final AIService aiService;
    private final VisualisationService visualisationService;
    private final DataSourceService dataSourceService;

    public GenerateChartFromPrompt(
            DataSourceService dataSourceService,
            AIService aiService,
            VisualisationService visualisationService
    ) {
        this.dataSourceService = dataSourceService;
        this.aiService = aiService;
        this.visualisationService = visualisationService;
    }

    private Integer internalExecuteFlow(String prompt, String modelName, List<String> sourceUrls, Integer targetDatasetId) throws Exception {
        logger.info("Executing monadic orchestration flow for model: {}", modelName);

        // PHASE 1: Crawl & Embed
        Try<List<Try<SourceData>>> ingestionStep = dataSourceService.fetchMultipleSources(sourceUrls)
                .collectList()
                .toFuture()
                .get();

        Match(ingestionStep).of(
            Case($Success($()), results -> {
                results.forEach(res -> Match(res).of(
                    Case($Success($()), data -> {
                        try {
                            return aiService.embedData(data.content()).toFuture().get();
                        } catch (Exception e) {
                            logger.error("Embedding failed for {}", data.url());
                            return Try.failure(e);
                        }
                    }),
                    Case($Failure($()), ex -> {
                        logger.warn("Source fetch failed: {}", ex.getMessage());
                        return Try.failure(ex);
                    })
                ));
                return null;
            }),
            Case($Failure($()), ex -> {
                throw new RuntimeException("Crawl phase failed", ex);
            })
        );

        // PHASE 2: Generate Chart Data
        Try<ChartDataSet> aiResult = aiService.generateChartData(prompt, modelName)
                .toFuture()
                .get();

        ChartDataSet dataSet = Match(aiResult).of(
            Case($Success($()), data -> data),
            Case($Failure($()), ex -> {
                logger.error("AI extraction failed");
                throw new RuntimeException("Generation phase failed", ex);
            })
        );

        // PHASE 3: Visualisation Sync
        Try<Integer> syncResult = visualisationService.syncDataToVisualisation(dataSet, targetDatasetId)
                .toFuture()
                .get();

        return Match(syncResult).of(
            Case($Success($()), id -> id),
            Case($Failure($()), ex -> {
                logger.error("Sync failed for dataset: {}", targetDatasetId);
                throw new RuntimeException("Visualisation phase failed", ex);
            })
        );
    }

    public Mono<Try<Integer>> execute(String prompt, String modelName, List<String> sourceUrls, Integer targetDatasetId) {
        return SafeRunner.futureSafe(() -> internalExecuteFlow(prompt, modelName, sourceUrls, targetDatasetId));
    }
}
