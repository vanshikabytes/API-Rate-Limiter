package com.ratelimiter.service.strategy;

import com.ratelimiter.model.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SlidingWindowStrategy.
 * Tests: allow, reject, window slide, reset, separate identifiers.
 */
class SlidingWindowStrategyTest {

    private SlidingWindowStrategy strategy;
    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        strategy = new SlidingWindowStrategy();
        config = RateLimitConfig.builder()
                .identifier("test-user")
                .identifierType(RateLimitConfig.IdentifierType.USER_ID)
                .maxRequests(3)
                .windowSeconds(2) // 2-second window for fast tests
                .refillRate(0)
                .build();
    }

    @Test
    @DisplayName("1. Should allow requests within the limit")
    void testAllowsRequestWithinLimit() {
        assertThat(strategy.isAllowed("sw-user1", config)).isTrue();
        assertThat(strategy.isAllowed("sw-user1", config)).isTrue();
        assertThat(strategy.isAllowed("sw-user1", config)).isTrue();
    }

    @Test
    @DisplayName("2. Should reject request when limit exceeded")
    void testRejectsRequestOverLimit() {
        String id = "sw-user-reject";
        strategy.isAllowed(id, config);
        strategy.isAllowed(id, config);
        strategy.isAllowed(id, config);

        // 4th request should be rejected
        assertThat(strategy.isAllowed(id, config)).isFalse();
    }

    @Test
    @DisplayName("3. Should allow requests again after window slides")
    void testWindowSlides() throws InterruptedException {
        String id = "sw-user-slide";

        // Fill the window
        strategy.isAllowed(id, config);
        strategy.isAllowed(id, config);
        strategy.isAllowed(id, config);
        assertThat(strategy.isAllowed(id, config)).isFalse();

        // Wait for window to expire (2 seconds + buffer)
        Thread.sleep(2200);

        // Window has slid — old timestamps expired, new requests allowed
        assertThat(strategy.isAllowed(id, config)).isTrue();
    }

    @Test
    @DisplayName("4. Should track different identifiers separately")
    void testDifferentIdentifiersTrackedSeparately() {
        String idA = "sw-userA";
        String idB = "sw-userB";

        // Exhaust idA
        strategy.isAllowed(idA, config);
        strategy.isAllowed(idA, config);
        strategy.isAllowed(idA, config);
        assertThat(strategy.isAllowed(idA, config)).isFalse();

        // idB should still be allowed
        assertThat(strategy.isAllowed(idB, config)).isTrue();
    }

    @Test
    @DisplayName("5. Should reset window and allow requests after reset")
    void testResetClearsWindow() {
        String id = "sw-user-reset";

        // Fill the window
        strategy.isAllowed(id, config);
        strategy.isAllowed(id, config);
        strategy.isAllowed(id, config);
        assertThat(strategy.isAllowed(id, config)).isFalse();

        // Reset
        strategy.reset(id, config);

        // Should be allowed again
        assertThat(strategy.isAllowed(id, config)).isTrue();
    }

    @Test
    @DisplayName("6. getRemainingRequests should return correct count")
    void testGetRemainingRequests() {
        String id = "sw-user-remaining";
        assertThat(strategy.getRemainingRequests(id, config)).isEqualTo(3);

        strategy.isAllowed(id, config);
        assertThat(strategy.getRemainingRequests(id, config)).isEqualTo(2);

        strategy.isAllowed(id, config);
        assertThat(strategy.getRemainingRequests(id, config)).isEqualTo(1);
    }
}
