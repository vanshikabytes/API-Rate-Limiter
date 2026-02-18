package com.ratelimiter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.dto.RateLimitResponse;
import com.ratelimiter.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP filter that intercepts every incoming request and enforces rate limits.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public static final String HEADER_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY = "Retry-After";

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String API_KEY_HEADER = "X-API-Key";

    public RateLimitFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String identifier = extractIdentifier(request);
        log.debug("Rate limit check for identifier: {} on path: {}", identifier, path);

        addRateLimitHeaders(response, identifier);

        boolean allowed = rateLimiterService.checkAndConsume(identifier);

        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            sendRateLimitExceededResponse(response, identifier);
        }
    }

    private String extractIdentifier(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.isBlank())
            return userId.trim();

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank())
            return apiKey.trim();

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank())
            return forwardedFor.split(",")[0].trim();

        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(HttpServletResponse response, String identifier) {
        long limit = rateLimiterService.getLimit(identifier);
        long remaining = rateLimiterService.getRemainingRequests(identifier);
        long reset = rateLimiterService.getResetTimeEpochSeconds(identifier);

        response.setHeader(HEADER_LIMIT, String.valueOf(limit));
        response.setHeader(HEADER_REMAINING, String.valueOf(remaining));
        response.setHeader(HEADER_RESET, String.valueOf(reset));
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response,
            String identifier) throws IOException {
        long resetAt = rateLimiterService.getResetTimeEpochSeconds(identifier);
        long retryAfter = Math.max(1, resetAt - System.currentTimeMillis() / 1000);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HEADER_RETRY, String.valueOf(retryAfter));
        response.setHeader(HEADER_REMAINING, "0");

        RateLimitResponse body = RateLimitResponse.builder()
                .status(429)
                .message("Rate limit exceeded. Too many requests.")
                .identifier(identifier)
                .retryAfterSeconds(retryAfter)
                .limitRemaining(0)
                .limitTotal(rateLimiterService.getLimit(identifier))
                .resetAtEpochSeconds(resetAt)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private boolean isExcluded(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs");
    }
}
