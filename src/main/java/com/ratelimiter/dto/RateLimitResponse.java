package com.ratelimiter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response body returned on HTTP 429 Too Many Requests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RateLimitResponse {

    private int status;
    private String message;
    private String identifier;
    private long retryAfterSeconds;
    private long limitRemaining;
    private long limitTotal;
    private long resetAtEpochSeconds;

    private RateLimitResponse() {
    }

    private RateLimitResponse(Builder b) {
        this.status = b.status;
        this.message = b.message;
        this.identifier = b.identifier;
        this.retryAfterSeconds = b.retryAfterSeconds;
        this.limitRemaining = b.limitRemaining;
        this.limitTotal = b.limitTotal;
        this.resetAtEpochSeconds = b.resetAtEpochSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String message;
        private String identifier;
        private long retryAfterSeconds;
        private long limitRemaining;
        private long limitTotal;
        private long resetAtEpochSeconds;

        public Builder status(int v) {
            this.status = v;
            return this;
        }

        public Builder message(String v) {
            this.message = v;
            return this;
        }

        public Builder identifier(String v) {
            this.identifier = v;
            return this;
        }

        public Builder retryAfterSeconds(long v) {
            this.retryAfterSeconds = v;
            return this;
        }

        public Builder limitRemaining(long v) {
            this.limitRemaining = v;
            return this;
        }

        public Builder limitTotal(long v) {
            this.limitTotal = v;
            return this;
        }

        public Builder resetAtEpochSeconds(long v) {
            this.resetAtEpochSeconds = v;
            return this;
        }

        public RateLimitResponse build() {
            return new RateLimitResponse(this);
        }
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getIdentifier() {
        return identifier;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public long getLimitRemaining() {
        return limitRemaining;
    }

    public long getLimitTotal() {
        return limitTotal;
    }

    public long getResetAtEpochSeconds() {
        return resetAtEpochSeconds;
    }
}
