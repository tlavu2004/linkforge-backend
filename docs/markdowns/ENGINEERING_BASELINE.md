# LinkForge Engineering Baseline

**Recorded:** 2026-08-02  
**Repository commit:** `8b28f34` (`Update OpenAPI documentation and PostgreSQL version in README`)  
**Application version:** `1.0.0`

This document captures the starting state for the improvements defined in [IMPROVEMENT_CHECKLIST.md](IMPROVEMENT_CHECKLIST.md). It is a baseline, not a claim that the current build is release-ready.

## Toolchain

| Component            | Baseline          |
|----------------------|-------------------|
| Java runtime         | Oracle JDK 25.0.1 |
| Target Java version  | 21 (`pom.xml`)    |
| Maven                | 3.9.11            |
| Spring Boot          | 3.5.10            |
| PostgreSQL container | 17-alpine         |
| Redis container      | redis:alpine      |
| Docker               | 28.5.1            |
| Docker Compose       | v2.40.2-desktop.1 |

## Current configuration

- The default Spring profile is currently `dev`; Phase 1 will rename it to `local` and introduce a dedicated `test` profile.
- Local infrastructure is defined in `docker-compose.yml` and consists of PostgreSQL and Redis.
- Production deployment is defined in `render.yaml` and uses `prod`.
- Required external configuration includes database, Redis, JWT, mail, frontend/backend URL, and VNPay values.
- The current configuration contains development-friendly/default values that must not be valid production fallbacks; Phase 1 and Phase 4 address this.

## Test baseline

The full Maven test suite was executed after dependencies were available. Its recorded result is:

| Metric    | Result |
|-----------|--------|
| Tests run | 195    |
| Failures  | 8      |
| Errors    | 10     |
| Skipped   | 0      |

Known causes:

- Assertion drift: several tests expect old English exception messages while the application now exposes i18n message keys.
- Web-slice context failures: controller tests do not provide a `MessageService` bean required by controllers and `GlobalExceptionHandler`.
- Testcontainers failures: repository/application-context integration tests require a Docker-capable test environment.

Notes from the baseline environment:

- A sandboxed Maven run may fail before testing because outbound access to Maven Central is restricted. This is an environment limitation, not a replacement for the recorded full-suite result above.
- `mvnw.cmd test` failed to start in the reviewed Windows environment. The Maven Wrapper requires repair in Phase 2.

## Error contract

The project uses message keys as the cross-layer error contract.

1. Domain and application code throw typed exceptions with a stable message key, such as `validation.alias_invalid_chars`.
2. `GlobalExceptionHandler` maps the exception type to the HTTP status code and resolves the key using `MessageService` and the request locale.
3. API tests assert HTTP status, response shape, and resolved message where locale behavior is under test.
4. Unit tests assert exception type, key, and behavior. They must not depend on hard-coded translated wording.
5. A changed message key is a compatibility-relevant change and requires updating the message bundles, API documentation, and affected tests in the same pull request.

## Pull request requirements

- Use a focused branch with the `codex/` prefix and a Conventional Commit-style title.
- Include a concise description, scope, and risk/compatibility notes.
- Run the applicable Make/Maven test commands and report the result.
- For schema changes, include a new Flyway migration; never edit an applied migration.
- For public API, authentication, payment, or secret changes, complete the security section in the pull request template.
- Update README, OpenAPI, and release notes whenever public behavior, configuration, or version changes.
