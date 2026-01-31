package com.catgineer.analytics_assistant.infrastructure.ports;

import reactor.core.publisher.Mono;

public interface DataSourceProvider {
    Mono<String> fetchFrom(String url);
}
