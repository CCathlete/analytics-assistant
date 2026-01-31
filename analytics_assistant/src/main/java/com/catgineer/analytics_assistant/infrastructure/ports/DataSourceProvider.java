package com.catgineer.analytics_assistant.infrastructure.ports;

import io.vavr.control.Either;

public interface DataSourceProvider {
    Either<Throwable, String> fetchFrom(String url);
}
