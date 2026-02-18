package com.ratelimiter.service;

import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.dto.RateLimitStatusResponse;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.repository.RateLimitConfigRepository;
import com.ratelimiter.service.strategy.RateLimiterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Core service that orchestrates rate limiting.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RateLimiterStrategy activeStrategy;
    private final RateLimitConfigRepository configRepository;
    private final RateLimiterProperties properties;

    public RateLimiterService(
            @Qualifier("tokenBucketStrategy") RateLimiterStrategy tokenBucket,
            @Qualifier("slidingWindowStrategy") RateLimiterStrategy slidingWindow,
            RateLimitConfigRepository configRepository,
            RateLimiterProperties properties) {
        this.configRepository = configRepository;
        this.properties = properties;

        if ("SLIDING_WINDOW".equalsIgnoreCase(properties.getAlgorithm())) {
            this.activeStrategy = slidingWindow;
            log.info("Rate Limiter using: SLIDING_WINDOW algorithm");
        } else {
            this.activeStrategy = tokenBucket;
            log.info("Rate Limiter using: TOKEN_BUCKET algorithm");
        }
    }

    public boolean checkAndConsume(String identifier) {
        RateLimitConfig config = getConfigForIdentifier(identifier);
        boolean allowed = activeStrategy.isAllowed(identifier, config);
        if (!allowed) {
            log.warn("Rate limit exceeded for identifier: {}", identifier);
        }
        return allowed;
    }

    public void reset(String identifier) {
        RateLimitConfig config = getConfigForIdentifier(identifier);
        activeStrategy.reset(identifier, config);
        log.info("Rate limit reset for identifier: {}", identifier);
    }

    public RateLimitStatusResponse getStatus(String identifier) {
        RateLimitConfig config = getConfigForIdentifier(identifier);
        return RateLimitStatusResponse.builder()
                .identifier(identifier)
                .tokensRemaining(activeStrategy.getRemainingRequests(identifier, config))
                .totalLimit(config.getMaxRequests())
                .windowSeconds(config.getWindowSeconds())
                .resetAtEpochSeconds(activeStrategy.getResetTimeEpochSeconds(identifier, config))
                .algorithm(activeStrategy.getAlgorithmName())
                .build();
    }

    public long getRemainingRequests(String identifier) {
        RateLimitConfig config = getConfigForIdentifier(identifier);
        return activeStrategy.getRemainingRequests(identifier, config);
    }

    public long getResetTimeEpochSeconds(String identifier) {
        RateLimitConfig config = getConfigForIdentifier(identifier);
        return activeStrategy.getResetTimeEpochSeconds(identifier, config);
    }

    public long getLimit(String identifier) {
        return getConfigForIdentifier(identifier).getMaxRequests();
    }

    public RateLimitConfig getConfigForIdentifier(String identifier) {
        return configRepository.findByIdentifier(identifier)
                .orElseGet(() -> buildDefaultConfig(identifier));
    }

    private RateLimitConfig buildDefaultConfig(String identifier) {
        return RateLimitConfig.builder()
                .identifier(identifier)
                .identifierType(RateLimitConfig.IdentifierType.USER_ID)
                .maxRequests(properties.getDefaultLimit())
                .windowSeconds(properties.getDefaultWindowSeconds())
                .refillRate(properties.getDefaultRefillRate())
                .build();
    }
}
