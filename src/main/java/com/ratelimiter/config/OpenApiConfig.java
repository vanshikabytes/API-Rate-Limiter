package com.ratelimiter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 * Swagger UI available at: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rateLimiterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Rate Limiter Service")
                        .description("""
                                Production-grade API Rate Limiter Service.

                                **Algorithms:** Token Bucket & Sliding Window Counter

                                **Identifier Priority:** X-User-Id header → X-API-Key header → Client IP

                                **Rate Limit Headers on every response:**
                                - `X-RateLimit-Limit` — Max requests allowed
                                - `X-RateLimit-Remaining` — Requests left in window
                                - `X-RateLimit-Reset` — Unix timestamp when limit resets
                                - `Retry-After` — Seconds to wait (only on 429)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Internship Technical Assessment")
                                .email("dev@ratelimiter.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
