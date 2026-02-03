package com.catgineer.analytics_assistant.domain.model;

import java.time.Instant;
import java.util.List;

public record ChartDataSet(
    String id,
    String sourcePrompt,
    List<ChartData> dataPoints,
    Instant extractedAt
) {
    public ChartDataSet(String id, String sourcePrompt, List<ChartData> dataPoints) {
        this(id, sourcePrompt, dataPoints, Instant.now());
    }
}
