package com.aicode.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring Cache with Caffeine backend.
 * Cache spec is configured in application.yml (spring.cache.caffeine.spec).
 *
 * Cache names used:
 *   "reviews" — keyed by djb2 hash of sanitized code
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
