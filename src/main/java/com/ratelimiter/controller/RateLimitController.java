package com.ratelimiter.controller;

import com.ratelimiter.dto.RateLimitStatusResponse;
import com.ratelimiter.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public API endpoints — all are rate-limited automatically by RateLimitFilter.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Rate Limit API", description = "Public endpoints — all are rate-limited automatically")
public class RateLimitController {

    private final RateLimiterService rateLimiterService;

    public RateLimitController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/status")
    @Operation(summary = "Check rate limit status (does not consume a token)")
    public ResponseEntity<RateLimitStatusResponse> checkStatus(
            @Parameter(description = "User identifier (optional — falls back to IP)") @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request) {

        String identifier = resolveIdentifier(userId, request);
        return ResponseEntity.ok(rateLimiterService.getStatus(identifier));
    }

    @PostMapping("/request")
    @Operation(summary = "Make a rate-limited request")
    public ResponseEntity<Map<String, Object>> makeRequest(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request) {

        String identifier = resolveIdentifier(userId, request);
        long remaining = rateLimiterService.getRemainingRequests(identifier);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Request processed successfully",
                "identifier", identifier,
                "tokensRemaining", remaining));
    }

    @GetMapping("/ping")
    @Operation(summary = "Health check — not rate limited")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "API Rate Limiter"));
    }

    private String resolveIdentifier(String userId, HttpServletRequest request) {
        if (userId != null && !userId.isBlank())
            return userId.trim();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank())
            return forwardedFor.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
