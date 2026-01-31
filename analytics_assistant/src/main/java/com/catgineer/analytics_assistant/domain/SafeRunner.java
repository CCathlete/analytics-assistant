package com.catgineer.analytics_assistant.domain;

import io.vavr.control.Try;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List; // Added import
import java.util.concurrent.Callable;

public final class SafeRunner {

    private SafeRunner() {
        // Private constructor to prevent instantiation
    }

    /**
     * Executes a block of potentially blocking code that can throw an exception and wraps the outcome in a {@link Try}.
     * This is for synchronous, potentially blocking operations that you want to wrap in Vavr's Try.
     *
     * @param codeBlock The {@link Callable} codeBlock of code to execute, returning T.
     * @param <T>   The return type of the codeBlock.
     * @return A {@link Try} containing either the successful result or the exception.
     */
    public static <T> Try<T> safe(Callable<T> codeBlock) {
        return Try.of(codeBlock::call);
    }

    /**
     * Executes a block of potentially blocking code that produces T, and wraps the outcome in a {@link Mono} of a {@link Try}.
     * The Callable itself runs on a boundedElastic scheduler to prevent blocking the main reactive threads.
     *
     * @param codeBlock The {@link Callable} codeBlock of code to execute, returning T.
     * @param <T>   The return type of the codeBlock.
     * @return A {@link Mono} representing the outcome, emitting a Try<T> on success.
     */
    public static <T> Mono<Try<T>> futureSafe(Callable<T> codeBlock) {
        return Mono.fromCallable(() -> safe(codeBlock)) // Execute blocking code and wrap in Try, then lift Try into Mono
                .subscribeOn(Schedulers.boundedElastic()); // Run blocking code on a dedicated scheduler
    }

    /**
     * Executes a block of potentially blocking code that produces T, and wraps the outcome in a {@link Flux} of a {@link Try}.
     * The Callable itself runs on a boundedElastic scheduler to prevent blocking the main reactive threads.
     * The single result Try<T> is emitted as a Flux of one item.
     *
     * @param codeBlock The {@link Callable} codeBlock of code to execute, returning T.
     * @param <T>   The return type of the codeBlock.
     * @return A {@link Flux} representing the outcome, emitting a Try<T> on success as a single item.
     */
    public static <T> Flux<Try<T>> futureStream(Callable<T> codeBlock) {
        return Mono.fromCallable(() -> safe(codeBlock)) // Execute blocking code and wrap in Try, then lift Try into Mono
                .subscribeOn(Schedulers.boundedElastic())
                .flux(); // Convert the Mono<Try<T>> to a Flux<Try<T>> emitting one item
    }

    /**
     * Executes a block of potentially blocking code that produces a List<T>, and wraps the outcome into a {@link Flux} of {@link Try<T>}.
     * Each item in the list is emitted as a separate {@link Try<T>} in the Flux.
     * The Callable itself runs on a boundedElastic scheduler.
     *
     * @param codeBlock The {@link Callable} codeBlock of code to execute, returning List<T>.
     * @param <T>   The type of items in the list.
     * @return A {@link Flux} representing the outcome, emitting individual Try<T> for each item in the list on success.
     */
    public static <T> Flux<Try<T>> futureStreamList(Callable<List<T>> codeBlock) {
        return Mono.fromCallable(() -> safe(codeBlock)) // Execute blocking code and wrap List<T> in Try<List<T>>
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(tryList -> { // tryList is Try<List<T>>
                    if (tryList.isSuccess()) {
                        return Flux.fromIterable(tryList.get()).map(Try::success); // Emit each item as Try.success
                    } else {
                        return Flux.just(Try.failure(tryList.getCause())); // Emit one failure for the entire list
                    }
                });
    }
}
