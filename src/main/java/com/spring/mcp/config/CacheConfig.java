package com.spring.mcp.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache configuration for documentation search results
 * Uses in-memory caching with Spring's simple cache manager
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager with predefined caches
     * - documentationSearch: Basic search results (TTL managed by eviction)
     * - documentationSearchPaged: Paginated search results
     * - documentationByVersion: Documentation by version ID
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("documentationSearch"),
            new ConcurrentMapCache("documentationSearchPaged"),
            new ConcurrentMapCache("documentationByVersion")
        ));

        return cacheManager;
    }
}
