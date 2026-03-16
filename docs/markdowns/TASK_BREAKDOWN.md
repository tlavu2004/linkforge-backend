# LinkForge – Task Breakdown (Commit-level)

> Mỗi task = 1 commit. Thứ tự từ trên xuống dưới.
> Convention: `type(scope): message` (Conventional Commits)

---

## Phase 0 – Project Setup

### Task 0.1: `init: initialize Spring Boot project`
- Khởi tạo project Spring Boot (Spring Initializr: start.spring.io)
  - **Project**: Maven / Java 17+
  - **Group**: `com.linkforge` / **Artifact**: `linkforge-backend`
  - **Dependencies**:
    - `Spring Web`
    - `Spring Data JPA`
    - `Validation`
    - `Spring Boot Actuator`
    - `Spring Data Redis (Access + Driver)`
    - `PostgreSQL Driver`
    - `Flyway Migration`
    - `Lombok`
- Manual dependencies (thêm sau vào `pom.xml`):
  - `io.hypersistence:tsid` (Base62/TSID)
  - `springdoc-openapi-starter-webmvc-ui` (Swagger)
  - `micrometer-registry-prometheus` (Metrics)
  - `org.testcontainers:postgresql` (Testing)
- Cấu hình `application.yml` cơ bản (server.port, spring.application.name)
- **Verify**: `mvn spring-boot:run` → app start thành công

### Task 0.2: `chore: add core dependencies and project structure`
- Thêm dependencies: `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `lombok`, `tsid-creator`
- Tạo package structure theo Clean Architecture (4 layers):
  ```
  com.tlavu.linkforge/
    ├── domain/            # Enterprise Business Rules
    │    ├── entity/
    │    ├── valueobject/
    │    ├── service/      # Domain Services
    │    ├── exception/
    │    └── repository/   # Repository Interfaces (Ports)
    ├── application/       # Application Business Rules
    │    ├── usecase/
    │    └── dto/
    ├── infrastructure/    # Frameworks & Drivers
    │    ├── persistence/  # JPA Entities & Repositories
    │    ├── config/
    │    └── adapter/
    ├── presentation/      # Interface Adapters
    │    ├── controller/
    │    └── request/response/
    ├── shared/            # Common/Shared Kernel
    │    └── util/
    └── LinkforgeApplication.java
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
- Fields: `id`, `code` (ShortCode), `originalUrl` (OriginalUrl), `createdAt`, `expiresAt`, `clickCount`, `userId`, `deleteTokenHash`
- Domain methods:
  - `isExpired(): boolean`
  - `incrementClickCount(): void`
  - `matchesDeleteToken(String rawToken): boolean`
- Enforce invariants: clickCount >= 0, originalUrl not null
- **Unit test**: tạo entity, check expiration, increment click, delete token matching
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
- Cấu hình `application.yml` (Profiles):
  - Dev: Local Docker PostgreSQL
  - Prod: NeonDB (via ENV vars)
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
    user_id BIGINT,
    delete_token_hash VARCHAR(64),
    CONSTRAINT uk_short_links_code UNIQUE (code)
  );
  CREATE INDEX idx_short_links_created_at ON short_links (created_at);
  CREATE INDEX idx_short_links_expires_at ON short_links (expires_at);
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
- Chưa có cache (thêm ở Phase 7)
- **Unit test**: mock repository, test happy path, not found, expired, inactive
- **Verify**: `mvn test` pass

---

## Phase 5 – Controller Layer (Core API)

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

### Task 5.2: `feat(api): add ShortLinkController (Create endpoint)`
- Tạo `ShortLinkController`:
  - `POST /api/v1/links` → `CreateShortLinkUseCase`
  - (Chưa có GET/DELETE, sẽ thêm ở Phase 6)
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
- MockMvc tests cho các core endpoints:
  - POST create → 201, verify response format + deleteToken present
  - GET redirect → 301 + Location
  - GET not found → 404
  - POST invalid URL → 400
- **Verify**: `mvn test` pass

---

## Phase 6 – Management Features (Delete & Get Info)

### Task 6.1: `feat(app): add DeleteShortLinkUseCase`
- Tạo `DeleteShortLinkUseCase`:
  1. Lookup by code via repository
  2. Verify delete token (hash & compare)
  3. Gọi `repository.delete(id)` → hard delete
- **Unit test**: valid token, invalid token, link not found
- **Verify**: `mvn test` pass

### Task 6.2: `feat(app): add GetShortLinkUseCase`
- Tạo `GetShortLinkUseCase`:
  1. Lookup by code
  2. Check expiration (optional)
  3. Return metadata DTO (không trả deleteToken)
- **Unit test**: happy path, not found
- **Verify**: `mvn test` pass

### Task 6.3: `feat(api): add Delete & Get endpoints to ShortLinkController`
- Update `ShortLinkController`:
  - `GET /api/v1/links/{code}` → `GetShortLinkUseCase`
  - `DELETE /api/v1/links/{code}?token={deleteToken}` → `DeleteShortLinkUseCase`
- **Verify**: app start, test bằng curl/Postman

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

## Phase 11 – Dockerization & Swagger

### Task 11.1: `chore: add Dockerfile and docker-compose`
- Tạo multi-stage `Dockerfile` (optimized for Render deployment)
- Update `docker-compose.yml`: app + PostgreSQL + Redis (Local Dev environment)
- **Verify**: `docker-compose up` → app running, endpoints work

### Task 11.2: `feat: add OpenAPI/Swagger documentation`
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

## Phase 12 – User Authentication & Authorization
### Task 12.1: `feat(auth): setup User entity, Role enum and security`
- Thêm dependency Spring Security, JWT (e.g., jjwt).
- Tạo Enum `Role` (USER, ADMIN).
- Tạo `User` entity (id, email, password_hash, role, is_vip, vip_expires_at).
- Implement VIP logic methods trong `User` (grantLifetimeVip, revokeVip, vv).
- Tạo DB migration `V2__create_users_table.sql`.
- Cấu hình SecurityFilterChain, PasswordEncoder.

### Task 12.2: `feat(auth): add register and login endpoints (JWT)`
- Implement `AuthUseCase` (register, login, generate JWT token).
- Cấu hình `JwtAuthenticationFilter` để bảo vệ route và parse Roles.
- API: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`.

### Task 12.3: `feat(auth): add admin VIP management endpoints`
- Implement `ToggleVipStatusUseCase` (tìm User, thay đổi trạng thái isVip tĩnh viễn, lưu DB).
- Cấu hình Spring Security để hạn chế dải URL `/api/v1/admin/**` chỉ cho role `ADMIN`.
- API: `POST /api/v1/admin/users/{userId}/vip/toggle`

---

## Phase 13 – Payment Gateways
### Task 13.1: `feat(payment): implement core payment services`
- Cấu hình các config keys cho VNPay, PayPal, SEPay, PayOS.
- Mở rộng domain model với `PaymentTransaction`.
- Tạo `IPaymentGateway` interface (generateUrl, processCallback).

### Task 13.2: `feat(payment): integrate VNPay`
- Map config sang VNPay SDK/API.
- Implement checkout logic và tạo URL thanh toán.
- Xử lý VNPay IPN/Callback để cấp VIP.

### Task 13.3: `feat(payment): integrate PayOS`
- Map config sang PayOS SDK/API.
- Implement checkout logic và tạo URL thanh toán (VietQR).
- Xử lý PayOS Webhook để cấp VIP.

### Task 13.4: `feat(payment): integrate PayPal`
- Map config sang PayPal API/SDK.
- Implement checkout logic.
- Xử lý PayPal Webhook.

### Task 13.5: `feat(payment): integrate SEPay`
- Map config sang SEPay API.
- Cấu hình nhận diện giao dịch chuyển khoản.
- Xử lý SEPay Webhook rẽ nhánh cấp VIP.

---

## Phase 14 – Documentation & Final Polish
### Task 14.1: `docs: add README.md`
- Architecture overview & diagram
- Tech stack
- How to run (`docker-compose up`)
- API documentation link
- Design decisions & trade-offs
- Future evolution path
- **Verify**: README reads well, links work

---

## Phase 15 – System Improvements (Scale & Analytics)

> Dựa trên phân tích GAP với yêu cầu từ ANALYTIC.md

### Task 15.1: `feat(infra): add PostgreSQL table partitioning`
- Tạo migration mới: partition `short_links` theo `created_at` (monthly)
- Cấu hình `HikariCP` pool size tối ưu cho read-heavy workload
- Tạo script tự động tạo partition mới hàng tháng (cron hoặc scheduled job)
- **Verify**: Query explain plan cho thấy partition pruning hoạt động

### Task 15.2: `feat(infra): add read replica support`
- Cấu hình Spring Boot dual datasource (write → primary, read → replica)
- Tạo `@ReadOnlyTransaction` annotation tự chọn replica datasource
- Apply cho `ResolveShortLinkUseCase` và các read queries
- **Verify**: Redirect queries đi qua read replica, write vẫn qua primary

### Task 15.3: `feat(infra): add CDN configuration`
- Thêm `Cache-Control` headers cho redirect response (public, max-age=86400)
- Cấu hình Cloudflare / CloudFront trước origin server
- Setup page rules: cache `/r/*` path
- **Verify**: CDN cache HIT headers xuất hiện trong response

### Task 15.4: `feat(analytics): add detailed click tracking`
- Tạo `ClickAnalytics` domain entity:
  - `click_id`, `short_code`, `timestamp`, `ip_address`, `user_agent`
  - `country`, `city`, `device_type`, `referrer`
- Tạo migration `V5__create_click_analytics_table.sql`
- Tích hợp IP geolocation (MaxMind GeoIP2 hoặc ip-api.com)
- Parse User-Agent → device type (mobile/desktop/tablet)
- Implement batch insert (gom events → flush mỗi 5s hoặc 100 records)
- **Unit test**: verify parsing, geolocation fallback
- **Verify**: Click tạo record analytics với đầy đủ metadata

### Task 15.5: `feat(api): add analytics API endpoints`
- `GET /api/v1/links/{code}/analytics` — tổng quan (total clicks, unique visitors)
- `GET /api/v1/links/{code}/analytics/clicks?from=&to=` — time-series
- `GET /api/v1/links/{code}/analytics/geo` — phân bổ theo quốc gia/thành phố
- `GET /api/v1/links/{code}/analytics/devices` — phân bổ thiết bị
- Chỉ cho phép owner hoặc admin xem analytics (authorization check)
- **Verify**: API trả dữ liệu đúng format, pagination hoạt động

### Task 15.6: `feat(cache): add local cache layer for hot keys`
- Thêm dependency: `com.github.ben-manes.caffeine:caffeine`
- Implement L1 cache (Caffeine) trước L2 cache (Redis):
  - L1: in-memory, TTL 30–60s, max 10,000 entries
  - L2: Redis, TTL 24h (giữ nguyên)
- Update `ResolveShortLinkUseCase`: check L1 → L2 → DB
- **Verify**: Hot key served từ L1 cache, metrics cho thấy giảm Redis calls

---

## Tổng kết

| Phase | Số tasks | Focus |
|-------|:--------:|-------|
| 0 – Setup | 3 | Foundation |
| 1 – Domain | 6 | Business logic (thuần Java) |
| 2 – Database | 4 | Persistence layer |
| 3 – Code Gen | 1 | TSID → Base62 |
| 4 – Use Cases | 3 | Application logic |
| 5 – API | 4 | Controllers + tests |
| 6 – Management | 3 | Delete & Get Info |
| 7 – Cache | 4 | Redis integration |
| 8 – Click Tracking | 1 | Async events |
| 9 – Rate Limit | 2 | Anti-abuse |
| 10 – Observability | 2 | Logging + metrics |
| 11 – Dockerization | 2 | Docker + Swagger |
| 12 – Auth | 2 | Security & User Entity |
| 13 – Payments | 5 | VNPay, PayOS, PayPal, SEPay |
| 14 – Docs | 1 | README |
| 15 – Improvements | 6 | DB Sharding, CDN, Analytics, Hot Key Cache |
| 16 – Custom Alias | 5 | Uniqueness, Reserved words, Frontend UI |
| **Tổng** | **54** | |

---

## Phase 16 – Custom Alias

### Task 16.1: `feat(app): add customAlias to CreateShortLinkRequest`
- Update `CreateShortLinkRequest` DTO: add `String customAlias` field.

### Task 16.2: `feat(domain): implement alias validation logic`
- Create `AliasValidator` or add logic to `CreateShortLinkUseCase`:
  - Check length (3-30), regex (`^[a-zA-Z0-9-_]+$`).
  - Block reserved words: `admin`, `api`, `dashboard`, `login`, `register`, `static`, `assets`, etc.

### Task 16.3: `feat(infra): add shortCode uniqueness check`
- Ensure `ShortLinkRepository` has `existsByCode(ShortCode)` or equivalent.
- Update `CreateShortLinkUseCase` to throw `AliasAlreadyTakenException` if custom alias exists.

### Task 16.4: `feat(frontend): add custom alias field to dashboard`
- Add input field (optional) in the create link form.
- Pass `customAlias` to API request.

### Task 16.5: `feat(frontend): handle alias API errors`
- Display user-friendly messages for "Alias taken" or "Reserved word used".

---

> **Nguyên tắc**: Mỗi task phải compile + test pass trước khi commit.

> Không commit code broken. Mỗi commit là một đơn vị hoàn chỉnh.
