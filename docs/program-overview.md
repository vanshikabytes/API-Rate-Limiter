# 🚦 API Rate Limiter Service — Program Overview & Build Plan

> **Project:** Option 3 — API Rate Limiter Service  
> **Duration:** 6-Week Intensive Implementation  
> **Tech Stack:** Java (Spring Boot) + Redis + PostgreSQL + Docker  
> **Total Points:** 100  

---

## 📋 Table of Contents

1. [What Is This Project?](#what-is-this-project)
2. [Program Structure](#program-structure)
3. [Phase 1 — Core Implementation (Weeks 1–2)](#phase-1--core-implementation-weeks-12)
4. [Phase 2 — Feature Extensions (Weeks 3–4)](#phase-2--feature-extensions-weeks-34)
5. [Phase 3 — Advanced Features (Weeks 5–6)](#phase-3--advanced-features-weeks-56)
6. [Evaluation & Scoring Breakdown](#evaluation--scoring-breakdown)
7. [Submission Requirements](#submission-requirements)
8. [My Chosen Options Strategy](#my-chosen-options-strategy)
9. [Architecture Overview](#architecture-overview)
10. [Tech Stack Decisions](#tech-stack-decisions)
11. [Project Folder Structure](#project-folder-structure)
12. [Key Algorithms to Implement](#key-algorithms-to-implement)
13. [API Endpoints Plan](#api-endpoints-plan)
14. [Week-by-Week Build Plan](#week-by-week-build-plan)
15. [Risk & Mitigation](#risk--mitigation)

---

## 🎯 What Is This Project?

Build a **production-grade API Rate Limiter Service** — middleware that enforces request limits per user, IP, or API key to prevent abuse and ensure fair usage of APIs.

### Core Problem Being Solved
- Without rate limiting, a single bad actor can flood an API with thousands of requests, crashing the server for everyone.
- Rate limiters act as a **traffic cop** — they count how many requests a client makes in a time window and block excess requests with an `HTTP 429 Too Many Requests` response.

### Real-World Analogy
> Think of it like a nightclub bouncer: "You can enter 10 times per hour. After that, you wait outside."

---

## 🗓️ Program Structure

```
Week 1-2  →  Phase 1: Core Rate Limiter (MANDATORY)
Week 3-4  →  Phase 2: Choose 1 of 4 Feature Extensions
Week 5-6  →  Phase 3: Choose 1 of 4 Advanced Features
              + Final Demo + Submission
```

### Evaluation Weights
| Phase | Points | Description |
|-------|--------|-------------|
| Core Requirements | 40 pts | The fundamental rate limiter |
| Documentation | 15 pts | README, Architecture Diagram, Swagger |
| Testing & Quality | 20 pts | Unit tests, Integration tests, Code quality |
| Phase 2 Option | 15 pts | Chosen intermediate feature |
| Phase 3 Option | 10 pts | Chosen advanced feature |
| **Total** | **100 pts** | |

---

## 🏗️ Phase 1 — Core Implementation (Weeks 1–2)

### Mandatory Features

#### 1. Rate Limit Validation Engine
- **Identify the requester** → by User ID, IP address, or API Key
- **Count their calls** within a time window
- **Return HTTP 429** when limit is exceeded
- **Return HTTP 200** with rate limit headers when allowed

#### 2. Algorithm Choices (Pick One or Both)
| Algorithm | Pros | Cons | Recommendation |
|-----------|------|------|----------------|
| **Token Bucket** | Simple, allows bursts, memory efficient | Less accurate for strict limits | ✅ Start here |
| **Sliding Window Counter** | Smooth throttling, more accurate | Slightly more complex | ✅ Add as Phase 2 upgrade |

**Token Bucket Logic:**
```
Each user has a "bucket" with N tokens.
Each request consumes 1 token.
Tokens refill at a fixed rate (e.g., 10 tokens/second).
If bucket is empty → reject with 429.
```

**Sliding Window Logic:**
```
Track timestamps of all requests in a rolling window.
Count requests in [now - windowSize, now].
If count >= limit → reject with 429.
```

#### 3. Configurable Limits
- Limits must be configurable via **database** OR **config file** (application.yml)
- Support different limits per:
  - User ID
  - IP Address
  - API Key
  - Endpoint path

#### 4. Reset Capabilities
- **Automatic Reset:** Limits reset after the time window expires
- **Manual Reset (Admin API):** Admin can manually reset a specific user's counter

#### 5. Storage (Phase 1)
- Use **HashMap (in-memory)** for Phase 1
- Key: `userId` or `ipAddress` or `apiKey`
- Value: request count + timestamp

#### 6. HTTP Response Headers
Every response must include:
```
X-RateLimit-Limit: 100          # Max requests allowed
X-RateLimit-Remaining: 45       # Requests left in window
X-RateLimit-Reset: 1708000000   # Unix timestamp when limit resets
Retry-After: 60                 # Seconds until retry (only on 429)
```

#### Phase 1 Deliverables
- [ ] Working rate limiter middleware/filter
- [ ] REST API endpoints (see API plan below)
- [ ] Postman collection with test scenarios
- [ ] Minimum 5 unit tests
- [ ] Basic README

---

## 🔧 Phase 2 — Feature Extensions (Weeks 3–4)

> **Choose ONE of the following options:**

### Option A: Multiple Tiers (User Tier System)
- Implement **Free / Pro / Enterprise** tiers with different limits
- Auto-detect user tier from database
- Admin API to upgrade/downgrade users
- Example limits:
  - Free: 100 req/hour
  - Pro: 1,000 req/hour
  - Enterprise: 10,000 req/hour

### Option B: Distributed Rate Limiting with Redis ⭐ (Recommended)
- Replace in-memory HashMap with **Redis** as centralized store
- Multiple API servers share the same Redis instance
- Use **atomic operations** (Lua scripts) to prevent race conditions
- TTL-based auto-cleanup of old records
- Redis Key Pattern: `rate_limit:{identifier}`
- Redis Value: Hash with `{tokens, capacity, refill_rate, last_refill}`
- Test: Start 2-3 API instances, verify total requests don't exceed limit

### Option C: Advanced Rules Engine
- Per-endpoint rate limits (e.g., `/search` = 100/min, `/login` = 5/min)
- Time-based rules (lower limits during peak hours 9AM–5PM)
- Combined limits (1000/hour AND 10/second)
- IP-based + User-based combined identifiers
- Rule priority system for overlapping rules

### Option D: Analytics Dashboard
- Track all rate limit checks and violations in real-time
- Identify top users/IPs hitting limits
- Show usage trends and alert on unusual patterns
- Export data as CSV/JSON
- Metrics: Total requests, violation %, unique identifiers, charts

---

## 🚀 Phase 3 — Advanced Features (Weeks 5–6)

> **Choose ONE of the following options:**

### Option A: High Performance (10K+ req/sec)
- Benchmark: Sustain **10,000+ requests per second**
- Latency: P95 < 10ms, P99 < 50ms
- Use lock-free data structures, minimize lock contention
- Memory pooling, efficient data types
- Document benchmarks using **k6, Locust, or Apache Bench**

### Option B: Resiliency & Circuit Breaker ⭐ (Recommended)
- Implement **Circuit Breaker Pattern:**
  - `CLOSED` → Normal operation
  - `OPEN` → Rate limiter is down, fail-open or fail-closed
  - `HALF-OPEN` → Testing if service recovered
- Configurable fail-open vs fail-closed modes
- Automated temporary blocking with escalating durations: 5min → 15min → 1hr
- Monitor rate limiter health (response time, error rates)

### Option C: Dynamic Whitelisting & Blacklisting
- **Whitelist:** Identifiers that bypass all limits
- **Blacklist:** Permanent or temporary blocks
- CRUD APIs for managing lists
- Audit logs for all changes
- Test: Verify whitelist bypass and blacklist rejection

### Option D: Cost-Based Rate Limiting
- Assign "costs" to operations based on resource intensity:
  - `GET /profile` = 1 point
  - `GET /search` = 5 points
  - `POST /report` = 50 points
- Users have a "point budget" per window (e.g., 1000 pts/hour)
- Reject when points insufficient even if request count is low

---

## 📊 Evaluation & Scoring Breakdown

### Core Requirements (40 Points)
| Criteria | Points | What's Needed |
|----------|--------|---------------|
| Sliding Window / Token Bucket | 15 pts | At least one algorithm fully working |
| Multiple Users Support | 10 pts | Rate limit per user, IP, API key |
| Race Condition Handling | 10 pts | Thread-safe operations (atomic/locks) |
| HTTP Headers & Status Codes | 5 pts | Correct 429 responses + rate limit headers |

### Documentation (15 Points)
| Criteria | Points | What's Needed |
|----------|--------|---------------|
| README | 5 pts | Setup instructions, usage, examples |
| Architecture Diagram | 5 pts | System design diagram |
| API Docs / Swagger | 5 pts | Swagger UI or Postman collection |

### Testing & Quality (20 Points)
| Criteria | Points | What's Needed |
|----------|--------|---------------|
| Unit Tests | 10 pts | Minimum 5, cover core logic |
| Integration Tests | 5 pts | End-to-end API tests |
| Code Organization | 5 pts | SOLID principles, clean code |

### Phase Selection (25 Points)
| Criteria | Points | What's Needed |
|----------|--------|---------------|
| Phase 2 Option | 15 pts | Complete chosen Phase 2 feature |
| Phase 3 Option | 10 pts | Complete chosen Phase 3 feature |

---

## 📦 Submission Requirements

### Required Files & Structure
```
/
├── src/                    # All source code
├── tests/                  # All test files
├── docs/                   # Documentation files
│   ├── architecture.png    # System architecture diagram
│   └── api-docs.md         # API documentation
├── README.md               # Project overview + setup guide
└── docker-compose.yml      # Docker setup for easy running
```

### Deliverables Checklist
- [ ] Public GitHub repository link
- [ ] 2-minute video demo (screen recording)
- [ ] Working Docker setup (`docker-compose up` should start everything)
- [ ] Postman collection exported as JSON

### Success Metrics
| Metric | Target |
|--------|--------|
| Baseline throughput | 1,000 req/sec |
| P95 Latency | < 50ms |
| Code quality | Clean, documented, SOLID principles |
| Test coverage | Minimum 5 unit tests + integration tests |

### Bonus Features (Extra Credit)
- GraphQL support
- Webhook notifications when limits are hit
- Kubernetes manifests (k8s deployment files)
- Prometheus + Grafana monitoring integration

---

## 🎯 My Chosen Options Strategy

Based on the scoring and complexity analysis, here's my recommended approach:

### Phase 2 Choice: **Option B — Distributed Rate Limiting with Redis**
**Why?**
- Redis is industry-standard for rate limiting (used by Stripe, GitHub, etc.)
- Demonstrates real distributed systems knowledge
- Directly enables Phase 3 performance work
- Atomic Lua scripts show advanced Redis knowledge

### Phase 3 Choice: **Option B — Resiliency & Circuit Breaker**
**Why?**
- Circuit Breaker is a fundamental distributed systems pattern
- Shows production-readiness thinking
- Pairs naturally with Redis (can monitor Redis health)
- Escalating block durations show thoughtful design

### Combined Strategy
```
Phase 1: Token Bucket + Sliding Window (both, for bonus points)
Phase 2: Redis Distributed Limiting (replaces in-memory)
Phase 3: Circuit Breaker (wraps Redis calls)
Bonus:   Prometheus/Grafana metrics
```

---

## 🏛️ Architecture Overview

```
                    ┌─────────────────────────────────────┐
                    │           CLIENT REQUESTS            │
                    └──────────────┬──────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────────────┐
                    │         SPRING BOOT API              │
                    │  ┌─────────────────────────────┐    │
                    │  │   Rate Limit Filter/Interceptor│  │
                    │  │   (Checks every request)      │  │
                    │  └──────────┬──────────────────┘    │
                    │             │                        │
                    │  ┌──────────▼──────────────────┐    │
                    │  │   Rate Limiter Service        │    │
                    │  │   - Token Bucket Algorithm    │    │
                    │  │   - Sliding Window Algorithm  │    │
                    │  └──────────┬──────────────────┘    │
                    │             │                        │
                    │  ┌──────────▼──────────────────┐    │
                    │  │   Circuit Breaker (Phase 3)   │    │
                    │  └──────────┬──────────────────┘    │
                    └─────────────┼───────────────────────┘
                                  │
               ┌──────────────────┼──────────────────┐
               │                  │                   │
               ▼                  ▼                   ▼
    ┌──────────────┐   ┌──────────────────┐  ┌──────────────┐
    │    REDIS      │   │   POSTGRESQL     │  │  PROMETHEUS  │
    │  (Rate State) │   │  (User Config,   │  │  (Metrics)   │
    │  - Counters   │   │   Tiers, Rules)  │  │              │
    │  - TTL keys   │   │                  │  └──────────────┘
    └──────────────┘   └──────────────────┘
```

### Request Flow
```
Request Arrives
      │
      ▼
Extract Identifier (User ID / IP / API Key)
      │
      ▼
Check Circuit Breaker State
      │
      ├── OPEN → Fail-open (allow) or Fail-closed (reject)
      │
      └── CLOSED/HALF-OPEN ──►
                              │
                              ▼
                    Check Redis for current count
                              │
                              ├── Under Limit → Allow + Increment counter
                              │                 Add rate limit headers
                              │                 Return 200
                              │
                              └── Over Limit → Return 429
                                               Add Retry-After header
                                               Log violation
```

---

## 🛠️ Tech Stack Decisions

| Component | Technology | Why |
|-----------|-----------|-----|
| **Language** | Java 17 | Required by assessment |
| **Framework** | Spring Boot 3.x | Industry standard, great for REST APIs |
| **Rate State Storage** | Redis 7.x | Atomic ops, TTL support, blazing fast |
| **Config/User DB** | PostgreSQL | Relational, good for user tiers & rules |
| **Containerization** | Docker + Docker Compose | Required by submission |
| **Testing** | JUnit 5 + Mockito | Java standard |
| **API Docs** | Springdoc OpenAPI (Swagger) | Required by submission |
| **Monitoring** | Prometheus + Grafana | Bonus points |
| **Build Tool** | Maven | Standard for Spring Boot |

---

## 📁 Project Folder Structure

```
API-Rate-Limiter/
├── Planning/                           # 📋 This planning folder
│   ├── program-overview.md             # This file
│   ├── phase1-implementation.md        # Detailed Phase 1 plan
│   ├── phase2-redis-plan.md            # Redis distributed limiting plan
│   ├── phase3-circuit-breaker.md       # Circuit breaker plan
│   └── api-design.md                   # All API endpoints design
│
├── src/
│   └── main/
│       └── java/com/ratelimiter/
│           ├── config/                 # Spring configs, Redis config
│           ├── controller/             # REST controllers
│           ├── service/                # Business logic
│           │   ├── RateLimiterService.java
│           │   ├── TokenBucketService.java
│           │   └── SlidingWindowService.java
│           ├── filter/                 # Spring filter (intercepts all requests)
│           │   └── RateLimitFilter.java
│           ├── model/                  # Entity classes
│           ├── repository/             # DB access layer
│           ├── dto/                    # Request/Response DTOs
│           └── exception/              # Custom exceptions
│
├── src/test/                           # All tests
├── docs/                               # Architecture diagrams, API docs
├── docker-compose.yml                  # Redis + PostgreSQL + App
├── README.md                           # Project overview
└── pom.xml                             # Maven dependencies
```

---

## 🔑 Key Algorithms to Implement

### Token Bucket (Phase 1)

```java
// Pseudocode
class TokenBucket {
    long capacity;        // Max tokens
    long tokens;          // Current tokens
    long refillRate;      // Tokens added per second
    long lastRefillTime;  // Last time tokens were added

    boolean allowRequest() {
        refillTokens();
        if (tokens > 0) {
            tokens--;
            return true;  // Allow
        }
        return false;     // Reject → 429
    }

    void refillTokens() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        long tokensToAdd = elapsed * refillRate / 1000;
        tokens = Math.min(capacity, tokens + tokensToAdd);
        lastRefillTime = now;
    }
}
```

### Sliding Window Counter (Phase 1)

```java
// Pseudocode
class SlidingWindowCounter {
    long windowSize;       // e.g., 60000ms (1 minute)
    int maxRequests;       // e.g., 100
    Deque<Long> timestamps; // Timestamps of recent requests

    boolean allowRequest() {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSize;

        // Remove old timestamps outside the window
        while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
            timestamps.poll();
        }

        if (timestamps.size() < maxRequests) {
            timestamps.add(now);
            return true;  // Allow
        }
        return false;     // Reject → 429
    }
}
```

### Redis Lua Script (Phase 2 — Atomic Token Bucket)

```lua
-- Atomic token bucket in Redis Lua script
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1]) or capacity
local last_refill = tonumber(bucket[2]) or now

-- Refill tokens based on elapsed time
local elapsed = now - last_refill
local new_tokens = math.min(capacity, tokens + (elapsed * refill_rate / 1000))

if new_tokens >= 1 then
    new_tokens = new_tokens - 1
    redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', now)
    redis.call('EXPIRE', key, 3600)
    return 1  -- Allowed
else
    return 0  -- Rejected
end
```

---

## 🌐 API Endpoints Plan

### Rate Limiting Endpoints (Core)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/check` | Check if a request is allowed |
| `POST` | `/api/v1/request` | Submit a request (auto rate-limited) |

### Admin Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/admin/reset/{identifier}` | Manually reset a user's counter |
| `GET` | `/admin/status/{identifier}` | Get current rate limit status |
| `PUT` | `/admin/config` | Update rate limit configuration |
| `GET` | `/admin/config` | Get current configuration |

### User/Tier Management (Phase 2 Option A)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/users` | List all users and their tiers |
| `PUT` | `/admin/users/{id}/tier` | Upgrade/downgrade user tier |

### Whitelist/Blacklist (Phase 3 Option C)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/admin/whitelist` | Add to whitelist |
| `DELETE` | `/admin/whitelist/{id}` | Remove from whitelist |
| `POST` | `/admin/blacklist` | Add to blacklist |
| `DELETE` | `/admin/blacklist/{id}` | Remove from blacklist |

### Analytics (Phase 2 Option D)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/analytics/overview` | Total requests, violations |
| `GET` | `/analytics/top-users` | Top 10 users by request count |
| `GET` | `/analytics/export` | Export data as CSV/JSON |

---

## 📅 Week-by-Week Build Plan

### Week 1: Foundation
- [ ] Set up Spring Boot project with Maven
- [ ] Configure Docker Compose (Redis + PostgreSQL + App)
- [ ] Implement Token Bucket algorithm (in-memory)
- [ ] Create `RateLimitFilter` that intercepts all requests
- [ ] Add HTTP response headers (X-RateLimit-*)
- [ ] Return HTTP 429 when limit exceeded
- [ ] Write 5 unit tests for Token Bucket

### Week 2: Core Completion
- [ ] Implement Sliding Window Counter algorithm
- [ ] Add configurable limits via `application.yml`
- [ ] Build Admin API (reset, status, config endpoints)
- [ ] Add PostgreSQL for user configuration storage
- [ ] Write integration tests
- [ ] Set up Swagger/OpenAPI documentation
- [ ] Create Postman collection

### Week 3: Phase 2 — Redis Integration
- [ ] Replace in-memory HashMap with Redis
- [ ] Write Lua script for atomic token bucket
- [ ] Test with multiple API instances
- [ ] Add TTL-based auto-cleanup
- [ ] Test graceful degradation when Redis is down

### Week 4: Phase 2 — Polish & Testing
- [ ] Load test with multiple concurrent users
- [ ] Verify race condition handling
- [ ] Add Redis connection pooling
- [ ] Write integration tests for distributed scenario
- [ ] Document Redis architecture

### Week 5: Phase 3 — Circuit Breaker
- [ ] Implement Circuit Breaker states (OPEN/CLOSED/HALF-OPEN)
- [ ] Add configurable fail-open vs fail-closed modes
- [ ] Implement escalating block durations (5min → 15min → 1hr)
- [ ] Monitor Redis health metrics
- [ ] Add Prometheus metrics endpoint

### Week 6: Final Polish & Submission
- [ ] Add Grafana dashboard (bonus)
- [ ] Write comprehensive README
- [ ] Create architecture diagram
- [ ] Record 2-minute demo video
- [ ] Final testing pass
- [ ] Push to public GitHub repo
- [ ] Submit

---

## ⚠️ Risk & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Redis connection issues | Medium | High | Implement fallback to in-memory |
| Race conditions in concurrent tests | High | High | Use Lua scripts + atomic Redis ops |
| Performance not meeting 1000 req/sec | Low | High | Profile early, use async where possible |
| Docker networking issues | Medium | Medium | Test docker-compose early in Week 1 |
| Scope creep (trying all options) | High | Medium | Stick to chosen options, don't deviate |

---

## 📝 Key Design Principles to Follow

1. **SOLID Principles** — Each class has one responsibility (evaluators check this!)
2. **Strategy Pattern** — Use interface for rate limiting algorithms (easy to swap Token Bucket ↔ Sliding Window)
3. **Filter/Interceptor Pattern** — Rate limiting should be transparent middleware, not in business logic
4. **Fail-Safe Defaults** — If rate limiter fails, decide: fail-open (allow all) or fail-closed (block all)
5. **Idempotent Admin APIs** — Reset operations should be safe to call multiple times
6. **Observability First** — Log every rate limit decision for debugging

---

## 🔗 Reference Resources

- [Token Bucket Algorithm — Wikipedia](https://en.wikipedia.org/wiki/Token_bucket)
- [Redis Rate Limiting Patterns](https://redis.io/docs/manual/patterns/rate-limiting/)
- [Spring Boot Filter Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Circuit Breaker Pattern — Martin Fowler](https://martinfowler.com/bliki/CircuitBreaker.html)
- [k6 Load Testing](https://k6.io/docs/)

---

*Last Updated: February 2026*  
*Project: API Rate Limiter — Internship Technical Assessment*
