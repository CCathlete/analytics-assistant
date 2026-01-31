package com.catgineer.analytics_assistant.infrastructure.ports;

import io.vavr.control.Try;
import reactor.core.publisher.Mono;

public interface VisualisationProvider {
    Mono<Try<Boolean>> updateDataset(String csvData);
}
