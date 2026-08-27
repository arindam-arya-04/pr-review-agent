package com.finops.prreviewagent.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's caching abstraction with a simple in-memory cache.
 *
 * @EnableCaching turns on @Cacheable/@CacheEvict support. We provide an explicit
 * ConcurrentMapCacheManager (a thread-safe in-memory map cache) — perfect for
 * local dev. For production we can swap this bean for a Redis-backed CacheManager
 * without changing any @Cacheable annotations elsewhere (swappable backend).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Named caches this app uses. Add names here as we cache more things.
        return new ConcurrentMapCacheManager("repoIndex");
    }
}
