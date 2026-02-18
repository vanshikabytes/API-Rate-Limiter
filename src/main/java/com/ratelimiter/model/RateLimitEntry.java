package com.ratelimiter.model;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory state for a single rate-limited identifier.
 * Stored in a ConcurrentHashMap — NOT persisted to DB.
 *
 * Used by both Token Bucket and Sliding Window strategies.
 */
public class RateLimitEntry {

    // ── Token Bucket fields ──────────────────────────────────────────────────
    /** Current number of available tokens */
    public final AtomicLong tokenCount;

    /** Epoch milliseconds of the last token refill */
    public final AtomicLong lastRefillTime;

    // ── Sliding Window fields ────────────────────────────────────────────────
    /** Timestamps (epoch ms) of recent requests within the current window */
    public final ConcurrentLinkedDeque<Long> requestTimestamps;

    // ── Shared ──────────────────────────────────────────────────────────────
    /** The identifier this entry belongs to (userId / IP / apiKey) */
    public final String identifier;

    public RateLimitEntry(String identifier, long initialTokens) {
        this.identifier = identifier;
        this.tokenCount = new AtomicLong(initialTokens);
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        this.requestTimestamps = new ConcurrentLinkedDeque<>();
    }
}
