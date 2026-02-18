package com.ratelimiter.service.strategy;

import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitEntry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Bucket Rate Limiting Algorithm.
 *
 * Concept:
 * - Each identifier has a "bucket" with a max capacity of tokens.
 * - Tokens refill at a constant rate (refillRate tokens/second).
 * - Each request consumes 1 token.
 * - If the bucket is empty → reject (HTTP 429).
 * - Allows brief bursts if tokens have accumulated.
 *
 * Thread Safety:
 * - ConcurrentHashMap for the store (safe concurrent reads/writes per key).
 * - synchronized block on the RateLimitEntry object for atomic refill +
 * consume.
 */
@Component("tokenBucketStrategy")
public class TokenBucketStrategy implements RateLimiterStrategy {

    // In-memory store: identifier → entry
    private final ConcurrentHashMap<String, RateLimitEntry> store = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String identifier, RateLimitConfig config) {
        RateLimitEntry entry = store.computeIfAbsent(
                identifier,
                id -> new RateLimitEntry(id, config.getMaxRequests()));

        synchronized (entry) {
            refillTokens(entry, config);

            if (entry.tokenCount.get() >= 1) {
                entry.tokenCount.decrementAndGet();
                return true; // ✅ Allowed
            }
            return false; // ❌ Rejected — bucket empty
        }
    }

    /**
     * Refill tokens based on elapsed time since last refill.
     * Must be called inside a synchronized block on the entry.
     */
    private void refillTokens(RateLimitEntry entry, RateLimitConfig config) {
        long now = System.currentTimeMillis();
        long lastRefill = entry.lastRefillTime.get();
        long elapsedMs = now - lastRefill;

        if (elapsedMs <= 0)
            return;

        // Calculate how many tokens to add
        long tokensToAdd = (elapsedMs * config.getRefillRate()) / 1000L;

        if (tokensToAdd > 0) {
            long newTokens = Math.min(
                    config.getMaxRequests(),
                    entry.tokenCount.get() + tokensToAdd);
            entry.tokenCount.set(newTokens);
            entry.lastRefillTime.set(now);
        }
    }

    @Override
    public RateLimitEntry getEntry(String identifier) {
        return store.get(identifier);
    }

    @Override
    public void reset(String identifier, RateLimitConfig config) {
        RateLimitEntry entry = store.get(identifier);
        if (entry != null) {
            synchronized (entry) {
                entry.tokenCount.set(config.getMaxRequests());
                entry.lastRefillTime.set(System.currentTimeMillis());
            }
        } else {
            // Create fresh entry at full capacity
            store.put(identifier, new RateLimitEntry(identifier, config.getMaxRequests()));
        }
    }

    @Override
    public long getRemainingRequests(String identifier, RateLimitConfig config) {
        RateLimitEntry entry = store.get(identifier);
        if (entry == null)
            return config.getMaxRequests();
        synchronized (entry) {
            refillTokens(entry, config);
            return Math.max(0, entry.tokenCount.get());
        }
    }

    @Override
    public long getResetTimeEpochSeconds(String identifier, RateLimitConfig config) {
        RateLimitEntry entry = store.get(identifier);
        if (entry == null)
            return System.currentTimeMillis() / 1000 + config.getWindowSeconds();

        long lastRefill = entry.lastRefillTime.get();
        // Next full refill time = lastRefill + time to refill all tokens
        long tokensNeeded = config.getMaxRequests() - entry.tokenCount.get();
        long msToFull = (tokensNeeded * 1000L) / Math.max(1, config.getRefillRate());
        return (lastRefill + msToFull) / 1000;
    }

    @Override
    public String getAlgorithmName() {
        return "TOKEN_BUCKET";
    }
}
