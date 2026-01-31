package com.catgineer.analytics_assistant.infrastructure.ports;

import com.catgineer.analytics_assistant.domain.model.ChartData;
import io.vavr.control.Try;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AIProvider {
    Mono<Try<Boolean>> validatePrompt(String prompt);
    Mono<Try<String>> sendPromptToAI(String prompt, List<String> contextData);
    Mono<Try<Boolean>> validateAIResponse(String aiResponse);
    Flux<Try<ChartData>> extractChartData(String aiResponse);
    Mono<Try<Boolean>> embedData(String data);
}
