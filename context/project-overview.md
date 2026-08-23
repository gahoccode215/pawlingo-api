# PawLingo Backend — Project Overview

🐾 **Learn English, Raise a Pet** — repo: `pawlingo-api`

> This is the backend repo (Spring Boot). The frontend lives in a separate repo: `pawlingo-ui` (Next.js). This file is a condensed, backend-focused version of the product spec provided by the FE team.

---

## 1. Core idea

Most English-learning apps rely on abstract motivators (streaks, XP, leaderboards) that feel like obligations rather than something users genuinely care about. PawLingo ties **the pet's growth directly to real language progress** — the pet levels up, learns new tricks, and evolves based on vocabulary mastered and skills improved, not just coins spent.

The backend owns **all business logic, the database, and auth**. Next.js only calls the API — it has no direct DB access.

---

## 2. Backend's role in the system

- Owns **all data** — entities currently in the codebase: User. `Pet`, `Vocabulary`, and `Progress` were removed (see §7) and will return once rebuilt with a new design after auth. The JPA entity classes + Flyway migrations in the codebase are the source of truth for exact fields/types, not this doc — the shape evolves as features land, so it isn't duplicated here to avoid drifting out of sync.
- Owns **authentication**: Email/Password + Google OAuth (ID token verification), issues JWT. The FE does not manage credentials itself.
- Exposes REST API for the FE (`pawlingo-ui`) consumed via `src/lib/api.ts`.
- Owns business rules: XP calculation, spaced repetition, etc.

---

## 3. Tech stack

Pin versions here as they're actually adopted, so this file stays the source of truth for "what version are we on" instead of everyone re-checking `pom.xml`/`mvnw -v` each time.

| Layer | Choice | Version | Status |
|---|---|---|---|
| Language | Java | 21 (LTS) | In use |
| Framework | Spring Boot | 4.1.0 | In use |
| Build | Maven | 3.9.16 (via `mvnw`) | In use |
| Database | PostgreSQL | 17 | In use — Neon-hosted Postgres, connection in `.env` (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`) |
| ORM | Spring Data JPA / Hibernate ORM | 7.4.1.Final | In use |
| Migration | Flyway | 12.4.0 (`flyway-core` + `flyway-database-postgresql`) | In use |
| Auth (session) | Spring Security | 7.1.0 | In use |
| Auth (JWT) | jjwt | 0.12.6 | In use — Spring Security has no built-in JWT support |
| Auth (OAuth) | `google-api-client` | 2.9.0 | In use — verifies Google ID tokens server-side (no `spring-boot-starter-oauth2-client`) |
| Validation | Jakarta Bean Validation / Hibernate Validator | 9.1.0.Final | In use |
| Object mapping | Lombok | 1.18.46 | In use |
| Docs | springdoc-openapi (Swagger UI) | 3.1.0 | In use — UI at `/swagger-ui.html`, JSON at `/v3/api-docs` |
| Test | JUnit Jupiter / Mockito / AssertJ | 6.0.3 / 5.23.0 / 3.27.7 | In use |
| Test | Testcontainers | — | Not added — needs Docker, not available in current dev environment |

---

## 4. Architecture

```
Next.js (pawlingo-ui)  --HTTP/JSON-->  Spring Boot (pawlingo-api)  --JPA-->  PostgreSQL
```

- Layered architecture in the backend: `Controller → Service → Repository → Entity`.
- A unified response envelope `{ success, data, error }` so it matches how the FE handles errors in `src/lib/api.ts`. The Java type is `ApiResponseDTO<T>` (`common/response`) — named `...DTO` specifically to avoid a class-name clash with springdoc-openapi's own response types once Swagger is added.
- Top level is package-by-feature (`auth`, `user`, plus `common` for cross-cutting concerns — `pet`/`vocab`/`progress` removed for now, see §7). **Within** each feature package, organize by layer: `controller/`, `service/` (interface) + `service/impl/` (`*ServiceImpl`), `repository/`, `dto/request/`, `dto/response/` — e.g. `auth/service/AuthService` (interface) + `auth/service/impl/AuthServiceImpl`. Controllers/other services depend on the interface, never the impl directly. A narrow single-caller technical utility (e.g. `auth/service/JwtService`) can stay a concrete class without an interface — the split is for business services consumed elsewhere via their interface. Full layout and naming rules: `coding-standards.md` §1–2.
- Exceptions: domain/business errors all throw one `BusinessException(ErrorCode)` (`common/exception`) rather than a class per error type — `ErrorCode` is an enum pairing each case with an `HttpStatus` and default message, so adding a new business error is a new enum constant, not a new file. `GlobalExceptionHandler` has a single handler for `BusinessException` plus separate handlers for validation errors and unexpected exceptions. Details: `coding-standards.md` §6.
- Auth: stateless JWT via Spring Security (`common/security/JwtAuthenticationFilter` + `JwtAuthenticationEntryPoint`, `auth/service/JwtService`). The entry point returns 401 through the same `ApiResponseDTO` envelope rather than Spring Security's default plain-text 401.

---

## 5. API contract

- Base path: `/api/v1`
- Auth: JWT access token (`Authorization: Bearer <token>`, 15 min lifetime) + a rotating opaque refresh token (30 days, `POST /auth/refresh` to renew).
- Response envelope: `{ success: boolean, data: T | null, error: { code, message } | null }`
- Three sources, three jobs — don't re-derive one from another by hand:
  - **This table** — current state of every endpoint. Read this to see what exists *right now*.
  - **`docs/api-changelog.md`** — history of what changed *and why*, one dated entry per completed feature (auto-populated by `/feature complete`). Read this to catch up after time away, without reading git log or code.
  - **Live OpenAPI spec** (`/v3/api-docs`, Swagger UI at `/swagger-ui.html`) — exact current field-level shapes, generated from code so it can't drift. Point `openapi-typescript` (or similar) at it to generate FE types instead of hand-reading DTOs/Java.

| Method | Path | Status | Description |
|---|---|---|---|
| POST | `/auth/register` | **Implemented** | Register with email/password; returns `{ accessToken, refreshToken, expiresIn }` |
| POST | `/auth/login` | **Implemented** | Login with email/password; returns `{ accessToken, refreshToken, expiresIn }` |
| POST | `/auth/google` | **Implemented** | Login or register via a Google ID token; returns `{ accessToken, refreshToken, expiresIn, isNewUser }` |
| POST | `/auth/refresh` | **Implemented** | Exchange a refresh token for a new access+refresh pair; rotates on every call |
| POST | `/auth/logout` | **Implemented** | Revoke a single refresh token; public — works even with an expired access token |
| GET | `/auth/me` | **Implemented** | Current authenticated user's basic profile — `{ id, email, goal, authProvider, createdAt }` |
| GET | `/users` | **Implemented (debug-only)** | List all users, no pagination — see security note below |

`/pet`, `/vocabularies`, `/vocabularies/{id}`, and `/progress` existed on `main` but were removed (see §7) — pet will reappear with a new design; vocab/progress will reappear once rebuilt.

Auth is public on `register`/`login`/`google`/`refresh`/`logout` only; every other endpoint requires a valid JWT by default (`common/config/SecurityConfig`).

**⚠ Security note:** `GET /users` is currently in `SecurityConfig`'s whitelist and requires no auth at all, exposing every user's email. It was whitelisted for local debugging during Google OAuth testing and was never meant to stay that way — it needs to be removed from the whitelist (or gated behind an admin role) before this goes anywhere beyond local dev.

---

## 6. Roadmap (backend portion)

**MVP — done:**
- Auth: email/password + Google OAuth, JWT access token + rotating/revocable refresh token.
- Entities + migrations for User.

**MVP — in progress / next:**
- Pet — previously implemented, then removed (see §7) to redesign from scratch with a new approach.
- Vocabulary content + Progress/XP — previously implemented, then removed on `feature/vocabulary-content-refactor` to reset the data model; will be rebuilt from scratch now that auth is done (see §7).
- Spaced repetition logic.
- Daily reminders (push/email) — could be a scheduled job or an external service integration.

**Phase 2:**
- Pronunciation scoring API (AI-based; could be a separate service or an external call — reference: ELSA Speak).
- Parent dashboard API.
- Premium tier + billing.

**Future:**
- Social features (visit friends' pets), leaderboards, seasonal events, multi-language support.

---

## 7. Current status

- Backend: auth (email/password + Google OAuth, JWT access + rotating refresh tokens) is implemented and complete. See the API contract table in §5.
- **Pet was removed** (`pet/` package, its tests, and the `PetService` dependency in `AuthServiceImpl` are gone from the codebase) — the pet feature will be redesigned and rebuilt with a new approach, not restored as-is. The `pets` table itself is dropped via `V4__drop_pets_table.sql` rather than deleting the original `V2__create_pets_table.sql` migration, since that migration was already applied on the shared dev DB (see the Flyway lesson below).
- **Vocab and Progress were removed** (both the original `Topic`/`VocabWord`/`Progress` implementation and the in-progress consolidation into a single `Vocabulary` entity on `feature/vocabulary-content-refactor`) — the `vocab/` and `progress/` packages, their tests, and their migrations (`topics`/`vocab_words`/`progress` tables) are gone from the codebase. Decision: finish auth first, then rebuild vocab (and progress) cleanly from scratch rather than carry the half-finished refactor forward. Flyway migrations are `V1__create_users_table.sql` (users, with Google OAuth columns included from the start), `V2__create_pets_table.sql` (pets, now dropped by V4), `V3__create_refresh_tokens_table.sql` (refresh-token feature), `V4__drop_pets_table.sql` (drops pets ahead of a redesign).
- **The dev Postgres (Neon) is a real, shared database with its own migration history** — it is not a scratch/empty DB per environment. Its schema was reset once (2026-08-23) to match the squashed V1–V3 above, after an earlier squash attempt collided with migrations that had already been applied there (including one migration that was run directly against this DB and never committed to git, so its exact original SQL is permanently lost). Lesson: never rewrite or delete a migration file that's already been applied there without first checking `flyway_schema_history` on this DB — `git log`/local file state alone doesn't tell you what's actually been applied. Add a new forward migration (e.g. a `DROP TABLE`) instead.
- Current focus: rebuild vocabulary learning (content + progress/XP) and redesign pet, now that auth is done.
- **Testing policy (current):** implement features without writing tests by default; tests are written only when explicitly requested for a specific, already-implemented feature — don't write them proactively per feature.
- Local dev env vars: `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (Neon Postgres), `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`/`JWT_REFRESH_EXPIRATION_SECONDS`, `GOOGLE_CLIENT_ID` — see `.env.example`. No Testcontainers wired up (tests needing a full Spring context, e.g. `PawlingoApiApplicationTests`, run against the real Neon dev DB instead).
- Git: pushed to `origin` (`https://github.com/gahoccode215/pawlingo-api.git`).
- FE (`pawlingo-ui`) status isn't tracked in this doc — check that repo directly.
- Next step for backend: see `current-feature.md`.
