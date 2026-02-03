package org.example.service;

import io.github.bucket4j.Bucket;

/**
 * Service interface for rate limiting operations using Bucket4j.
 */
public interface RateLimitService {

    /**
     * Get rate limit bucket for API endpoint.
     *
     * @param key unique key for rate limiting (e.g., IP address or user ID)
     * @param bucketType type of bucket (GENERAL, AUTH, API)
     * @return bucket instance
     */
    Bucket resolveBucket(String key, BucketType bucketType);

    /**
     * Check if request should be rate limited.
     *
     * @param key unique key for rate limiting
     * @param bucketType type of bucket
     * @return true if request is allowed, false if rate limited
     */
    boolean tryConsume(String key, BucketType bucketType);

    /**
     * Get remaining tokens for a key.
     *
     * @param key unique key
     * @param bucketType type of bucket
     * @return number of remaining tokens
     */
    long getRemainingTokens(String key, BucketType bucketType);

    /**
     * Reset rate limit for a key (e.g., after successful authentication).
     *
     * @param key unique key
     * @param bucketType type of bucket
     */
    void reset(String key, BucketType bucketType);

    /**
     * Bucket types for different rate limiting strategies.
     */
    enum BucketType {
        /**
         * General API rate limiting (100 requests per minute per IP)
         */
        GENERAL,

        /**
         * Authentication endpoints (5 attempts per minute per IP)
         */
        AUTH,

        /**
         * API endpoints by user (1000 requests per minute per user)
         */
        API,

        /**
         * MFA verification (5 attempts per 5 minutes per user)
         */
        MFA
    }
}
