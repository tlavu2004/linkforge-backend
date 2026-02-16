# LinkForge – Task Breakdown (Commit-level)

> Mỗi task = 1 commit. Thứ tự từ trên xuống dưới.
> Convention: `type(scope): message` (Conventional Commits)

---

## Phase 0 – Project Setup

### Task 0.1: `init: initialize Spring Boot project`
- Khởi tạo project Spring Boot (Spring Initializr hoặc manual)
- Java 17+, Maven
- Dependencies: `spring-boot-starter-web`
- Cấu hình `application.yml` cơ bản (port, app name)
- **Verify**: `mvn spring-boot:run` → app start thành công

### Task 0.2: `chore: add core dependencies and project structure`
- Thêm dependencies: `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `lombok`, `tsid-creator`
- Tạo package structure theo Clean Architecture:
  ```
  com.linkforge/
    ├── modules/
    │    ├── shortlink/
    │    │    ├── controller/
    │    │    ├── application/usecase/
    │    │    ├── application/dto/
    │    │    ├── domain/entity/
    │    │    ├── domain/valueobject/
    │    │    ├── domain/service/
    │    │    ├── domain/exception/
    │    │    ├── repository/
    │    │    └── infrastructure/
    │    └── common/
    └── LinkForgeApplication.java
  ```
- **Verify**: project compile thành công

### Task 0.3: `feat: add health check endpoint`
- Tạo `GET /health` endpoint (hoặc dùng Spring Actuator)
- Thêm `spring-boot-starter-actuator` dependency
- Cấu hình expose `/actuator/health`
- **Verify**: `GET /actuator/health` → `{"status": "UP"}`

---

## Phase 1 – Domain Layer

### Task 1.1: `feat(domain): add OriginalUrl value object`
- Tạo `OriginalUrl` value object (thuần Java, không Spring)
- Validation: scheme + host required (`http://` hoặc `https://`)
- Normalize: trim whitespace, lowercase scheme & host
- Reject dangerous schemes: `javascript:`, `data:`, `file:`
- Custom exception: `InvalidUrlException`
- **Unit test**: valid URLs, invalid URLs, normalization, dangerous schemes
- **Verify**: `mvn test` pass

### Task 1.2: `feat(domain): add ShortCode value object`
- Tạo `ShortCode` value object (thuần Java)
- Validation: non-null, non-blank, chỉ chứa `[0-9a-zA-Z]`
- Immutable (final field, no setter)
- Factory method `ShortCode.of(String)`
- Custom exception: `InvalidShortCodeException`
- **Unit test**: valid codes, invalid codes (special chars, empty, too long)
- **Verify**: `mvn test` pass

### Task 1.3: `feat(domain): add Base62 encoder utility`
- Tạo `Base62` utility class trong `common/`
- `encode(long): String` — convert long → Base62 string
- `decode(String): long` — convert Base62 string → long
- Charset: `0-9a-zA-Z` (62 ký tự)
- **Unit test**: encode/decode roundtrip, known values, edge cases (0, Long.MAX_VALUE)
- **Verify**: `mvn test` pass

### Task 1.4: `feat(domain): add ShortLink entity`
- Tạo `ShortLink` entity (thuần Java, không JPA annotations)
- Fields: `id`, `code` (ShortCode), `originalUrl` (OriginalUrl), `createdAt`, `expiresAt`, `clickCount`, `isActive`, `deleteTokenHash`
- Domain methods:
  - `isExpired(): boolean`
  - `incrementClickCount(): void`
  - `deactivate(): void`
  - `matchesDeleteToken(String rawToken): boolean`
- Enforce invariants: clickCount >= 0, originalUrl not null
- **Unit test**: tạo entity, check expiration, increment click, deactivate, delete token matching
- **Verify**: `mvn test` pass

### Task 1.5: `feat(domain): add domain exceptions`
- Tạo package `domain/exception/`:
  - `ShortLinkNotFoundException`
  - `ShortLinkExpiredException`
  - `InvalidUrlException` (nếu chưa tạo ở task 1.1)
  - `InvalidShortCodeException` (nếu chưa tạo ở task 1.2)
  - `InvalidDeleteTokenException`
- Tất cả extend từ base `DomainException`
- **Verify**: project compile thành công

### Task 1.6: `feat(domain): add ShortLinkRepository port interface`
- Tạo `ShortLinkRepository` interface trong `repository/` (domain port)
  - `save(ShortLink): ShortLink`
  - `findByCode(ShortCode): Optional<ShortLink>`
  - `findById(Long): Optional<ShortLink>`
- Interface thuần Java, không Spring annotation
- **Verify**: project compile thành công

---

## Phase 2 – Database & Infrastructure

### Task 2.1: `feat(infra): add PostgreSQL and Flyway configuration`
- Thêm dependencies: `postgresql`, `flyway-core`
- Cấu hình `application.yml`:
  - PostgreSQL datasource (local hoặc Docker)
  - Flyway migration settings
  - JPA/Hibernate settings (ddl-auto: validate)
- Tạo `docker-compose.yml` cho PostgreSQL
- **Verify**: `docker-compose up -d` → PostgreSQL running

### Task 2.2: `feat(infra): add Flyway migration for short_links table`
- Tạo migration `V1__create_short_links_table.sql`:
  ```sql
  CREATE TABLE short_links (
    id BIGINT PRIMARY KEY,
    code VARCHAR(10) NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    delete_token_hash VARCHAR(64),
    CONSTRAINT uk_short_links_code UNIQUE (code)
  );
  CREATE INDEX idx_short_links_created_at ON short_links (created_at);
  CREATE INDEX idx_short_links_expires_at ON short_links (expires_at) WHERE is_active = true;
  ```
- **Verify**: app start → Flyway migration applied → table exists

### Task 2.3: `feat(infra): add JPA entity and repository adapter`
- Tạo `ShortLinkJpaEntity` (JPA entity với annotations)
- Tạo `ShortLinkJpaRepository` (extends `JpaRepository`)
- Tạo `ShortLinkMapper` (JPA entity ↔ domain entity conversion)
- Tạo `ShortLinkRepositoryAdapter` implements domain `ShortLinkRepository`
  - Delegate tới `ShortLinkJpaRepository`
  - Convert qua `ShortLinkMapper`
- **Verify**: project compile thành công

### Task 2.4: `test(infra): add repository integration tests`
- Integration test cho `ShortLinkRepositoryAdapter`
- Dùng `@DataJpaTest` + Testcontainers PostgreSQL (hoặc embedded nếu chưa setup)
- Test: save, findByCode, findById
- **Verify**: `mvn test` pass

---

## Phase 3 – Short Code Generation

### Task 3.1: `feat(domain): add TSID-based short code generator`
- Tạo `ShortCodeGenerator` interface (domain service port)
- Tạo `TsidShortCodeGenerator` implementation:
  - Dùng `tsid-creator` library
  - Generate TSID → Base62 encode → ShortCode
- Đăng ký Spring bean
- **Unit test**: generate 1000 codes → tất cả unique, format hợp lệ
- **Verify**: `mvn test` pass

---

## Phase 4 – Application Layer (Use Cases)

### Task 4.1: `feat(app): add ApiResponse wrapper and DTOs`
- Tạo `ApiResponse<T>` wrapper class:
  ```java
  { success: boolean, data: T, error: String, timestamp: Instant }
  ```
- Tạo DTOs:
  - `CreateShortLinkRequest` (originalUrl, expiresAt?)
  - `ShortLinkResponse` (code, originalUrl, shortUrl, createdAt, expiresAt, clickCount)
  - `CreateShortLinkResponse` extends ShortLinkResponse + deleteToken
- **Verify**: project compile thành công

### Task 4.2: `feat(app): add CreateShortLinkUseCase`
- Tạo `CreateShortLinkUseCase`:
  1. Validate originalUrl → `OriginalUrl` value object
  2. Generate code via `ShortCodeGenerator`
  3. Generate delete token (UUID) → hash SHA-256
  4. Build `ShortLink` entity
  5. Save via repository
  6. Return `CreateShortLinkResponse` (với raw delete token)
- **Unit test**: mock repository & generator, verify flow
- **Verify**: `mvn test` pass

### Task 4.3: `feat(app): add ResolveShortLinkUseCase`
- Tạo `ResolveShortLinkUseCase`:
  1. Lookup by code via repository
  2. Check `isActive` → throw nếu false
  3. Check `isExpired()` → throw nếu expired
  4. Return original URL
- Chưa có cache (thêm ở Phase 6)
- **Unit test**: mock repository, test happy path, not found, expired, inactive
- **Verify**: `mvn test` pass

### Task 4.4: `feat(app): add DeleteShortLinkUseCase`
- Tạo `DeleteShortLinkUseCase`:
  1. Lookup by code via repository
  2. Verify delete token (hash & compare)
  3. Call `entity.deactivate()` → soft delete
  4. Save updated entity
- **Unit test**: valid token, invalid token, link not found
- **Verify**: `mvn test` pass

### Task 4.5: `feat(app): add GetShortLinkUseCase`
- Tạo `GetShortLinkUseCase`:
  1. Lookup by code
  2. Check isActive
  3. Return metadata DTO (không trả deleteToken)
- **Unit test**: happy path, not found, inactive
- **Verify**: `mvn test` pass

---

## Phase 5 – Controller Layer (API)

### Task 5.1: `feat(api): add global exception handler`
- Tạo `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Map exceptions → HTTP status:
  - `InvalidUrlException` → 400
  - `InvalidShortCodeException` → 400
  - `ShortLinkNotFoundException` → 404
  - `ShortLinkExpiredException` → 410
  - `InvalidDeleteTokenException` → 403
  - `MethodArgumentNotValidException` → 400
  - `Exception` (catch-all) → 500
- Tất cả trả về `ApiResponse` format
- **Verify**: project compile thành công

### Task 5.2: `feat(api): add ShortLinkController (CRUD endpoints)`
- Tạo `ShortLinkController`:
  - `POST /api/v1/links` → `CreateShortLinkUseCase`
  - `GET /api/v1/links/{code}` → `GetShortLinkUseCase`
  - `DELETE /api/v1/links/{code}?token={deleteToken}` → `DeleteShortLinkUseCase`
- Mỗi endpoint trả về `ApiResponse<T>`
- Bean validation trên request (`@Valid`)
- **Verify**: app start, test bằng curl/Postman

### Task 5.3: `feat(api): add RedirectController`
- Tạo `RedirectController`:
  - `GET /r/{code}` → `ResolveShortLinkUseCase` → HTTP 301 redirect
- Tách riêng khỏi `ShortLinkController` (khác path prefix)
- Return `HttpStatus.MOVED_PERMANENTLY` với `Location` header
- **Verify**: `curl -v localhost:8080/r/{code}` → 301 + Location header

### Task 5.4: `test(api): add controller integration tests`
- MockMvc tests cho tất cả endpoints:
  - POST create → 201, verify response format + deleteToken present
  - GET metadata → 200, verify response
  - DELETE with valid token → 200
  - DELETE with invalid token → 403
  - GET redirect → 301 + Location
  - GET not found → 404
  - POST invalid URL → 400
- **Verify**: `mvn test` pass

---

## Phase 6 – Cache (Redis)

### Task 6.1: `feat(infra): add Redis configuration`
- Thêm dependency: `spring-boot-starter-data-redis`
- Thêm Redis vào `docker-compose.yml`
- Cấu hình `application.yml`: Redis host, port
- Cấu hình `RedisTemplate` (serializer: JSON)
- **Verify**: app start, Redis connection OK

### Task 6.2: `feat(infra): add ShortLinkCacheService`
- Tạo `ShortLinkCacheService`:
  - `get(ShortCode): Optional<ShortLink>` — lookup cache
  - `put(ShortLink): void` — populate cache với TTL
  - `evict(ShortCode): void` — invalidate cache entry
- Cache key: `link:{code}`
- TTL logic:
  - Có expiresAt → TTL = expiresAt - now
  - Không expire → TTL = 24h
- **Unit test**: mock RedisTemplate, verify key format & TTL
- **Verify**: `mvn test` pass

### Task 6.3: `feat(app): integrate cache into ResolveShortLinkUseCase`
- Update `ResolveShortLinkUseCase`:
  1. **Cache lookup first**
  2. Cache hit → return (skip DB)
  3. Cache miss → DB lookup → **populate cache** → return
- Update `DeleteShortLinkUseCase`: thêm `cacheService.evict()` sau soft delete
- **Test**: verify cache hit path, cache miss + populate path, evict on delete
- **Verify**: `mvn test` pass

### Task 6.4: `feat(infra): add Redis health indicator`
- Custom `HealthIndicator` cho Redis connectivity
- Register trong Spring Actuator
- **Verify**: `/actuator/health` show Redis status

---

## Phase 7 – Click Tracking (Async)

### Task 7.1: `feat(app): add async click tracking with Spring Events`
- Tạo `ClickEvent` record (shortCode, timestamp, ip, userAgent)
- Tạo `ClickEventPublisher` — publish Spring ApplicationEvent
- Tạo `ClickEventListener` (`@Async`, `@EventListener`):
  - Nhận event → update click_count trong DB
- Enable `@EnableAsync` trong config
- Update `ResolveShortLinkUseCase`: publish ClickEvent sau resolve
- **Test**: verify event published, async update xảy ra
- **Verify**: tạo link → redirect → check click_count tăng

---

## Phase 8 – Rate Limiting

### Task 8.1: `feat(ratelimit): add IP-based rate limiter`
- Tạo `RateLimiterService`:
  - Redis sliding window counter
  - Key: `ratelimit:{ip}`
  - Limit: 10 requests/phút (configurable qua application.yml)
- Tạo `RateLimitException` extends `DomainException`
- **Unit test**: mock Redis, verify allow/deny logic
- **Verify**: `mvn test` pass

### Task 8.2: `feat(ratelimit): add rate limit filter`
- Tạo `RateLimitFilter` (Spring `OncePerRequestFilter` hoặc `HandlerInterceptor`)
- Apply cho `POST /api/v1/links` (chỉ rate limit create, không limit redirect)
- Return 429 khi vượt limit
- Thêm `X-RateLimit-Remaining` header trong response
- Update `GlobalExceptionHandler` cho `RateLimitException`
- **Test**: gửi > 10 requests → verify 429 returned
- **Verify**: manual test bằng curl

---

## Phase 9 – Observability

### Task 9.1: `feat(observability): add structured logging and correlation ID`
- Cấu hình Logback JSON format (`logback-spring.xml`)
- Tạo `CorrelationIdFilter`: generate + set MDC correlation ID cho mỗi request
- Thêm correlation ID vào mọi log entry
- Add meaningful logs:
  - Link created: `INFO code={}, url={}`
  - Redirect: `INFO code={}, latencyMs={}`
  - Error: `WARN/ERROR` với context
- **Verify**: request → check log format là JSON với correlationId

### Task 9.2: `feat(observability): add custom metrics`
- Thêm dependency: `micrometer-registry-prometheus`
- Custom metrics:
  - `linkforge.links.created` (Counter)
  - `linkforge.redirect.count` (Counter)
  - `linkforge.redirect.latency` (Timer)
  - `linkforge.cache.hit` / `linkforge.cache.miss` (Counters)
- Inject `MeterRegistry` vào use cases
- Expose `/actuator/prometheus`
- **Verify**: tạo link + redirect → check `/actuator/prometheus` có metrics

---

## Phase 10 – Dockerization & Documentation

### Task 10.1: `chore: add Dockerfile and docker-compose`
- Tạo multi-stage `Dockerfile` (build + runtime)
- Update `docker-compose.yml`: app + PostgreSQL + Redis
- **Verify**: `docker-compose up` → app running, endpoints work

### Task 10.2: `feat: add OpenAPI/Swagger documentation`
- Thêm dependency: `springdoc-openapi-starter-webmvc-ui`
- Annotate controllers với OpenAPI annotations
- Cấu hình info (title, version, description)
- **Verify**: `/swagger-ui.html` accessible, endpoints documented

### Task 10.3: `docs: add README.md`
- Architecture overview & diagram
- Tech stack
- How to run (`docker-compose up`)
- API documentation link
- Design decisions & trade-offs
- Future evolution path
- **Verify**: README reads well, links work

---

## Tổng kết

| Phase | Số tasks | Focus |
|-------|:--------:|-------|
| 0 – Setup | 3 | Foundation |
| 1 – Domain | 6 | Business logic (thuần Java) |
| 2 – Database | 4 | Persistence layer |
| 3 – Code Gen | 1 | TSID → Base62 |
| 4 – Use Cases | 5 | Application logic |
| 5 – API | 4 | Controllers + tests |
| 6 – Cache | 4 | Redis integration |
| 7 – Click Tracking | 1 | Async events |
| 8 – Rate Limit | 2 | Anti-abuse |
| 9 – Observability | 2 | Logging + metrics |
| 10 – Deploy & Docs | 3 | Docker + Swagger + README |
| **Tổng** | **35** | |

> **Nguyên tắc**: Mỗi task phải compile + test pass trước khi commit.
> Không commit code broken. Mỗi commit là một đơn vị hoàn chỉnh.
