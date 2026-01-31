package com.catgineer.analytics_assistant.infrastructure.ports;

import io.vavr.control.Try;

public interface DataSourceProvider {
    Try<String> fetchFrom(String url);
}
