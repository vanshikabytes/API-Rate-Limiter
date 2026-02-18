package com.ratelimiter.service.strategy;

import com.ratelimiter.model.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TokenBucketStrategy.
 * Tests: allow, reject, refill, concurrency, reset.
 */
class TokenBucketStrategyTest {

    private TokenBucketStrategy strategy;
    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        strategy = new TokenBucketStrategy();
        config = RateLimitConfig.builder()
                .identifier("test-user")
                .identifierType(RateLimitConfig.IdentifierType.USER_ID)
                .maxRequests(5)
                .windowSeconds(60)
                .refillRate(1) // 1 token/sec — slow refill for predictable tests
                .build();
    }

    @Test
    @DisplayName("1. Should allow requests when tokens are available")
    void testAllowsRequestWhenTokensAvailable() {
        boolean result = strategy.isAllowed("user1", config);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("2. Should reject request when bucket is empty (101st request on capacity 5)")
    void testRejectsRequestWhenBucketEmpty() {
        String identifier = "user-reject";

        // Exhaust all 5 tokens
        for (int i = 0; i < 5; i++) {
            assertThat(strategy.isAllowed(identifier, config)).isTrue();
        }

        // 6th request should be rejected
        assertThat(strategy.isAllowed(identifier, config)).isFalse();
    }

    @Test
    @DisplayName("3. Should refill tokens after time passes")
    void testTokensRefillAfterTime() throws InterruptedException {
        String identifier = "user-refill";

        // Exhaust all tokens
        for (int i = 0; i < 5; i++) {
            strategy.isAllowed(identifier, config);
        }
        assertThat(strategy.isAllowed(identifier, config)).isFalse();

        // Wait 2 seconds — refillRate=1 token/sec, so 2 tokens should be added
        Thread.sleep(2100);

        // Should now allow requests again
        assertThat(strategy.isAllowed(identifier, config)).isTrue();
        assertThat(strategy.isAllowed(identifier, config)).isTrue();
    }

    @Test
    @DisplayName("4. Should handle concurrent requests thread-safely")
    void testConcurrentRequestsThreadSafe() throws InterruptedException {
        String identifier = "user-concurrent";
        int threadCount = 20;
        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    if (strategy.isAllowed(identifier, config)) {
                        allowed.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly maxRequests (5) should be allowed, rest rejected
        assertThat(allowed.get()).isEqualTo(config.getMaxRequests());
        assertThat(rejected.get()).isEqualTo(threadCount - config.getMaxRequests());
    }

    @Test
    @DisplayName("5. Should reset counter to full capacity")
    void testResetClearsTokenCount() {
        String identifier = "user-reset";

        // Exhaust tokens
        for (int i = 0; i < 5; i++) {
            strategy.isAllowed(identifier, config);
        }
        assertThat(strategy.isAllowed(identifier, config)).isFalse();

        // Reset
        strategy.reset(identifier, config);

        // Should be allowed again
        assertThat(strategy.isAllowed(identifier, config)).isTrue();
    }

    @Test
    @DisplayName("6. Should track different identifiers separately")
    void testDifferentIdentifiersTrackedSeparately() {
        // Exhaust userA
        for (int i = 0; i < 5; i++) {
            strategy.isAllowed("userA", config);
        }
        assertThat(strategy.isAllowed("userA", config)).isFalse();

        // userB should still be allowed
        assertThat(strategy.isAllowed("userB", config)).isTrue();
    }

    @Test
    @DisplayName("7. getRemainingRequests should return correct count")
    void testGetRemainingRequests() {
        String identifier = "user-remaining";
        assertThat(strategy.getRemainingRequests(identifier, config)).isEqualTo(5);

        strategy.isAllowed(identifier, config);
        strategy.isAllowed(identifier, config);

        assertThat(strategy.getRemainingRequests(identifier, config)).isEqualTo(3);
    }
}
