package com.ratelimiter.repository;

import com.ratelimiter.model.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for RateLimitConfig.
 * Provides CRUD + custom lookup by identifier.
 */
@Repository
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {

    /**
     * Find config for a specific identifier (e.g., "user123", "192.168.1.1",
     * "key-abc").
     */
    Optional<RateLimitConfig> findByIdentifier(String identifier);

    /**
     * Find config by identifier and its type.
     */
    Optional<RateLimitConfig> findByIdentifierAndIdentifierType(
            String identifier,
            RateLimitConfig.IdentifierType identifierType);

    /**
     * Check if a config exists for this identifier.
     */
    boolean existsByIdentifier(String identifier);
}
