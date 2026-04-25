package com.example.nidar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "sosTaskExecutor")
    public Executor sosTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);     // always-on threads
        executor.setMaxPoolSize(50);      // burst capacity
        executor.setQueueCapacity(500);   // queue before rejecting
        executor.setThreadNamePrefix("sos-alert-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
            // If queue is full, run on the calling thread rather than drop the alert
        );
        executor.initialize();
        return executor;
    }
}
