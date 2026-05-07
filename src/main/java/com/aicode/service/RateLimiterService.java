package com.aicode.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-identity rate limiter using Bucket4j.
 *
 * Each User_Identity gets its own token bucket. Buckets are created lazily
 * on first request and stored in a ConcurrentHashMap for lock-free reads.
 *
 * The ConcurrentHashMap structure is designed to support future replacement
 * with a distributed store (Redis, Hazelcast) without changing the rate-limiting
 * logic — only the bucket storage layer needs to change.
 *
 * Identity resolution order (handled by UserIdentityResolver):
 *   1. GitHub user login
 *   2. X-User-Id header (VS Code extension UUID)
 *   3. SHA-256(clientIP) — raw IP never stored
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    @Value("${rate-limit.per-user-rpm:10}")
    private int perUserRpm;

    // Identity → Bucket — lazily populated, never evicted (buckets are lightweight)
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Attempts to consume one token from the identity's bucket.
     *
     * @param identity the resolved User_Identity string
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    public boolean tryConsume(String identity) {
        Bucket bucket = buckets.computeIfAbsent(identity, this::createBucket);
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            log.info("Rate limit exceeded for identity={}", identity);
        }
        return allowed;
    }

    /**
     * Returns the number of nanoseconds until the next token is available
     * for the given identity. Used to compute the Retry-After header value.
     *
     * @param identity the resolved User_Identity string
     * @return nanoseconds to wait, or 0 if a token is immediately available
     */
    public long nanosToWaitForRefill(String identity) {
        Bucket bucket = buckets.get(identity);
        if (bucket == null) return 0;
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
    }

    /**
     * Returns the Retry-After value in whole seconds (rounded up).
     */
    public long secondsToWaitForRefill(String identity) {
        long nanos = nanosToWaitForRefill(identity);
        return (nanos + 999_999_999L) / 1_000_000_000L; // ceiling division
    }

    private Bucket createBucket(String identity) {
        Bandwidth limit = Bandwidth.classic(
                perUserRpm,
                Refill.intervally(perUserRpm, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
