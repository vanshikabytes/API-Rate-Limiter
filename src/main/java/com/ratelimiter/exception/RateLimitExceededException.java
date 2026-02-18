package com.ratelimiter.exception;

/**
 * Thrown when a rate limit is exceeded.
 * Caught by GlobalExceptionHandler to return HTTP 429.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String identifier;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String identifier, long retryAfterSeconds) {
        super("Rate limit exceeded for: " + identifier);
        this.identifier = identifier;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getIdentifier() {
        return identifier;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
