package com.ratelimiter.service.strategy;

import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitEntry;

/**
 * Strategy interface for rate limiting algorithms.
 * Implementations: TokenBucketStrategy, SlidingWindowStrategy.
 *
 * Using the Strategy Pattern allows swapping algorithms at runtime
 * via configuration without changing the calling code.
 */
public interface RateLimiterStrategy {

    /**
     * Check if the request from this identifier is allowed.
     * If allowed, the counter/token is consumed atomically.
     *
     * @param identifier userId, IP address, or API key
     * @param config     rate limit configuration for this identifier
     * @return true if request is allowed, false if rate limit exceeded
     */
    boolean isAllowed(String identifier, RateLimitConfig config);

    /**
     * Get the current in-memory state for an identifier.
     * Returns null if no entry exists yet.
     */
    RateLimitEntry getEntry(String identifier);

    /**
     * Reset the rate limit counter for an identifier.
     * Used by the admin reset endpoint.
     */
    void reset(String identifier, RateLimitConfig config);

    /**
     * Get the number of remaining allowed requests for an identifier.
     */
    long getRemainingRequests(String identifier, RateLimitConfig config);

    /**
     * Get the epoch second at which the current window/bucket resets.
     */
    long getResetTimeEpochSeconds(String identifier, RateLimitConfig config);

    /**
     * Algorithm name for display/logging purposes.
     */
    String getAlgorithmName();
}
