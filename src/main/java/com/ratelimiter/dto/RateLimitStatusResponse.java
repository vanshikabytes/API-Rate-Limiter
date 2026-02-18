package com.ratelimiter.dto;

/**
 * Response body for admin status check endpoint.
 */
public class RateLimitStatusResponse {

    private String identifier;
    private long tokensRemaining;
    private long totalLimit;
    private long windowSeconds;
    private long resetAtEpochSeconds;
    private String algorithm;

    private RateLimitStatusResponse() {
    }

    private RateLimitStatusResponse(Builder b) {
        this.identifier = b.identifier;
        this.tokensRemaining = b.tokensRemaining;
        this.totalLimit = b.totalLimit;
        this.windowSeconds = b.windowSeconds;
        this.resetAtEpochSeconds = b.resetAtEpochSeconds;
        this.algorithm = b.algorithm;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String identifier;
        private long tokensRemaining;
        private long totalLimit;
        private long windowSeconds;
        private long resetAtEpochSeconds;
        private String algorithm;

        public Builder identifier(String v) {
            this.identifier = v;
            return this;
        }

        public Builder tokensRemaining(long v) {
            this.tokensRemaining = v;
            return this;
        }

        public Builder totalLimit(long v) {
            this.totalLimit = v;
            return this;
        }

        public Builder windowSeconds(long v) {
            this.windowSeconds = v;
            return this;
        }

        public Builder resetAtEpochSeconds(long v) {
            this.resetAtEpochSeconds = v;
            return this;
        }

        public Builder algorithm(String v) {
            this.algorithm = v;
            return this;
        }

        public RateLimitStatusResponse build() {
            return new RateLimitStatusResponse(this);
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    public long getTokensRemaining() {
        return tokensRemaining;
    }

    public long getTotalLimit() {
        return totalLimit;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public long getResetAtEpochSeconds() {
        return resetAtEpochSeconds;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
