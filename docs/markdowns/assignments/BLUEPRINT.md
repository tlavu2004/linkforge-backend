# LinkForge – URL Shortener Service

## Detailed Plan & Task Breakdown

> **Mục tiêu**: Xây dựng dịch vụ rút gọn link ở mức *production-ready mini service*, phù hợp để demo kỹ thuật (Backend-oriented, Clean Architecture).

---

## 1. Project Overview

### 1.1 Project Name
**LinkForge**

**Tagline**: High-performance URL Shortener with Clean Architecture

### 1.2 Mục tiêu dự án
- Xây dựng dịch vụ rút gọn link theo hướng *production-ready mini system*
- Tối ưu cho hệ thống **read-heavy** (redirect traffic lớn)
- Thể hiện tư duy **Clean Architecture + System Design**
- Phù hợp để dùng cho **đồ án sinh viên / demo kỹ thuật / CV backend**

### 1.3 In-scope (bắt buộc)
- Tạo short link từ URL gốc
- Redirect short link → original URL
- Đếm số lượt click
- Cache-first strategy cho redirect
- Kiến trúc rõ ràng, dễ test, dễ mở rộng

### 1.4 Optional (nâng cao)
- Custom alias
- Expiration (link hết hạn)
- Rate limiting (IP-based)
- Analytics cơ bản

### 1.5 Out-of-scope (không làm)
- Authentication / User system (không cần cho public shortener)
- Microservices
- Kubernetes / Service Mesh
- Real-time analytics dashboard

> **Tại sao không cần auth?** Các dịch vụ rút gọn link phổ biến (TinyURL, is.gd)
> hoạt động không cần đăng nhập — ai cũng tạo link được. Chống abuse bằng
> **rate limiting theo IP** và **delete token** thay vì user system.

---

## 2. High-level Architecture

```
Client (Browser / API Consumer)
  │
  ▼
API Layer (Spring MVC + Global Exception Handler)
  │
  ├── Input Validation (Bean Validation / Custom)
  │
  ▼
Application Layer (Use Cases / Command-Query)
  │
  ├── Event Publishing (Spring Events)
  │
  ▼
Domain Layer (Entities, Value Objects, Domain Services)
  │
  ├── Domain Rules & Invariants
  │
  ▼
Infrastructure Layer
   ├── Database (PostgreSQL)
   ├── Cache (Redis - Cache-Aside)
   └── ID Generator (TSID / Snowflake-like)
```

Kiến trúc: **Modular Monolith + Clean Architecture (Hexagonal-inspired)**

> **Tại sao Hexagonal-inspired?** Ports & Adapters pattern giúp domain layer hoàn toàn
> độc lập với framework. Repository interface là Port, JPA implementation là Adapter.
> Điều này cho phép swap database engine hoặc cache provider mà không sửa business logic.

---

## 3. Module Breakdown (Modular Monolith)

```
modules/
 ├── shortlink/
 │    ├── controller/        # REST controllers
 │    ├── application/
 │    │    ├── usecase/       # Business use cases
 │    │    └── dto/
 │    ├── domain/
 │    │    ├── entity/
 │    │    ├── valueobject/
 │    │    ├── service/       # Domain services (code generator)
 │    │    └── exception/
 │    ├── repository/        # Domain repository interface
 │    └── infrastructure/    # JPA / Redis implementations
 │
 ├── analytics/              # Click tracking (async, optional)
 ├── ratelimit/              # Rate limiting module (optional)
 └── common/                 # Shared utilities, config
```

Nguyên tắc:
- Module không import chéo domain
- Domain layer **không phụ thuộc Spring**
- Có thể tách service sau này mà không rewrite logic

---

## 4. Detailed Task Plan

### Phase 0 – Project Setup

- [ ] Khởi tạo project Spring Boot
- [ ] Setup Java version (17+)
- [ ] Setup Gradle / Maven
- [ ] Cấu hình base package theo Clean Architecture
- [ ] Add common dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - validation
  - lombok

Deliverable:
- Project chạy được với endpoint test `/health`

---

### Phase 1 – Domain Design (Quan trọng)

#### 1.1 Domain Entity

- [ ] Entity `ShortLink`
  - `id: Long` – sử dụng **TSID** (Time-Sorted ID) thay vì auto-increment
  - `code: String` (short code, unique, derived from ID via Base62)
  - `originalUrl: String`
  - `createdAt: Instant`
  - `expiresAt: Instant?`
  - `clickCount: Long`
  - `isActive: Boolean` (soft delete thay vì xóa record)

> **Tại sao TSID?** Auto-increment dễ bị enumerate (tấn công brute-force dự đoán ID).
> TSID (Time-Sorted ID) vừa unique, vừa sortable, vừa không predictable.
> Short code sẽ được **encode từ TSID bằng Base62**, tránh hoàn toàn collision.

- [ ] Value Object `ShortCode`
  - Derived from TSID via Base62 encoding (7–8 ký tự)
  - Charset: `[0-9a-zA-Z]` (62 ký tự)
  - Immutable, self-validating

- [ ] Value Object `OriginalUrl`
  - Validate URL format (scheme + host required)
  - Normalize: trim whitespace, lowercase scheme & host
  - Reject dangerous schemes (javascript:, data:, etc.)

#### 1.2 Domain Rules

- [ ] Không cho phép tạo short link với URL không hợp lệ
- [ ] Short code phải unique toàn hệ thống (guaranteed by TSID → Base62)
- [ ] Không redirect nếu link đã hết hạn → return HTTP 410 Gone
- [ ] Click count không được âm (enforce tại domain)
- [ ] Soft-delete: link bị xóa vẫn giữ record, chỉ set `isActive = false`

Deliverable:
- Domain layer **không phụ thuộc Spring/JPA**, chỉ thuần Java
- Domain rule được enforce tại entity / value object
- ID generation strategy eliminates collision completely

---**

---

### Phase 2 – Database & Repository

#### 2.1 Database Design

Bảng: `short_links`

| Column | Type | Note |
|------|------|------|
| id | BIGINT | PK, TSID-generated (not auto-increment) |
| code | VARCHAR(10) | unique, indexed, Base62(id) |
| original_url | TEXT | not null |
| created_at | TIMESTAMP WITH TIME ZONE | not null, default now() |
| expires_at | TIMESTAMP WITH TIME ZONE | nullable |
| click_count | BIGINT | default 0 |
| is_active | BOOLEAN | default true |

Index:
- [ ] Unique index cho `code` (B-tree, lookup chính)
- [ ] Partial index cho `expires_at WHERE is_active = true` (cleanup job)
- [ ] Index cho `created_at` (phục vụ analytics/sorting)

> **Tại sao TIMESTAMP WITH TIME ZONE?** Tránh bug timezone khi deploy trên server
> ở timezone khác local. Luôn store UTC, convert khi hiển thị.

#### 2.2 Repository Layer

- [ ] Domain interface `ShortLinkRepository` (Port)
  - `save(ShortLink): ShortLink`
  - `findByCode(ShortCode): Optional<ShortLink>`
  - `findById(Long): Optional<ShortLink>`
  - `softDelete(Long): void`

- [ ] JPA implementation trong `infrastructure` (Adapter)
- [ ] Mapping entity ↔ domain model rõ ràng (MapStruct hoặc manual mapper)
- [ ] DB migration bằng **Flyway** (version-controlled schema)

Deliverable:
- Persist & query hoạt động ổn định
- Enforce unique constraint ở DB level
- Schema migration reproducible qua Flyway

---

---

### Phase 3 – Short Code Generation (Core Logic)

**Strategy: TSID → Base62 Encoding** (không có collision)

- [ ] Sử dụng thư viện TSID (e.g. `com.github.f4b6a3:tsid-creator`)
- [ ] Encode TSID thành Base62 string
- [ ] Charset: `[0-9a-zA-Z]` (62 ký tự)
- [ ] Kết quả: 7–8 ký tự, unique, không predictable
- [ ] **Không cần collision handling** vì TSID guaranteed unique

Pseudo-flow:
```java
long tsid = TsidCreator.getTsid().toLong();
String code = Base62.encode(tsid); // "a7Bx3Kp"
// No collision check needed – TSID is globally unique
```

> **So sánh với Random approach:**
> - Random Base62: cần check collision (DB query), retry loop, vẫn có thể fail
> - TSID → Base62: deterministic, zero collision, O(1), không cần DB roundtrip
> - Trade-off: code dài hơn 1-2 ký tự nhưng eliminate toàn bộ collision problem

**Custom Alias (Optional):**
- [ ] User có thể tự chọn alias → validate format + check uniqueness trong DB
- [ ] Custom alias và auto-generated code dùng cùng bảng, cùng constraint

Deliverable:
- Code generation deterministic, zero collision, O(1) complexity
- Dễ test, dễ giải thích trong phỏng vấn

---

### Phase 4 – Application Layer (Use Cases)

#### 4.1 Create Short Link

- [ ] Use case: `CreateShortLinkUseCase`
- [ ] Input:
  - originalUrl
  - expiresAt (optional)
- [ ] Flow:
  1. Validate input
  2. Generate short code
  3. Generate delete token (UUID, stored hashed)
  4. Persist DB
  5. Return DTO (bao gồm `deleteToken` — chỉ trả 1 lần duy nhất)

#### 4.2 Resolve Short Link

- [ ] Use case: `ResolveShortLinkUseCase`
- [ ] Input: short code
- [ ] Flow:
  1. Lookup cache
  2. Cache miss → DB
  3. Check expiration
  4. Increase click count (async)
  5. Return original URL

Deliverable:
- Application logic tập trung, test được

---

### Phase 5 – Controller Layer (API)

#### 5.1 REST Endpoints

- [ ] `POST /api/v1/links` – Tạo short link (public, no auth)
- [ ] `GET /api/v1/links/{code}` – Lấy thông tin link (public metadata)
- [ ] `DELETE /api/v1/links/{code}?token={deleteToken}` – Soft delete (xác minh bằng delete token)
- [ ] `GET /r/{code}` – Redirect (tách riêng khỏi API path)

> **API Versioning**: Prefix `/api/v1/` cho phép evolve API mà không break client cũ.
> Redirect endpoint `/r/{code}` không version vì đây là public-facing URL.

#### 5.2 Standardized API Response

```json
// POST response (chỉ lần tạo mới có deleteToken)
{
  "success": true,
  "data": {
    "code": "a7Bx3Kp",
    "originalUrl": "...",
    "shortUrl": "...",
    "deleteToken": "550e8400-e29b-41d4-a716-446655440000"
  },
  "timestamp": "2026-02-16T23:00:00Z"
}
```

> **Delete Token**: Trả về 1 lần duy nhất khi tạo link. Ai có token mới xóa được.
> Token được hash (SHA-256) trước khi lưu DB — tương tự cách lưu password.

- [ ] Wrapper class `ApiResponse<T>` cho mọi response
- [ ] Global `@RestControllerAdvice` exception handler

#### 5.3 HTTP Status Codes

- [ ] Validation error → 400 Bad Request
- [ ] Not found → 404 Not Found
- [ ] Expired → 410 Gone
- [ ] Redirect → 301 Moved Permanently (SEO-friendly, cacheable bởi browser)
- [ ] Rate limited → 429 Too Many Requests
- [ ] Internal error → 500 (generic, không leak stack trace)

> **301 vs 302:** Dùng 301 cho permanent redirect vì browser cache kết quả,
> giảm traffic về server. Nếu cần track mọi click thì dùng 302.
> → Cho phép config per-link (default: 301).

Deliverable:
- API hoạt động end-to-end với consistent response format
- Error handling centralized, không leak internal details

---

### Phase 6 – Cache (Redis)

**Strategy: Cache-Aside (Lazy Loading)**

- [ ] Setup Redis (local Docker, persistent volume)
- [ ] Cache key design:
  - `link:{code}` → serialized `ShortLink` (JSON hoặc MessagePack)
- [ ] TTL strategy:
  - Link có `expiresAt`: TTL = `expiresAt - now`
  - Link không expire: TTL = 24h (avoid stale cache forever)
- [ ] Cache population: on DB hit (lazy)
- [ ] Cache invalidation: on update/delete → xóa cache entry

```
Resolve Flow:
1. GET link:{code} from Redis
2. HIT  → return immediately
3. MISS → query DB → populate cache → return
4. DELETE/UPDATE → evict link:{code}
```

> **Tại sao Cache-Aside thay vì Write-Through?**
> - Write-through: mỗi lần write đều update cache → overhead cho create
> - Cache-aside: chỉ cache khi read, phù hợp read-heavy system
> - Trade-off: first request sau cache miss sẽ chậm hơn (cold start)

Deliverable:
- Redirect P99 latency < 10ms cho cached links
- Cache miss transparent cho caller

---

### Phase 7 – Click Tracking (Async, Non-blocking)

Mục tiêu: **redirect nhanh**, tracking không ảnh hưởng latency

- [ ] Không update DB trực tiếp trong thread redirect
- [ ] Option 1: `@Async` + update click_count
- [ ] Option 2: Accumulate in memory → batch update DB

Event data:
- shortCode
- timestamp
- ip (optional)
- userAgent (optional)

Deliverable:
- Redirect latency thấp và ổn định

---

### Phase 8 – Optional Enhancements

#### 8.1 Rate Limiting (Anti-abuse, no auth)
- [ ] Rate limit theo IP address
- [ ] Redis-based sliding window counter
- [ ] Giới hạn: ~10 links/phút/IP (configurable)
- [ ] Return `429 Too Many Requests` khi vượt limit

> **Tại sao IP-based thay vì API key?** Đơn giản, không cần user registration.
> Đủ hiệu quả cho public shortener. Nếu scale lên cần API key thì thêm sau.

#### 8.2 Custom Alias
- [ ] User nhập code thủ công
- [ ] Validate & check collision

#### 8.3 Expiration Job
- [ ] Scheduled job dọn link hết hạn

---

## 5. Testing Plan

### 5.1 Unit Tests
- [ ] Domain entities & value objects (validation, invariants)
- [ ] Use cases (mock repository, verify flow)
- [ ] Short code generation (deterministic, format validation)

### 5.2 Integration Tests
- [ ] Repository layer (Testcontainers + PostgreSQL)
- [ ] Cache layer (Testcontainers + Redis)
- [ ] Full flow: create → resolve → redirect

### 5.3 API Tests
- [ ] Controller tests (MockMvc)
- [ ] Error handling (400, 404, 410)
- [ ] Response format validation

### 5.4 Performance Tests (Optional, ấn tượng cho CV)
- [ ] Load test redirect endpoint (k6 hoặc wrk)
- [ ] Đo cache hit ratio
- [ ] Đo P50/P95/P99 latency

> **Testcontainers** thay vì H2: test trên real PostgreSQL & Redis,
> tránh behavior mismatch giữa H2 và PostgreSQL.

---

## 6. Non-functional Considerations (viết trong README)

- High read / low write system (ratio ~100:1)
- Cache-first strategy (target: >95% cache hit ratio)
- Stateless service (no server-side session)
- Horizontal scalability (share-nothing architecture)
- Clear boundary for future microservice split
- Graceful degradation: nếu Redis down → fallback to DB

---

## 7. Observability (Ấn tượng cho CV)

- [ ] **Structured Logging** (SLF4J + Logback, JSON format)
- [ ] **Health Check**: `/actuator/health` (Spring Actuator)
  - Custom health indicator cho Redis connectivity
- [ ] **Metrics** (Micrometer + Prometheus format):
  - `linkforge.redirect.count` – tổng số redirect
  - `linkforge.redirect.latency` – histogram latency
  - `linkforge.cache.hit_ratio` – cache effectiveness
  - `linkforge.links.created` – counter link tạo mới
- [ ] **Request tracing**: MDC correlation ID cho mỗi request

> Observability cho thấy production mindset – biết hệ thống đang chạy thế nào,
> không chỉ biết **nó chạy được**.

---

## 8. Deliverables Cuối

- Source code (clean, documented)
- README.md:
  - Architecture rationale & diagram
  - Trade-offs & design decisions
  - How to run (Docker Compose one-command)
  - Future evolution path
- API documentation (OpenAPI / Swagger UI)
- Dockerfile + docker-compose.yml

---

## 9. Future Evolution (Design-only)

```
Phase 1 (Current): Modular Monolith
   LinkForge (single deployable)
   ├── shortlink module
   ├── analytics module
   └── ratelimit module

Phase 2 (Scale reads): CDN + Edge
   CDN (Cloudflare/AWS CloudFront)
      ↓ cache miss
   LinkForge (origin server)

Phase 3 (Scale writes): Event-driven
   LinkForge → Message Queue → Analytics Service
```

Điều kiện để tách service:
- Redirect traffic cực lớn → thêm CDN trước, không cần refactor code
- Analytics phức tạp, real-time → tách qua message queue
- Nhiều team làm song song → module boundaries đã sẵn sàng

> Không tách microservices cho tới khi **bị buộc phải tách**.
> Modular Monolith boundaries đảm bảo việc tách sau này là mechanical, không cần rewrite.

---

## 10. Giá trị cho CV / Phỏng vấn

- **System Design**: thiết kế hệ thống read-heavy với cache-first architecture
- **Clean Architecture**: domain thuần Java, không phụ thuộc framework
- **ID Design**: TSID → Base62, giải quyết collision và security cùng lúc
- **Trade-off thinking**: 301 vs 302, cache-aside vs write-through, monolith vs microservices
- **Production mindset**: observability, graceful degradation, structured logging
- **Có lộ trình mở rộng thực tế** (CDN → Event-driven → Microservices)

**Talking points cho phỏng vấn:**
1. "Tại sao không dùng auto-increment ID?" → Security + collision avoidance
2. "Tại sao không microservices từ đầu?" → Premature optimization, monolith faster to iterate
3. "Làm sao handle 10K redirect/s?" → Redis cache, 301 browser cache, CDN layer
4. "Cache invalidation strategy?" → Cache-aside + TTL + explicit eviction on mutation

---

✅ **Kết quả cuối**: *LinkForge* – một dự án nhỏ nhưng thể hiện tư duy backend & system design ở mức production mindset, với architectural decisions có thể giải thích rõ ràng trong phỏng vấn.

