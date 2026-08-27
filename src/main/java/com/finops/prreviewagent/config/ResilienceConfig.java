package com.finops.prreviewagent.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Defines reusable resilience policies for calls to external services
 * (OpenAI and GitHub), which can fail transiently.
 *
 * Retry: automatically re-attempts a failed call a few times with a growing
 *        delay between attempts (exponential backoff), so a brief blip doesn't
 *        fail the whole review.
 *
 * CircuitBreaker: if a service fails repeatedly, the breaker "opens" and calls
 *        fail fast for a while instead of piling on a service that's clearly
 *        down. It then tries again after a wait. This protects both us and them.
 */
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
