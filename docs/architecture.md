# Architecture — API Rate Limiter Service

## System Architecture

```
                    ┌─────────────────────────────────────────────┐
                    │              CLIENT REQUESTS                 │
                    │  (curl / Postman / Frontend / Other APIs)    │
                    └──────────────────┬──────────────────────────┘
                                       │ HTTP Request
                                       ▼
                    ┌─────────────────────────────────────────────┐
                    │           SPRING BOOT APP (Port 8080)        │
                    │                                              │
                    │  ┌────────────────────────────────────────┐  │
                    │  │         RateLimitFilter                │  │
                    │  │  (OncePerRequestFilter — runs first)   │  │
                    │  │                                        │  │
                    │  │  1. Extract identifier                 │  │
                    │  │     X-User-Id → X-API-Key → Client IP  │  │
                    │  │  2. Call RateLimiterService            │  │
                    │  │  3. Add X-RateLimit-* headers          │  │
                    │  │  4. Allow (200) or Reject (429)        │  │
                    │  └──────────────┬─────────────────────────┘  │
                    │                 │                             │
                    │  ┌──────────────▼─────────────────────────┐  │
                    │  │         RateLimiterService             │  │
                    │  │                                        │  │
                    │  │  - Loads config from DB (or defaults)  │  │
                    │  │  - Delegates to active strategy        │  │
                    │  └──────────────┬─────────────────────────┘  │
                    │                 │                             │
                    │  ┌──────────────▼─────────────────────────┐  │
                    │  │       RateLimiterStrategy (Interface)  │  │
                    │  │                                        │  │
                    │  │  ┌─────────────────────────────────┐  │  │
                    │  │  │   TokenBucketStrategy           │  │  │
                    │  │  │   - ConcurrentHashMap store     │  │  │
                    │  │  │   - synchronized on entry       │  │  │
                    │  │  │   - Atomic refill + consume     │  │  │
                    │  │  └─────────────────────────────────┘  │  │
                    │  │  ┌─────────────────────────────────┐  │  │
                    │  │  │   SlidingWindowStrategy         │  │  │
                    │  │  │   - ConcurrentHashMap store     │  │  │
                    │  │  │   - ConcurrentLinkedDeque       │  │  │
                    │  │  │   - Prune + count timestamps    │  │  │
                    │  │  └─────────────────────────────────┘  │  │
                    │  └────────────────────────────────────────┘  │
                    │                                              │
                    │  ┌────────────────────────────────────────┐  │
                    │  │         REST Controllers               │  │
                    │  │  /api/v1/* — Public (rate limited)     │  │
                    │  │  /admin/*  — Admin (reset, config)     │  │
                    │  └────────────────────────────────────────┘  │
                    └──────────────┬──────────────────────────────┘
                                   │
               ┌───────────────────┼───────────────────┐
               │                   │                   │
               ▼                   ▼                   ▼
    ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
    │   IN-MEMORY      │ │   POSTGRESQL     │ │     REDIS        │
    │   HashMap        │ │   (Port 5432)    │ │   (Port 6379)    │
    │                  │ │                  │ │                  │
    │  Rate limit      │ │  rate_limit_     │ │  Phase 2:        │
    │  state (Phase 1) │ │  configs table   │ │  Distributed     │
    │  - Token counts  │ │                  │ │  rate limiting   │
    │  - Timestamps    │ │  Custom limits   │ │  across multiple │
    │                  │ │  per identifier  │ │  app instances   │
    └──────────────────┘ └──────────────────┘ └──────────────────┘
```

## Request Flow

```
HTTP Request Arrives
        │
        ▼
RateLimitFilter.doFilterInternal()
        │
        ├─ Is path excluded? (actuator, swagger)
        │   └─ YES → Skip rate limiting, pass through
        │
        ▼
Extract Identifier
        │
        ├─ X-User-Id header present? → use it
        ├─ X-API-Key header present? → use it
        └─ Fallback → Client IP address
        │
        ▼
Add X-RateLimit-* headers to response
        │
        ▼
RateLimiterService.checkAndConsume(identifier)
        │
        ▼
Load RateLimitConfig from PostgreSQL
        │
        └─ Not found? → Use defaults from application.yml
        │
        ▼
Active Strategy: isAllowed(identifier, config)
        │
        ├─ TOKEN_BUCKET:
        │   1. Get/create entry in ConcurrentHashMap
        │   2. synchronized(entry) {
        │       refill tokens based on elapsed time
        │       if tokens >= 1 → consume, return true
        │       else → return false
        │   }
        │
        └─ SLIDING_WINDOW:
            1. Get/create entry in ConcurrentHashMap
            2. synchronized(entry) {
                prune timestamps older than windowStart
                if size < maxRequests → add timestamp, return true
                else → return false
            }
        │
        ├─ ALLOWED (true)
        │   └─ filterChain.doFilter() → Controller → HTTP 200
        │
        └─ REJECTED (false)
            └─ Write HTTP 429 JSON response
               Add Retry-After header
               Stop filter chain
```

## Thread Safety

| Component | Mechanism | Why |
|-----------|-----------|-----|
| `ConcurrentHashMap` | Lock-free reads, segment locks on write | Safe concurrent access to different keys |
| `synchronized(entry)` | Object-level lock | Atomic refill + consume within one entry |
| `AtomicLong` | Compare-and-swap | Lock-free token count updates |
| `ConcurrentLinkedDeque` | Lock-free queue | Safe concurrent timestamp tracking |

## Data Model

```
rate_limit_configs (PostgreSQL)
┌─────────────────────────────────────────────────────┐
│ id (PK)  │ identifier │ identifier_type │ max_requests │ window_seconds │ refill_rate │
├──────────┼────────────┼─────────────────┼──────────────┼────────────────┼─────────────┤
│ 1        │ user123    │ USER_ID         │ 1000         │ 60             │ 50          │
│ 2        │ 192.168.1.1│ IP_ADDRESS      │ 50           │ 60             │ 5           │
│ 3        │ key-abc    │ API_KEY         │ 500          │ 3600           │ 10          │
└─────────────────────────────────────────────────────┘

In-Memory Store (ConcurrentHashMap)
┌─────────────────────────────────────────────────────┐
│ Key: identifier → Value: RateLimitEntry             │
│                                                     │
│ "user123" → {                                       │
│   tokenCount: AtomicLong(45),                       │
│   lastRefillTime: AtomicLong(1708000000000),        │
│   requestTimestamps: ConcurrentLinkedDeque[...]     │
│ }                                                   │
└─────────────────────────────────────────────────────┘
```
