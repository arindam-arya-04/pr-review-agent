package com.finops.prreviewagent.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@Configuration
public class ResilienceConfig {

    @Bean
    public Retry externalCallRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                                  // try up to 3 times
                .waitDuration(Duration.ofSeconds(2))             // base wait between tries
                .retryExceptions(Exception.class)                // retry on any exception
                .build();
        return Retry.of("externalCall", config);
    }

    @Bean
    public CircuitBreaker externalCallCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                        // open if >50% of calls fail
                .slidingWindowSize(10)                           // measured over last 10 calls
                .waitDurationInOpenState(Duration.ofSeconds(30)) // stay open 30s before retrying
                .build();
        return CircuitBreaker.of("externalCall", config);
    }
}
