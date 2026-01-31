package com.catgineer.analytics_assistant.domain;

import io.vavr.control.Try;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SafeRunner {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Executes a block of code that can throw an exception and wraps the outcome in a {@link Try}.
     * This is a synchronous operation.
     *
     * @param codeBlock The {@link Callable} codeBlock of code to execute.
     * @param <T>   The return type of the codeBlock.
     * @return A {@link Try} containing either the successful result or the exception.
     */
    public static <T> Try<T> safe(Callable<T> codeBlock) {
        return Try.of(codeBlock::call);
    }

    /**
     * Executes a codeBlock of code asynchronously that can throw an exception and wraps the outcome
     * in a {@link CompletableFuture} of a {@link Try}.
     *
     * @param codeBlock The {@link Callable} codeBlock of code to execute.
     * @param <T>   The return type of the codeBlock.
     * @return A {@link CompletableFuture} that will complete with a {@link Try}
     *         containing either the successful result or the exception.
     */
    public static <T> CompletableFuture<Try<T>> futureSafe(Callable<T> codeBlock) {
        return CompletableFuture.supplyAsync(() -> safe(codeBlock), executor);
    }
}
