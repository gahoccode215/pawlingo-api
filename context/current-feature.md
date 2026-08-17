# Current Feature: Authentication (Email/Password) — MVP

## Status

In Progress

## Goals

- User can register a new account with email + password
- User can log in with email + password and receive a JWT access token
- Passwords hashed with BCrypt, never stored in plaintext
- Endpoints protected by a JWT filter (Spring Security); missing/invalid token → 401
- Clear errors for: duplicate email, wrong email/password, invalid input
- Auto-create a default `Pet` for the user immediately after successful registration (unblocks Pet feature)

## Endpoints

| Method | Path | Status |
|---|---|---|
| POST | `/api/v1/auth/register` | Planned |
| POST | `/api/v1/auth/login` | Planned |
| GET | `/api/v1/auth/me` | Planned |

## Notes

- Implemented so far (branch `feature/authentication-email-password-mvp`):
  - `pom.xml` — added web, data-jpa, security, validation, postgresql, flyway-core (+flyway-database-postgresql), lombok, jjwt 0.12.6
  - `application.yaml` — datasource/JWT secret/CORS origin all via env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `CORS_ALLOWED_ORIGINS`), `ddl-auto: none`, Flyway enabled
  - `src/main/resources/db/migration/V1__create_users_table.sql`
  - `user` package: `User` entity, `Goal` enum (JSON value `beginner`/`test-prep`/`professional`/`for-child` via `@JsonValue`/`@JsonCreator`, DB stores enum name via `@Enumerated(STRING)`), `AuthProvider` enum, `user/repository/UserRepository`
  - `common/response`: `ApiResponseDTO<T>` (renamed from `ApiResponse` to avoid a future clash with springdoc-openapi's own response type), `ErrorDetail`
  - `common/exception`: `BusinessException` + `ErrorCode` enum (replaced per-error exception classes `DuplicateEmailException`/`InvalidCredentialsException` — new business errors are now a new `ErrorCode` constant, not a new file), `GlobalExceptionHandler` (409/401/400/500)
  - `common/security`: `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint` (401 via envelope, reuses `ErrorCode.UNAUTHORIZED`, not Spring's default plain-text 401)
  - `common/config/SecurityConfig`: stateless JWT, BCrypt bean, permits register/login, CORS from `app.cors.allowed-origins`
  - `auth` package restructured by layer: `auth/controller/AuthController`, `auth/service/AuthService` (interface) + `auth/service/impl/AuthServiceImpl`, `auth/service/JwtService` (no interface — single implementation, only used inside `auth`), DTOs in `auth/dto/request/` (`RegisterRequest`, `LoginRequest`) and `auth/dto/response/` (`RegisterResponse`, `LoginResponse`, `MeResponse`)
  - Pet auto-creation on register is a no-op with a TODO comment pointing at `auth-spec.md#data-model` (Pet entity doesn't exist yet — per spec, don't block auth merge on it)
  - `AuthServiceImplTest` (`auth/service/impl`) — 5 Mockito unit tests against `AuthServiceImpl` (register success/duplicate, login success/wrong-password/unknown-email), all passing
- Build note: Spring Boot 4.1.0 ships Jackson 3 (`tools.jackson.databind`, not `com.fasterxml.jackson.databind`) — `JwtAuthenticationEntryPoint` uses the `tools.jackson` import accordingly
- `mvnw compile` and the 5 new unit tests pass. The pre-existing `PawlingoApiApplicationTests.contextLoads` smoke test now fails because it boots the full Spring context, which needs a live Postgres connection — no DB/Docker is provisioned in this dev environment yet. Not a code defect; needs either a local Postgres or Testcontainers+Docker once available.
- `pawlingo-api` had no git repo before this feature — initialized one and made a baseline commit on `main` before branching
- Spec: `context/features/auth-spec.md`
- Google OAuth is explicitly out of scope for this pass (separate future feature) — don't let it block Pet/Vocab/Progress work
- Also out of scope: refresh token/logout-all-devices, forgot/reset password via email, login rate limiting (should come soon after but not blocking MVP)
- Data model: `User { id (UUID, PK), email (unique, not null), passwordHash (not null), goal (enum: beginner|test-prep|professional|for-child, default beginner), authProvider (enum: LOCAL only in scope), createdAt/updatedAt }`
- Migration: Flyway `V1__create_users_table.sql`
- Pet auto-creation on register needs a minimal `Pet` entity (id, userId, stage, energy, XP fields defaulting to 0) to exist first — if Pet entity isn't ready yet, stub the hook with a TODO referencing a ticket (not an orphaned TODO) and don't let it block merging auth
- Validation: email required + valid format + unique (409 `DUPLICATE_EMAIL` on conflict); password required, min 8 chars, no special-char restrictions for MVP; goal optional, defaults to `beginner`
- Security: BCryptPasswordEncoder for hashing; JWT signed with secret from `JWT_SECRET` env var (never hardcoded); access-token only, no refresh token this pass; token via `Authorization: Bearer <token>` header (not cookie) — chosen for MVP simplicity, avoids CORS/cookie config
- Error envelope follows `coding-standards.md` `{success, data, error}` shape via `GlobalExceptionHandler`:
  | Situation | HTTP | error.code |
  |---|---|---|
  | Duplicate email on register | 409 | `DUPLICATE_EMAIL` |
  | Wrong email/password on login | 401 | `INVALID_CREDENTIALS` |
  | Invalid input (validation) | 400 | `VALIDATION_ERROR` |
  | Missing/expired/invalid token | 401 | `UNAUTHORIZED` |
- **Open question for FE**: confirm `Authorization: Bearer` header (spec's default) vs httpOnly cookie before coding `AuthController` — cookie may be preferred if FE wants server-side token reads in Next.js SSR

## History

- 2026-08-16: Created static landing page prototype at `docs/pawlingo-landing/index.html` per Week 1 waitlist roadmap — hero, problem/why, features, personas, and waitlist CTA sections, styled with Tailwind (CDN) per project-overview.md.
- 2026-08-16: Reworked header to logo-left/centered-nav/login-register-right layout with mobile menu, and rebuilt footer into a 4-column layout (brand+social, Product, Support, copyright bar) with dynamic year.
- 2026-08-16: Landing Page Implementation feature left "In Progress" (branch `feature/landing-page-implementation`) — app/ moved to src/app/, all sections rebuilt as React/Tailwind v4 components, build+lint verified — when Vocabulary Learning spec was loaded on top of it. Resume/complete that feature separately before this history entry is superseded further.
- 2026-08-17: Completed Vocabulary Learning (Week 1 MVP) — topic intro, flashcard, mandatory 4-option quiz, Leitner-style 3-box repetition (wrong answers reset to box 1, capped at 5 repeats), and session summary screens, all wired via `VocabSession` at route `src/app/learn/page.tsx`. Local in-memory state only, no backend/DB. Added `src/types/vocab.ts`, `src/data/vocab/animals.ts`, `src/lib/vocab/{leitner,quiz}.ts`, `src/components/vocab/{TopicIntro,Flashcard,QuizCard,SessionSummary,VocabSession}.tsx`. Build and lint verified; UI not visually tested in-browser by Claude (user tests UI themselves).
- 2026-08-18: Vocabulary Learning — Phase 2 (persistence, multi-topic, backend-ready) left "In Progress" — localStorage persistence, second topic ("Everyday Food"), topic picker UI (`/learn`, `/learn/[topicId]`), and mock-data isolation (`src/lib/vocab/topics.ts`) all implemented; build+lint verified but UI not yet visually verified in-browser — when Auth spec was loaded on top of it. Resume/complete that feature separately before this history entry is superseded further.
</content>
