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

    private Integer internalExecuteFlow(String prompt, String modelName, List<String> sourceUrls, Integer targetDatasetId
    ) throws Exception {
            logger.info("Executing ingestion and generation flow for model: {}", modelName);

        // STEP 1: Ingestion
        // fetchMultipleSources returns Flux<Try<SourceData>>, so we get a List<Try<SourceData>>
        List<Try<SourceData>> ingestionResults = dataSourceService.fetchMultipleSources(sourceUrls)
                .collectList()
                .toFuture()
                .get();
        // STEP 2: Explicitly process results and trigger Embedding
        //
        List<Try<Boolean>> embeddingResults = ingestionResults.stream()
                .map(sourceResult -> Match(sourceResult).of(
                        Case($Success($()), (SourceData data) -> {
                            logger.debug("Source fetched: {}. Triggering embedding.", data.sourceUrl());
                            
                            Try<Try<Boolean>> embedOperation = Try.of(() -> 
                                aiService.embedData(data.content()).toFuture().get()
                            );

                            return Match(embedOperation).of(
                                Case($Success($()), (Try<Boolean> embeddingSuccessTry) -> {
                                    logger.info("Embedding call completed for {}", data.sourceUrl());
                                    return embeddingSuccessTry;
                                }),
                                Case($Failure($()), ex -> {
                                    logger.error("IO Failure during embedding for {}", data.sourceUrl());
                                    return Try.<Boolean>failure(ex);
                                })
                            );
                        }),
                        Case($Failure($()), ex -> {
                            logger.warn("Source fetch failed, skipping embedding: {}", ex.getMessage());
                            return Try.<Boolean>failure(ex);
                        })
                )).toList();

        // Explicit Validation of the Embedding Phase.
        long successfulEmbeddings = embeddingResults.stream()
                .filter(res -> res.isSuccess() && res.get())
                .count();

        if (successfulEmbeddings == 0 && !sourceUrls.isEmpty()) {
            logger.error("Data platform halt: 0/%d sources embedded successfully.", sourceUrls.size());
            throw new RuntimeException("Knowledge base ingestion failed - aborting generation");
        }

        logger.info("Ingestion complete. {} sources ready in knowledge base.", successfulEmbeddings);

        // STEP 3: Generate Chart Data
        //
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



        // STEP 4: Visualisation Sync

        Try<Integer> syncResult = visualisationService.syncDataToVisualisation(dataSet, targetDatasetId)

                .toFuture()

                .get();

        return Match(syncResult).of(

                Case($Success($()), id -> {

                    logger.info("Successfully synchronized dataset: {}", id);

                    return id;

                }),

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
