# LinkForge – Improvement Checklist

> Goal: evolve the backend into a secure, testable, observable, and maintainable product while keeping it a **modular monolith** (one Spring Boot application and one deployment unit).
>
> Branch convention: create branches from `main` with the `` prefix. Each phase should be split into small pull requests with the relevant tests.

---

## Phase 0 – Establish an engineering baseline

**Suggested branch:** `chore/engineering-baseline`

- [x] Record the baseline: Java, Maven, PostgreSQL, Redis, Docker versions, and required environment variables. See [ENGINEERING_BASELINE.md](ENGINEERING_BASELINE.md).
- [x] Run `mvn test` and record the current result in PR/CI. Current issues include stale i18n assertions, missing web-slice mocks, and integration tests that require Docker/Testcontainers. See [ENGINEERING_BASELINE.md](ENGINEERING_BASELINE.md).
- [x] Define a single error contract: domain/use cases throw **message keys** (for example, `validation.alias_invalid_chars`); the controller/exception handler resolves them by locale; tests do not assert translated text. See [ENGINEERING_BASELINE.md](ENGINEERING_BASELINE.md).
- [x] Establish PR conventions: description, executed tests, migrations (if applicable), and a security checklist for public/payment endpoint changes. See [the pull request template](../../.github/pull_request_template.md).

**Done when:** the baseline is documented and CI distinguishes unit tests from integration tests.

---

## Phase 1 – Separate `local`, `test`, and `prod` environments

**Suggested branch:** `chore/environment-profiles`

- [ ] Rename the current `dev` profile to **`local`** in configuration and documentation; use `SPRING_PROFILES_ACTIVE=local` as the developer default.
- [ ] Split shared configuration from environment-specific files:
  - [ ] `application-local.yaml`: local PostgreSQL/Redis, developer-friendly logging, no real secrets.
  - [ ] `application-test.yaml`: Testcontainers/fixtures provide dependencies; email and external HTTP are mocked or disabled.
  - [ ] `application-prod.yaml`: no credential/default-password fallbacks; all secrets come from environment variables or a secret manager.
- [ ] Add a secret-free `.env.example` for JWT, database, Redis, mail, VNPay, admin bootstrap, and frontend/backend URLs.
- [ ] Replace `ADMIN_PASSWORD:admin123` with a mandatory production variable; run the admin seeder only through an explicit bootstrap flag and emit an audit log.
- [ ] Add fail-fast validation for required configuration: a sufficiently long valid JWT secret, VNPay secret, DB/Redis credentials, and valid URLs.
- [ ] Update Docker Compose and README terminology to use `local`, never `dev`.

**Done when:** the local app starts with local values from `.env.example`; tests make no real email/VNPay request; production fails fast when required secrets are missing.

---

## Phase 2 – Add a Makefile and a consistent developer workflow

**Suggested branch:** `chore/makefile-workflow`

- [ ] Add a `Makefile` for CI/Linux shells and equivalent PowerShell documentation for Windows developers.
- [ ] Add these targets:
  - [ ] `make help` – list targets and descriptions.
  - [ ] `make local-up`, `make local-down`, `make local-logs` – manage local PostgreSQL/Redis through Docker Compose.
  - [ ] `make run` – run Spring Boot with the `local` profile.
  - [ ] `make test-unit` – run unit and web-slice tests without Docker.
  - [ ] `make test-integration` – run Testcontainers/integration tests when Docker is ready.
  - [ ] `make test`, `make verify`, and `make coverage` – complete test suite, CI checks, and JaCoCo report.
- [ ] Keep destructive data operations out of default targets. Any `clean-data` target must warn clearly and target local only.
- [ ] Fix the Maven Wrapper on Windows; `mvnw.cmd test` failed to start during the review.

**Done when:** a new developer needs only `.env`, Docker, `make local-up`, and `make test-unit` to start working.

---

## Phase 3 – Stabilize tests and measure test coverage

**Suggested branch:** `test/stabilize-and-coverage`

- [ ] Align `CreateShortLinkUseCaseTest`, `ShortCodeTest`, and `GetLinkAnalyticsUseCaseTest` with the current message-key/i18n contract. Assert exception type, message key, and behavior instead of translated text.
- [ ] Fix `@WebMvcTest` contexts by mocking/importing `MessageService`, `GlobalExceptionHandler`, and required controller dependencies.
- [ ] Classify tests through naming, tags, or Maven Failsafe:
  - [ ] Unit tests: no DB, Redis, or Docker connection.
  - [ ] Web-slice tests: MockMvc without real infrastructure.
  - [ ] Integration tests: PostgreSQL/Redis Testcontainers, only when Docker is available.
- [ ] Remove or rename `RedisCleaningUtility` so Surefire does not treat it as a test; no test suite may flush Redis outside a test container.
- [ ] Add JaCoCo and publish HTML/XML coverage reports in CI.
- [ ] Set an incremental quality gate: start with >= 70% line and >= 60% branch coverage for new/changed code, then raise it deliberately.
- [ ] Add tests for authorization, proxy-aware client IP/rate limiting, ad-token replay/expiry, cache invalidation, and payment invalid-signature/idempotency/VIP-extension paths.

**Done when:** `make test-unit` passes without Docker; `make test-integration` passes with Docker; CI stores coverage reports.

---

## Phase 4 – Harden API security and production configuration

**Suggested branch:** `security/harden-public-surface`

- [ ] Restrict actuator access: do not use `permitAll()` for `/actuator/**`; expose only required health/liveness endpoints and protect Prometheus/management with authentication or network policy.
- [ ] Remove `beans` exposure in production and set health details to `never` or `when-authorized`.
- [ ] Review public access per HTTP method. Link creation/resolution may be public by product decision, but administration, analytics, and sensitive operations require ownership/JWT or a valid delete token.
- [ ] Avoid query-string delete tokens where possible; prefer a header or body to reduce exposure through logs, browser history, and referrers.
- [ ] Trust `X-Forwarded-For`/`X-Real-IP` only from trusted proxies; centralize client-IP extraction for redirects, ad verification, and rate limiting.
- [ ] Apply dedicated rate limits to auth, link creation, ad verification, and payment callbacks; return HTTP 429 with suitable headers.
- [ ] Add API security headers: production HTTPS HSTS, `X-Content-Type-Options`, `Referrer-Policy`, and CSP where web content exists.
- [ ] Use constant-time webhook signature comparison; do not swallow crypto exceptions; replace `printStackTrace` in `VNPayUtil` with structured logging/exception handling.
- [ ] Ensure production CORS has explicit frontend origins and never mixes credentials with a wildcard origin.

**Done when:** public/protected policy tests pass, actuator exposes no sensitive bean/details, and production cannot start with default secrets.

---

## Phase 5 – Make payments reliable and auditable

**Suggested branch:** `feat/payment-ipn-reliability`

- [ ] Separate the browser redirect callback (`vnpay-return`) from a server-to-server IPN/webhook; use the IPN as the payment authority.
- [ ] Verify signature, transaction reference, amount, currency, status, and merchant data before changing a transaction.
- [ ] Implement idempotency: repeated callbacks cannot grant VIP more than once; persist processing state, timestamp, payload hash, and provider response code.
- [ ] Do not catch/log a VIP-grant failure and then continue successfully. Explicitly mark transactions for retry, failure, or manual reconciliation.
- [ ] Add reconciliation for long-running `PENDING` transactions and metrics/dashboard for paid, failed, invalid-signature, and retry states.
- [ ] Add immutable audit records for VIP/payment changes; never log hash secrets, access tokens, or payment-card data.

**Done when:** integration tests cover valid, invalid, and repeated IPN payloads; payment state remains correct even if the user never returns to the frontend.

---

## Phase 6 – Establish observability

**Suggested branch:** `feat/observability-foundation`

- [ ] Use structured JSON logs in production: UTC timestamp, level, service, environment, trace/correlation ID, method/path/status/duration. Redact Authorization, JWT, delete token, OTP, password, and VNPay secrets.
- [ ] Test correlation-ID propagation across filters, async events, and outbound requests.
- [ ] Complete Micrometer metrics: request latency/error rate, cache hit/miss, Redis/DB pool, link create/resolve, rate-limit rejection, ad verification, payment lifecycle, and async analytics failures.
- [ ] Add health groups: liveness independent of external services; readiness checks important dependencies according to deployment policy.
- [ ] Add OpenTelemetry tracing (OTLP) for HTTP, JDBC, Redis, and async work; choose a collector/backend before production enablement.
- [ ] Configure alerts for rising error rate/latency, DB or Redis outages, long-pending payments, invalid webhook signatures, and async failures.
- [ ] Write concise runbooks for correlation-ID lookup, pending-payment handling, migration rollback, Redis/cache incidents, and secret rotation.

**Done when:** an error can be traced from log to trace, and staging/production has useful dashboards and alerts.

---

## Phase 7 – Refactor into a modular monolith

**Suggested branch:** `refactor/modular-monolith`

- [ ] Keep one Spring Boot application, one database deployment, and internal transactions; **do not split into microservices** at this stage.
- [ ] Organize code around business modules that own their API/application/domain/persistence:
  - [ ] `links` – creation, resolution, QR, delete tokens, aliases, cache.
  - [ ] `identity` – users, authentication, refresh tokens, OTP, email, roles.
  - [ ] `analytics` – click events, enrichment, reports.
  - [ ] `billing` – VIP packages, payment transactions, VNPay/IPN.
  - [ ] `administration` – user/link administration and policies.
  - [ ] `platform` – security, configuration, observability, shared technical adapters.
- [ ] Use `api`, `application`, `domain`, and `infrastructure` consistently inside each module; export only necessary interfaces/DTOs across module boundaries.
- [ ] Reduce `shared` to a small shared kernel (common primitives/value types/error contract); do not place module use cases/domain rules there.
- [ ] Prevent circular dependencies. Modules call another module's public application facade/port, never its internal JPA repository/entity.
- [ ] Migrate one module at a time, beginning with `links`, then `identity`, `billing`, and `analytics`; keep HTTP APIs and schemas compatible in each PR.
- [ ] Add ArchUnit tests to enforce dependency direction and module boundaries.
- [ ] Organize Flyway migrations by module ownership while retaining one history; every migration must be backward-safe and have a rollback/forward-fix plan.

**Done when:** package ownership is clear, ArchUnit blocks cross-module infrastructure access, and refactoring does not unintentionally change APIs or schema.

---

## Phase 8 – CI/CD, releases, and operations

**Suggested branch:** `ci/cd-quality-gates`

- [ ] Add a pull-request CI pipeline: checkout, Maven cache, compile, `make test-unit`, static checks, coverage, and published test reports.
- [ ] Add a Docker/Testcontainers integration job, required before merge or in a merge queue depending on pipeline duration.
- [ ] Add dependency/security scanning, an SBOM, and vulnerability alerts.
- [ ] Add formatter/lint checks (for example, Spotless plus Checkstyle/PMD) in CI and developer workflows.
- [ ] Build a multi-stage Docker image that runs as a non-root user; pin its base image and tag it by commit SHA and release version.
- [ ] Store secrets in CI/hosting secret storage; never commit `.env`, keys, or credentials; run secret scanning on PRs.
- [ ] Deploy staging automatically after merge; require approval and migration pre-checks for production.
- [ ] Run post-deploy smoke tests for health/readiness, migrations, link creation/resolution, and a mocked payment flow; test rollback strategy.
- [ ] Publish release notes, artifact/image digest, and deployment status for traceability.
- [ ] Update `README.md` as part of every release/documentation change:
  - [ ] Describe the product scope, main features, architecture, and modular boundaries.
  - [ ] Document prerequisites and the local quick start using `make local-up`, `make run`, and `make test-unit`.
  - [ ] Document `local`, `test`, and `prod` profiles, required environment variables, and secret-management rules; never include real credentials.
  - [ ] Document API usage, authentication/JWT flow, delete-token and analytics authorization, ad-token flow, QR code behavior, and VNPay/IPN flow.
  - [ ] Link to the current OpenAPI/Swagger documentation and database/architecture diagrams.
  - [ ] Document test commands, coverage report location, integration-test Docker requirement, CI/CD gates, deployment and rollback steps.
  - [ ] Add troubleshooting for PostgreSQL/Redis startup, Flyway migration failures, missing secrets, Testcontainers, and Maven Wrapper issues.
  - [ ] Maintain a concise changelog/release history and verify all version numbers, profile names, URLs, and commands before tagging a release.

**Done when:** failing tests/quality/security gates block PRs, and deployments have traceable artifacts, smoke tests, and a clear rollback path.

---

## Recommended execution order

- [ ] Complete Phases 0–3 first to establish stable environments and tests.
- [ ] Complete Phases 4–5 before any production rollout.
- [ ] Implement Phase 6 alongside Phase 5 when a staging environment exists.
- [ ] Refactor Phase 7 one module at a time after contract tests are stable.
- [ ] Enable the full Phase 8 deployment/quality gates after the Makefile and test profiles are complete.
