package com.catgineer.analytics_assistant;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import com.catgineer.analytics_assistant.control.configuration.DotenvInitializer;

import io.netty.util.concurrent.ThreadPerTaskExecutor;

import java.util.concurrent.ThreadFactory;

@SpringBootApplication
@ComponentScan(basePackages = "com.catgineer.analytics_assistant")
public class AnalyticsAssistantApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AnalyticsAssistantApplication.class)
        .initializers(new DotenvInitializer())
        .run(args);
    }

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor applicationTaskExecutor() {
        ThreadFactory factory = Thread.ofVirtual().name("analytics-vt-", 0).factory();
        return new TaskExecutorAdapter(new ThreadPerTaskExecutor(factory));
    }
}
