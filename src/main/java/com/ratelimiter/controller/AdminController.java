package com.ratelimiter.controller;

import com.ratelimiter.dto.RateLimitStatusResponse;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.repository.RateLimitConfigRepository;
import com.ratelimiter.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for managing rate limits.
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin API", description = "Admin endpoints for managing rate limit configurations and resetting counters")
public class AdminController {

    private final RateLimiterService rateLimiterService;
    private final RateLimitConfigRepository configRepository;

    public AdminController(RateLimiterService rateLimiterService,
            RateLimitConfigRepository configRepository) {
        this.rateLimiterService = rateLimiterService;
        this.configRepository = configRepository;
    }

    @PostMapping("/reset/{identifier}")
    @Operation(summary = "Reset rate limit for an identifier")
    public ResponseEntity<Map<String, String>> resetLimit(@PathVariable String identifier) {
        rateLimiterService.reset(identifier);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Rate limit reset for: " + identifier));
    }

    @GetMapping("/status/{identifier}")
    @Operation(summary = "Get rate limit status for any identifier")
    public ResponseEntity<RateLimitStatusResponse> getStatus(@PathVariable String identifier) {
        return ResponseEntity.ok(rateLimiterService.getStatus(identifier));
    }

    @GetMapping("/config")
    @Operation(summary = "List all rate limit configurations")
    public ResponseEntity<List<RateLimitConfig>> getAllConfigs() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    @GetMapping("/config/{id}")
    @Operation(summary = "Get config by ID")
    public ResponseEntity<RateLimitConfig> getConfigById(@PathVariable Long id) {
        return configRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/config")
    @Operation(summary = "Create a custom rate limit config")
    public ResponseEntity<RateLimitConfig> createConfig(@RequestBody RateLimitConfig config) {
        return ResponseEntity.ok(configRepository.save(config));
    }

    @PutMapping("/config/{id}")
    @Operation(summary = "Update an existing rate limit config")
    public ResponseEntity<RateLimitConfig> updateConfig(@PathVariable Long id,
            @RequestBody RateLimitConfig updated) {
        return configRepository.findById(id)
                .map(existing -> {
                    existing.setMaxRequests(updated.getMaxRequests());
                    existing.setWindowSeconds(updated.getWindowSeconds());
                    existing.setRefillRate(updated.getRefillRate());
                    return ResponseEntity.ok(configRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/config/{id}")
    @Operation(summary = "Delete a rate limit config")
    public ResponseEntity<Map<String, String>> deleteConfig(@PathVariable Long id) {
        if (!configRepository.existsById(id))
            return ResponseEntity.notFound().build();
        configRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted", "id", String.valueOf(id)));
    }
}
