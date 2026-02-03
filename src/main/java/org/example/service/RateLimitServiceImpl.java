package org.example.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of rate limiting service using Bucket4j token bucket algorithm.
 * Thread-safe in-memory implementation (use Redis for distributed systems).
 */
@Service
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {

    // In-memory cache of buckets (use Redis for distributed systems)
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    // General API rate limiting
    @Value("${app.rate-limit.general.capacity:100}")
    private long generalCapacity;

    @Value("${app.rate-limit.general.refill-tokens:100}")
    private long generalRefillTokens;

    @Value("${app.rate-limit.general.refill-duration-seconds:60}")
    private long generalRefillDuration;

    // Authentication rate limiting
    @Value("${app.rate-limit.auth.capacity:5}")
    private long authCapacity;

    @Value("${app.rate-limit.auth.refill-tokens:5}")
    private long authRefillTokens;

    @Value("${app.rate-limit.auth.refill-duration-seconds:60}")
    private long authRefillDuration;

    // API rate limiting (by user)
    @Value("${app.rate-limit.api.capacity:1000}")
    private long apiCapacity;

    @Value("${app.rate-limit.api.refill-tokens:1000}")
    private long apiRefillTokens;

    @Value("${app.rate-limit.api.refill-duration-seconds:60}")
    private long apiRefillDuration;

    // MFA rate limiting
    @Value("${app.rate-limit.mfa.capacity:5}")
    private long mfaCapacity;

    @Value("${app.rate-limit.mfa.refill-tokens:5}")
    private long mfaRefillTokens;

    @Value("${app.rate-limit.mfa.refill-duration-seconds:300}")
    private long mfaRefillDuration;

    @Override
    public Bucket resolveBucket(String key, BucketType bucketType) {
        String cacheKey = bucketType + ":" + key;
        return bucketCache.computeIfAbsent(cacheKey, k -> createBucket(bucketType));
    }

    @Override
    public boolean tryConsume(String key, BucketType bucketType) {
        if (!rateLimitEnabled) {
            return true;
        }

        Bucket bucket = resolveBucket(key, bucketType);
        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            log.warn("Rate limit exceeded for key: {} with bucket type: {}", key, bucketType);
        }

        return consumed;
    }

    @Override
    public long getRemainingTokens(String key, BucketType bucketType) {
        if (!rateLimitEnabled) {
            return Long.MAX_VALUE;
        }

        Bucket bucket = resolveBucket(key, bucketType);
        return bucket.getAvailableTokens();
    }

    @Override
    public void reset(String key, BucketType bucketType) {
        String cacheKey = bucketType + ":" + key;
        bucketCache.remove(cacheKey);
        log.info("Rate limit reset for key: {} with bucket type: {}", key, bucketType);
    }

    /**
     * Create a bucket with appropriate configuration for the bucket type.
     */
    private Bucket createBucket(BucketType bucketType) {
        Bandwidth bandwidth;

        switch (bucketType) {
            case GENERAL:
                bandwidth = Bandwidth.classic(
                        generalCapacity,
                        Refill.intervally(generalRefillTokens, Duration.ofSeconds(generalRefillDuration))
                );
                break;

            case AUTH:
                bandwidth = Bandwidth.classic(
                        authCapacity,
                        Refill.intervally(authRefillTokens, Duration.ofSeconds(authRefillDuration))
                );
                break;

            case API:
                bandwidth = Bandwidth.classic(
                        apiCapacity,
                        Refill.intervally(apiRefillTokens, Duration.ofSeconds(apiRefillDuration))
                );
                break;

            case MFA:
                bandwidth = Bandwidth.classic(
                        mfaCapacity,
                        Refill.intervally(mfaRefillTokens, Duration.ofSeconds(mfaRefillDuration))
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown bucket type: " + bucketType);
        }

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
}
