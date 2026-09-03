package com.finops.prreviewagent.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reviewExecutor")
    public Executor reviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);        // baseline background threads
        executor.setMaxPoolSize(5);         // scale up to 5 under load
        executor.setQueueCapacity(50);      // queue pending reviews
        executor.setThreadNamePrefix("review-");
        executor.initialize();
        return executor;
    }
}
