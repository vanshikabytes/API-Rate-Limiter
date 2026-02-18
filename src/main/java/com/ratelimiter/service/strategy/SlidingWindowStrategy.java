package com.ratelimiter.service.strategy;

import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitEntry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Counter Rate Limiting Algorithm.
 *
 * Concept:
 * - Track timestamps of all requests within a rolling time window.
 * - Window slides with time (not fixed boundaries like "top of the minute").
 * - Count requests in [now - windowSize, now].
 * - If count >= limit → reject (HTTP 429).
 * - More accurate than fixed window; prevents boundary bursts.
 *
 * Thread Safety:
 * - ConcurrentHashMap for the store.
 * - synchronized block on the entry for atomic prune + count + add.
 */
@Component("slidingWindowStrategy")
public class SlidingWindowStrategy implements RateLimiterStrategy {

    // In-memory store: identifier → entry (with timestamp deque)
    private final ConcurrentHashMap<String, RateLimitEntry> store = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String identifier, RateLimitConfig config) {
        RateLimitEntry entry = store.computeIfAbsent(
                identifier,
                id -> new RateLimitEntry(id, config.getMaxRequests()));

        synchronized (entry) {
            long now = System.currentTimeMillis();
            long windowStartMs = now - (config.getWindowSeconds() * 1000L);

            // Remove timestamps outside the current window
            pruneOldTimestamps(entry, windowStartMs);

            if (entry.requestTimestamps.size() < config.getMaxRequests()) {
                entry.requestTimestamps.addLast(now);
                return true; // ✅ Allowed
            }
            return false; // ❌ Rejected — window full
        }
    }

    /**
     * Remove all timestamps older than windowStart.
     * Must be called inside a synchronized block on the entry.
     */
    private void pruneOldTimestamps(RateLimitEntry entry, long windowStartMs) {
        while (!entry.requestTimestamps.isEmpty()
                && entry.requestTimestamps.peekFirst() < windowStartMs) {
            entry.requestTimestamps.pollFirst();
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
                entry.requestTimestamps.clear();
            }
        }
        // If no entry exists, nothing to reset
    }

    @Override
    public long getRemainingRequests(String identifier, RateLimitConfig config) {
        RateLimitEntry entry = store.get(identifier);
        if (entry == null)
            return config.getMaxRequests();

        synchronized (entry) {
            long windowStartMs = System.currentTimeMillis() - (config.getWindowSeconds() * 1000L);
            pruneOldTimestamps(entry, windowStartMs);
            return Math.max(0, config.getMaxRequests() - entry.requestTimestamps.size());
        }
    }

    @Override
    public long getResetTimeEpochSeconds(String identifier, RateLimitConfig config) {
        RateLimitEntry entry = store.get(identifier);
        if (entry == null || entry.requestTimestamps.isEmpty()) {
            return System.currentTimeMillis() / 1000 + config.getWindowSeconds();
        }
        synchronized (entry) {
            // The oldest timestamp in the window — when it expires, a slot opens
            Long oldest = entry.requestTimestamps.peekFirst();
            if (oldest == null)
                return System.currentTimeMillis() / 1000 + config.getWindowSeconds();
            return (oldest + config.getWindowSeconds() * 1000L) / 1000;
        }
    }

    @Override
    public String getAlgorithmName() {
        return "SLIDING_WINDOW";
    }
}
