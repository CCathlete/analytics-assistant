package com.catgineer.analytics_assistant.control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import io.netty.util.concurrent.ThreadPerTaskExecutor;

import java.util.concurrent.ThreadFactory;

@SpringBootApplication
@ComponentScan(basePackages = "com.catgineer.analytics_assistant")
public class AnalyticsAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsAssistantApplication.class, args);
    }

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor applicationTaskExecutor() {
        ThreadFactory factory = Thread.ofVirtual().name("analytics-vt-", 0).factory();
        return new TaskExecutorAdapter(new ThreadPerTaskExecutor(factory));
    }
}
