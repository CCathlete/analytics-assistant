package com.catgineer.analytics_assistant.domain.services;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.domain.model.ChartData;
import com.catgineer.analytics_assistant.domain.model.ChartDataSet;
import com.catgineer.analytics_assistant.infrastructure.ports.VisualisationProvider;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VisualisationService {

    private static final Logger logger = LoggerFactory.getLogger(VisualisationService.class);
    private final VisualisationProvider visualisationProvider;

    public VisualisationService(VisualisationProvider visualisationProvider) {
        this.visualisationProvider = visualisationProvider;
    }

    /**
     * Parallel mapping of the inner data points to minimaps. 
     **/
    private List<Map<String, Object>> internalMapToTableRows(List<ChartData> dataPoints) {
        logger.debug("Mapping {} domain points to raw table rows", dataPoints.size());
        return dataPoints.stream()
                .map(dp -> Map.of(
                        "label", (Object) dp.getLabel(),
                        "value", (Object) dp.getValue()
                ))
                .collect(Collectors.toList());
    }

    private Integer internalSyncLogic(ChartDataSet dataSet, Integer targetDatasetId) throws Exception {
        logger.info("Executing deterministic sync logic for dataset id: {}", dataSet.id());
        
        List<Map<String, Object>> rows = internalMapToTableRows(dataSet.dataPoints());
        String identifier = dataSet.id().replace("-", "_");

        logger.info("Writing {} rows to physical table: {}", rows.size(), identifier);
        Try<Boolean> writeResult = visualisationProvider.overwritePhysicalTable(identifier, rows).toFuture().get();
        if (writeResult.isFailure() || !writeResult.get()) {
            logger.error("Physical table write failed for {}", identifier);
            throw new RuntimeException("Data layer write failed");
        }

        logger.info("Triggering refresh for Superset dataset id: {}", targetDatasetId);
        Try<Boolean> refreshResult = visualisationProvider.refreshDataset(targetDatasetId).toFuture().get();
        if (refreshResult.isFailure() || !refreshResult.get()) {
            logger.error("Superset refresh failed for dataset id: {}", targetDatasetId);
            throw new RuntimeException("Presentation refresh failed");
        }

        logger.info("Creating chart for identifier: {}", identifier);
        Try<Integer> chartResult = visualisationProvider.createChart(targetDatasetId, "Chart_" + identifier, "bar", "{}")
                .toFuture()
                .get();

        Integer chartId = chartResult.getOrElseThrow(() -> {
            logger.error("Chart creation logic failed for identifier: {}", identifier);
            return new RuntimeException("Chart creation failed");
        });

        logger.info("Sync logic completed successfully. Generated Chart ID: {}", chartId);
        return chartId;
    }

    public Mono<Try<Integer>> syncDataToVisualisation(ChartDataSet dataSet, Integer targetDatasetId) {
        logger.info("Orchestrating safe async sync for dataset: {}", dataSet.id());
        return SafeRunner.futureSafe(() -> internalSyncLogic(dataSet, targetDatasetId));
    }
}
