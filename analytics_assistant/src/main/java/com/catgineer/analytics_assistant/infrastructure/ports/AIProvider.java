package com.catgineer.analytics_assistant.infrastructure.ports;

import io.vavr.control.Try;
import reactor.core.publisher.Mono;

import java.util.List;

import com.catgineer.analytics_assistant.domain.model.ChartDataSet;

public interface AIProvider {
    Mono<Try<Boolean>> validatePrompt(String prompt);
    Mono<Try<String>> sendPromptToAI(String model, String prompt, List<String> contextData);
    Mono<Try<Boolean>> validateAIResponse(String aiResponse);
    Mono<Try<ChartDataSet>> extractChartDataSet(String prompt, String aiResponse);
    Mono<Try<Boolean>> embedData(String data);
    Mono<Try<Boolean>> authenticate(String username, String password);
}
