# PawLingo Backend — Project Overview

🐾 **Learn English, Raise a Pet** — repo: `pawlingo-api`

> This is the backend repo (Spring Boot). The frontend lives in a separate repo: `pawlingo-ui` (Next.js). This file is a condensed, backend-focused version of the product spec provided by the FE team.

---

## 1. Core idea

Most English-learning apps rely on abstract motivators (streaks, XP, leaderboards) that feel like obligations rather than something users genuinely care about. PawLingo ties **the pet's growth directly to real language progress** — the pet levels up, learns new tricks, and evolves based on vocabulary mastered and skills improved, not just coins spent.

The backend owns **all business logic, the database, and auth**. Next.js only calls the API — it has no direct DB access.

---

## 2. Backend's role in the system

- Owns **all data** — entities: User, Pet, VocabWord, Progress. The JPA entity classes + Flyway migrations in the codebase are the source of truth for exact fields/types, not this doc — the shape evolves as features land, so it isn't duplicated here to avoid drifting out of sync.
- Owns **authentication**: Email/Password + Google OAuth, issues JWT/session. The FE does not manage credentials itself.
- Exposes REST API for the FE (`pawlingo-ui`) consumed via `src/lib/api.ts`.
- Owns business rules: XP calculation, pet energy decay over time, spaced repetition, etc.

---

## 3. Tech stack

Pin versions here as they're actually adopted, so this file stays the source of truth for "what version are we on" instead of everyone re-checking `pom.xml`/`mvnw -v` each time.

| Layer | Choice | Version | Status |
|---|---|---|---|
| Language | Java | 21 (LTS) | In use |
| Framework | Spring Boot | 4.1.0 | In use |
| Build | Maven | 3.9.16 (via `mvnw`) | In use |
| Database | PostgreSQL | 17 | Target — no server provisioned yet in dev |
| ORM | Spring Data JPA / Hibernate ORM | 7.4.1.Final | In use |
| Migration | Flyway | 12.4.0 (`flyway-core` + `flyway-database-postgresql`) | In use |
| Auth (session) | Spring Security | 7.1.0 | In use (email/password only so far) |
| Auth (JWT) | jjwt | 0.12.6 | In use — Spring Security has no built-in JWT support |
| Auth (OAuth) | `spring-boot-starter-oauth2-client` | — | Not added — Google OAuth out of scope for now |
| Validation | Jakarta Bean Validation / Hibernate Validator | 9.1.0.Final | In use |
| Object mapping | Lombok | 1.18.46 | In use |
| Docs | springdoc-openapi (Swagger UI) | — | Not added yet |
| Test | JUnit Jupiter / Mockito / AssertJ | 6.0.3 / 5.23.0 / 3.27.7 | In use |
| Test | Testcontainers | — | Not added — needs Docker, not available in current dev environment |

---

## 4. Architecture

```
Next.js (pawlingo-ui)  --HTTP/JSON-->  Spring Boot (pawlingo-api)  --JPA-->  PostgreSQL
```

- Layered architecture in the backend: `Controller → Service → Repository → Entity`.
- A unified response envelope `{ success, data, error }` so it matches how the FE handles errors in `src/lib/api.ts`. The Java type is `ApiResponseDTO<T>` (`common/response`) — named `...DTO` specifically to avoid a class-name clash with springdoc-openapi's own response types once Swagger is added.
- Top level is package-by-feature (`auth`, `user`, `pet`, `vocab`, `progress`, plus `common` for cross-cutting concerns). **Within** each feature package, organize by layer: `controller/`, `service/` (interface) + `service/impl/` (`*ServiceImpl`), `repository/`, `dto/request/`, `dto/response/` — e.g. `auth/service/AuthService` (interface) + `auth/service/impl/AuthServiceImpl`. Controllers/other services depend on the interface, never the impl directly. A narrow single-caller technical utility (e.g. `auth/service/JwtService`) can stay a concrete class without an interface — the split is for business services consumed elsewhere via their interface. Full layout and naming rules: `coding-standards.md` §1–2.
- Exceptions: domain/business errors all throw one `BusinessException(ErrorCode)` (`common/exception`) rather than a class per error type — `ErrorCode` is an enum pairing each case with an `HttpStatus` and default message, so adding a new business error is a new enum constant, not a new file. `GlobalExceptionHandler` has a single handler for `BusinessException` plus separate handlers for validation errors and unexpected exceptions. Details: `coding-standards.md` §6.
- Auth: stateless JWT via Spring Security (`common/security/JwtAuthenticationFilter` + `JwtAuthenticationEntryPoint`, `auth/service/JwtService`). The entry point returns 401 through the same `ApiResponseDTO` envelope rather than Spring Security's default plain-text 401.

---

## 5. API contract (initial direction — to be refined)

- Base path: `/api/v1`
- Auth: JWT (access token, plus refresh token if needed); FE stores it via httpOnly cookie or a session bridge.
- Response envelope: `{ success: boolean, data: T | null, error: { code, message } | null }`

Priority endpoints for MVP:

| Method | Path | Status | Description |
|---|---|---|---|
| POST | `/auth/register` | **Implemented** | Register with email/password, returns JWT |
| POST | `/auth/login` | **Implemented** | Login with email/password, returns JWT |
| GET | `/auth/me` | **Implemented** | Current authenticated user (requires `Authorization: Bearer`) |
| GET/POST | `/auth/google` | Planned | Google OAuth login/callback |
| GET | `/pet` | Planned | Get the current user's pet |
| GET | `/vocab/topics/{topic}` | Planned | Get vocabulary words for a topic |
| POST | `/progress` | Planned | Record a learning result (correct/wrong) |

Auth is public on `register`/`login` only; every other endpoint requires a valid JWT by default (`common/config/SecurityConfig`).

---

## 6. Roadmap (backend portion)

**MVP (after the FE validation phase):**
- Stand up the Spring Boot backend: auth (email/password + Google OAuth) + database.
- Entities + migrations for User, Pet, VocabWord, Progress.
- API for FE integration via `src/lib/api.ts`.
- Multiple vocab topics, basic spaced repetition logic.
- Daily reminders (push/email) — could be a scheduled job or an external service integration.

**Phase 2:**
- Pronunciation scoring API (AI-based; could be a separate service or an external call — reference: ELSA Speak).
- Parent dashboard API.
- Premium tier + billing.

**Future:**
- Social features (visit friends' pets), leaderboards, seasonal events, multi-language support.

---

## 7. Current status

- Backend: `auth` module (email/password) implemented on branch `feature/authentication-email-password-mvp` — see the API contract table in §5 and the architecture notes in §4. `user`/`pet`/`vocab`/`progress` modules not started beyond the `User` entity.
- Git: `pawlingo-api` had no repo until this feature; initialized locally with a baseline commit on `main` before branching. Not yet pushed to a remote.
- Local dev needs a Postgres connection (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars) and `JWT_SECRET` set — no Docker/Testcontainers wired up yet, so the full-context Spring Boot test needs a real local Postgres to pass.
- FE (`pawlingo-ui`): in the validation MVP phase (landing page + static demo, not yet calling the backend).
- Next step for backend: see `current-feature.md`.