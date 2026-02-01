package com.catgineer.analytics_assistant.infrastructure.ports;

import io.vavr.control.Try;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

public interface VisualisationProvider {
    // Data Layer (Postgres)
    Mono<Try<Boolean>> overwritePhysicalTable(String tableName, List<Map<String, Object>> data);

    // Presentation Layer (Superset)
    Mono<Try<Boolean>> refreshDataset(Integer datasetId);
    Mono<Try<Integer>> createChart(Integer datasetId, String chartName, String vizType, String paramsJson);
}
