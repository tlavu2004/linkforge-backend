# LinkForge – Task Breakdown (Commit-level)

> Mỗi task = 1 commit. Thứ tự từ trên xuống dưới.
> Convention: `type(scope): message` (Conventional Commits)

---

## Phase 0 – Project Setup

### Task 0.1: `init: initialize Spring Boot project` [x]
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
- **Verify**: `mvn spring-boot:run` → app start thành công [x]

### Task 0.2: `chore: add core dependencies and project structure` [x]
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
- **Verify**: project compile thành công [x]

### Task 0.3: `feat: add health check endpoint` [x]
- Tạo `GET /health` endpoint (hoặc dùng Spring Actuator)
- Thêm `spring-boot-starter-actuator` dependency
- Cấu hình expose `/actuator/health`
- **Verify**: `GET /actuator/health` → `{"status": "UP"}` [x]

---

## Phase 1 – Domain Layer

### Task 1.1: `feat(domain): add OriginalUrl value object` [x]
- Tạo `OriginalUrl` value object (thuần Java, không Spring)
- Validation: scheme + host required (`http://` hoặc `https://`)
- Normalize: trim whitespace, lowercase scheme & host
- Reject dangerous schemes: `javascript:`, `data:`, `file:`
- Custom exception: `InvalidUrlException`
- **Unit test**: valid URLs, invalid URLs, normalization, dangerous schemes [x]
- **Verify**: `mvn test` pass [x]

### Task 1.2: `feat(domain): add ShortCode value object` [x]
- Tạo `ShortCode` value object (thuần Java)
- Validation: non-null, non-blank, chỉ chứa `[0-9a-zA-Z]`
- Immutable (final field, no setter)
- Factory method `ShortCode.of(String)`
- Custom exception: `InvalidShortCodeException`
- **Unit test**: valid codes, invalid codes (special chars, empty, too long) [x]
- **Verify**: `mvn test` pass [x]

### Task 1.3: `feat(domain): add Base62 encoder utility` [x]
- Tạo `Base62` utility class trong `common/`
- `encode(long): String` — convert long → Base62 string
- `decode(String): long` — convert Base62 string → long
- Charset: `0-9a-zA-Z` (62 ký tự)
- **Unit test**: encode/decode roundtrip, known values, edge cases (0, Long.MAX_VALUE) [x]
- **Verify**: `mvn test` pass [x]

### Task 1.4: `feat(domain): add ShortLink entity` [x]
- Tạo `ShortLink` entity (thuần Java, không JPA annotations)
- Fields: `id`, `code` (ShortCode), `originalUrl` (OriginalUrl), `createdAt`, `expiresAt`, `clickCount`, `userId`, `deleteTokenHash`
- Domain methods:
  - `isExpired(): boolean`
  - `incrementClickCount(): void`
  - `matchesDeleteToken(String rawToken): boolean`
- Enforce invariants: clickCount >= 0, originalUrl not null
- **Unit test**: tạo entity, check expiration, increment click, delete token matching [x]
- **Verify**: `mvn test` pass [x]

### Task 1.5: `feat(domain): add domain exceptions` [x]
- Tạo package `domain/exception/`:
  - `ShortLinkNotFoundException`
  - `ShortLinkExpiredException`
  - `InvalidUrlException`
  - `InvalidShortCodeException`
  - `InvalidDeleteTokenException`
- Tất cả extend từ base `DomainException`
- **Verify**: project compile thành công [x]

### Task 1.6: `feat(domain): add ShortLinkRepository port interface` [x]
- Tạo `ShortLinkRepository` interface trong `repository/` (domain port)
  - `save(ShortLink): ShortLink`
  - `findByCode(ShortCode): Optional<ShortLink>`
  - `findById(Long): Optional<ShortLink>`
- Interface thuần Java, không Spring annotation
- **Verify**: project compile thành công [x]

---

## Phase 2 – Database & Infrastructure

### Task 2.1: `feat(infra): add PostgreSQL and Flyway configuration` [x]
- Thêm dependencies: `postgresql`, `flyway-core`
- Cấu hình `application.yml` (Profiles):
  - Dev: Local Docker PostgreSQL
  - Prod: NeonDB (via ENV vars)
  - Flyway migration settings
  - JPA/Hibernate settings (ddl-auto: validate)
- Tạo `docker-compose.yml` cho PostgreSQL
- **Verify**: `docker-compose up -d` → PostgreSQL running [x]

### Task 2.2: `feat(infra): add Flyway migration for short_links table` [x]
- Tạo migration `V1__create_short_links_table.sql`:
- **Verify**: app start → Flyway migration applied → table exists [x]

### Task 2.3: `feat(infra): add JPA entity and repository adapter` [x]
- Tạo `ShortLinkJpaEntity`
- Tạo `ShortLinkJpaRepository`
- Tạo `ShortLinkMapper`
- Tạo `ShortLinkRepositoryAdapter`
- **Verify**: project compile thành công [x]

### Task 2.4: `test(infra): add repository integration tests` [x]
- Integration test cho `ShortLinkRepositoryAdapter`
- **Verify**: `mvn test` pass [x]

---

## Phase 3 – Short Code Generation

### Task 3.1: `feat(domain): add TSID-based short code generator` [x]
- Tạo `ShortCodeGenerator` interface
- Tạo `TsidShortCodeGenerator` implementation:
  - Dùng `tsid-creator` library
  - Generate TSID → Base62 encode → ShortCode
- Đăng ký Spring bean
- **Unit test**: generate 1000 codes → tất cả unique, format hợp lệ [x]
- **Verify**: `mvn test` pass [x]

---

## Phase 4 – Application Layer (Use Cases)

### Task 4.1: `feat(app): add ApiResponse wrapper and DTOs` [x]
- Tạo `ApiResponse<T>` wrapper class
- Tạo `CreateShortLinkRequest`, `ShortLinkResponse`, `CreateShortLinkResponse`
- **Verify**: project compile thành công [x]

### Task 4.2: `feat(app): add CreateShortLinkUseCase` [x]
- Tạo `CreateShortLinkUseCase` logic
- **Unit test**: mock repository & generator [x]
- **Verify**: `mvn test` pass [x]

### Task 4.3: `feat(app): add ResolveShortLinkUseCase` [x]
- Tạo `ResolveShortLinkUseCase` logic
- **Unit test**: mock repository, test scenarios [x]
- **Verify**: `mvn test` pass [x]

---

## Phase 5 – Controller Layer (Core API)

### Task 5.1: `feat(api): add global exception handler` [x]
- Tạo `GlobalExceptionHandler`
- Map exceptions → HTTP status (400, 404, 410, 403, 500)
- **Verify**: project compile thành công [x]

### Task 5.2: `feat(api): add ShortLinkController (Create endpoint)` [x]
- Tao `ShortLinkController` - `POST /api/v1/links`
- **Verify**: app start, test endpoint [x]

### Task 5.3: `feat(api): add RedirectController` [x]
- Tạo `RedirectController` - `GET /r/{code}` → 301
- **Verify**: test redirect [x]

### Task 5.4: `test(api): add controller integration tests` [x]
- MockMvc tests cho các core endpoints
- **Verify**: `mvn test` pass [x]

---

## Phase 6 – Management Features (Delete & Get Info)

### Task 6.1: `feat(app): add DeleteShortLinkUseCase` [x]
- Tạo `DeleteShortLinkUseCase`
- **Unit test**: verify token logic [x]
- **Verify**: `mvn test` pass [x]

### Task 6.2: `feat(app): add GetShortLinkUseCase` [x]
- Tạo `GetShortLinkUseCase`
- **Unit test**: happy path [x]
- **Verify**: `mvn test` pass [x]

### Task 6.3: `feat(api): add Delete & Get endpoints to ShortLinkController` [x]
- Update `ShortLinkController`: `GET` & `DELETE`
- **Verify**: API test [x]

---

## Phase 7 – Cache (Redis)

### Task 7.1: `feat(infra): add Redis configuration` [x]
- Add Redis to `docker-compose.yml`, config `RedisTemplate`
- **Verify**: connection OK [x]

### Task 7.2: `feat(infra): add ShortLinkCacheService` [x]
- Tạo `ShortLinkCacheService` (get, put, evict)
- **Unit test**: verify format & TTL [x]
- **Verify**: `mvn test` pass [x]

### Task 7.3: `feat(app): integrate cache into ResolveShortLinkUseCase` [x]
- Update Resolve use case (L2 Cache Aside)
- Update Delete use case (Evict cache)
- **Verify**: `mvn test` pass [x]

### Task 7.4: `feat(infra): add Redis health indicator` [x]
- Register Redis HealthIndicator in Actuator
- **Verify**: health check status [x]

---

## Phase 8 – Click Tracking (Async) [x]

### Task 8.1: `feat(app): add async click tracking with Spring Events` [x]
- Tạo `ClickEvent`, `ClickEventPublisher`, `ClickEventListener` (`@Async`)
- **Verify**: tạo link → redirect → check click_count tăng [x]

---

## Phase 9 – Rate Limiting [x]

### Task 9.1: `feat(ratelimit): add IP-based rate limiter` [x]
- Tạo `RateLimiterService` (Redis LUA script)
- **Verify**: `mvn test` pass [x]

### Task 9.2: `feat(ratelimit): add rate limit filter` [x]
- Tạo `RateLimitFilter` - apply cho create endpoints
- **Verify**: manual test with curl [x]

---

## Phase 10 – Observability [x]

### Task 10.1: `feat(observability): add structured logging and correlation ID` [x]
- Logback JSON format, `CorrelationIdFilter`
- **Verify**: logs check [x]

### Task 10.2: `feat(observability): add custom metrics` [x]
- Counters & Timers for redirect, create, cache hit/miss
- **Verify**: `/actuator/prometheus` metrics [x]

---

## Phase 11 – Dockerization & Swagger [x]

### Task 11.1: `chore: add Dockerfile and docker-compose` [x]
- Multi-stage Dockerfile
- **Verify**: `docker-compose up` OK [x]

### Task 11.2: `feat: add OpenAPI/Swagger documentation` [x]
- `springdoc-openapi`, annotate controllers
- **Verify**: `/swagger-ui.html` documented [x]

### Task 11.3: `docs: update README.md` [x]
- Architecture overview & diagram
- Tech stack
- How to run (`docker-compose up`)
- API documentation link
- Design decisions & trade-offs
- Future evolution path
- **Verify**: README reads well, links work [x]

---

## Phase 12 – User Authentication & Authorization [x]
### Task 12.1: `feat(auth): User entity, Role enum and security` [x]
- Spring Security, JWT, `User` entity, `V2` migration
- **Verify**: security config OK [x]

### Task 12.2: `feat(auth): register and login endpoints (JWT)` [x]
- AuthUseCase, Register/Login API
- **Verify**: auth test [x]

### Task 12.3: `feat(auth): admin VIP management endpoints` [x]
- `ToggleVipStatusUseCase`, Admin filters
- **Verify**: admin API test [x]

---

## Phase 13 – Payment Gateways [x]
### Task 13.1: `feat(payment): core payment services` [x]
- `PaymentTransaction` entity, `IPaymentGateway` interface
- **Verify**: base payment layer OK [x]

### Task 13.2: `feat(payment): integrate VNPay` [x]
- Checkout & Webhook/IPN
- **Verify**: VNPay test [x]

### Task 13.3: `feat(payment): integrate PayOS` [ ]
- Checkout & Webhook
- **Verify**: PayOS test [ ]

### Task 13.4: `feat(payment): integrate PayPal` [ ]
- Checkout & Webhook
- **Verify**: PayPal test [ ]

### Task 13.5: `feat(payment): integrate SEPay` [ ]
- Webhook recognition
- **Verify**: SEPay test [ ]

---

## Phase 14 – Documentation & Final Polish [x]
### Task 14.1: `docs: update README and documents` [x]
- Walkthrough, Implementation Plan, Blueprint updates
- **Verify**: docs consistent [x]

---

## Phase 15 – System Improvements (Scale & Analytics) [x]

### Task 15.1: `feat(infra): add PostgreSQL table partitioning` [x]
- Monthly partitioning for `short_links`
- **Verify**: EXPLAIN plan confirms pruning [x]

### Task 15.2: `feat(infra): add read replica support` [x]
- Dual datasource support (NeonDB focus)
- **Verify**: read query distribution [x]

### Task 15.3: `feat(infra): add CDN configuration` [x]
- Cache-Control headers, CDN edge rules
- **Verify**: Cache headers present [x]

### Task 15.4: `feat(analytics): add detailed click tracking` [x]
- Geo, Device, Referrer tracking
- **Verify**: analytics records created [x]

### Task 15.5: `feat(api): add analytics API endpoints` [x]
- Stats for overview, geo, devices, clicks
- **Verify**: API response valid [x]

### Task 15.6: `feat(cache): add local cache layer for hot keys` [x]
- L1 Caffeine Cache integration
- **Verify**: reduced Redis calls for hot keys [x]

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

## Phase 16 – Custom Alias [x]

### Task 16.1: `feat(app): customAlias to CreateShortLinkRequest` [x]
- Update DTO [x]

### Task 16.2: `feat(domain): alias validation logic` [x]
- Length, regex, reserved words blocking
- **Verify**: validation tests pass [x]

### Task 16.3: `feat(infra): shortCode uniqueness check` [x]
- Repository update & UseCase integration
- **Verify**: alias taken scenarios [x]

### Task 16.4: `feat(frontend): custom alias field` [x]
- Dashboard UI update
- **Verify**: UI functional [x]

### Task 16.5: `feat(frontend): handle alias API errors` [x]
- Localized error messages for alias [x]

---

> **Nguyên tắc**: Mỗi task phải compile + test pass trước khi commit.

> Không commit code broken. Mỗi commit là một đơn vị hoàn chỉnh.
