package com.ratelimiter.model;

import jakarta.persistence.*;

/**
 * Persisted rate limit configuration per identifier.
 * Stored in PostgreSQL. Allows per-user/IP/API-key custom limits.
 *
 * Note: Explicit getters/setters used for Java 25 compatibility.
 */
@Entity
@Table(name = "rate_limit_configs", uniqueConstraints = @UniqueConstraint(columnNames = { "identifier",
        "identifier_type" }))
public class RateLimitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The actual value: a userId, IP address, or API key string */
    @Column(nullable = false)
    private String identifier;

    /** What kind of identifier this is */
    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false)
    private IdentifierType identifierType;

    /** Maximum requests allowed per window */
    @Column(nullable = false)
    private int maxRequests;

    /** Time window in seconds (e.g., 60 = per minute) */
    @Column(nullable = false)
    private int windowSeconds;

    /** Token refill rate per second — only used by Token Bucket algorithm */
    @Column(nullable = false)
    private int refillRate;

    public enum IdentifierType {
        USER_ID, IP_ADDRESS, API_KEY
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public RateLimitConfig() {
    }

    private RateLimitConfig(Builder builder) {
        this.id = builder.id;
        this.identifier = builder.identifier;
        this.identifierType = builder.identifierType;
        this.maxRequests = builder.maxRequests;
        this.windowSeconds = builder.windowSeconds;
        this.refillRate = builder.refillRate;
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String identifier;
        private IdentifierType identifierType;
        private int maxRequests;
        private int windowSeconds;
        private int refillRate;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder identifierType(IdentifierType identifierType) {
            this.identifierType = identifierType;
            return this;
        }

        public Builder maxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
            return this;
        }

        public Builder windowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
            return this;
        }

        public Builder refillRate(int refillRate) {
            this.refillRate = refillRate;
            return this;
        }

        public RateLimitConfig build() {
            return new RateLimitConfig(this);
        }
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(int refillRate) {
        this.refillRate = refillRate;
    }

    @Override
    public String toString() {
        return "RateLimitConfig{id=" + id + ", identifier='" + identifier + "', " +
                "identifierType=" + identifierType + ", maxRequests=" + maxRequests +
                ", windowSeconds=" + windowSeconds + ", refillRate=" + refillRate + "}";
    }
}
