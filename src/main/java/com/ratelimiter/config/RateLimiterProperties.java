package com.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds rate-limiter.* properties from application.yml.
 */
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private String algorithm = "TOKEN_BUCKET";
    private int defaultLimit = 100;
    private int defaultWindowSeconds = 60;
    private int defaultRefillRate = 10;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getDefaultWindowSeconds() {
        return defaultWindowSeconds;
    }

    public void setDefaultWindowSeconds(int defaultWindowSeconds) {
        this.defaultWindowSeconds = defaultWindowSeconds;
    }

    public int getDefaultRefillRate() {
        return defaultRefillRate;
    }

    public void setDefaultRefillRate(int defaultRefillRate) {
        this.defaultRefillRate = defaultRefillRate;
    }
}
