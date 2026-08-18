# Current Feature: Vocabulary Learning — Phase 1: Vocab Content API

## Status

In Progress

## Goals

- FE lấy được danh sách chủ đề (topic) để hiện màn chọn chủ đề.
- FE lấy được danh sách từ trong 1 chủ đề để chạy flashcard/quiz.
- Schema đủ chỗ cho ảnh/audio ngay từ đầu (dù chưa dùng) để Phase 3 (ghép hình, nghe) không cần migration thêm cột.
- Nội dung (chủ đề, từ vựng) quản lý được qua DB, không hardcode trong code Java.

## Endpoints

| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/vocab/topics` | Planned |
| GET | `/api/v1/vocab/topics/{topicCode}` | Planned |

## Notes

- Spec đầy đủ: `context/features/vocab-phase-1-content-api.md` (phần của `context/vocab-learning-roadmap.md`, Phase 1/4).
- 3 quyết định đã chốt với user trước khi code:
  1. Định danh topic trong URL: **slug/code** (vd `animals`), không dùng UUID.
  2. Nguồn nội dung: **seed cứng trong Flyway migration** (vài chục từ mẫu), không xây admin API ở phase này.
  3. Endpoint **cần JWT** (giữ nhất quán với convention hiện tại — mọi endpoint trừ register/login/google đều yêu cầu Bearer token).
- Package mới `vocab` (ngang hàng `auth`, `user`): `entity/`, `repository/`, `service/`+`service/impl/`, `controller/`, `dto/response/`.
- Migration mới: `V3__create_topics_and_vocab_words.sql`.
- Chưa đụng `Pet`/XP/`Progress` — thuần content API, đó là Phase 2 (`vocab-phase-2-progress-pet-xp.md`).

## History

- 2026-08-16: Created static landing page prototype at `docs/pawlingo-landing/index.html` per Week 1 waitlist roadmap — hero, problem/why, features, personas, and waitlist CTA sections, styled with Tailwind (CDN) per project-overview.md.
- 2026-08-16: Reworked header to logo-left/centered-nav/login-register-right layout with mobile menu, and rebuilt footer into a 4-column layout (brand+social, Product, Support, copyright bar) with dynamic year.
- 2026-08-16: Landing Page Implementation feature left "In Progress" (branch `feature/landing-page-implementation`) — app/ moved to src/app/, all sections rebuilt as React/Tailwind v4 components, build+lint verified — when Vocabulary Learning spec was loaded on top of it. Resume/complete that feature separately before this history entry is superseded further.
- 2026-08-17: Completed Vocabulary Learning (Week 1 MVP) — topic intro, flashcard, mandatory 4-option quiz, Leitner-style 3-box repetition (wrong answers reset to box 1, capped at 5 repeats), and session summary screens, all wired via `VocabSession` at route `src/app/learn/page.tsx`. Local in-memory state only, no backend/DB. Added `src/types/vocab.ts`, `src/data/vocab/animals.ts`, `src/lib/vocab/{leitner,quiz}.ts`, `src/components/vocab/{TopicIntro,Flashcard,QuizCard,SessionSummary,VocabSession}.tsx`. Build and lint verified; UI not visually tested in-browser by Claude (user tests UI themselves).
- 2026-08-18: Vocabulary Learning — Phase 2 (persistence, multi-topic, backend-ready) left "In Progress" — localStorage persistence, second topic ("Everyday Food"), topic picker UI (`/learn`, `/learn/[topicId]`), and mock-data isolation (`src/lib/vocab/topics.ts`) all implemented; build+lint verified but UI not yet visually verified in-browser — when Auth spec was loaded on top of it. Resume/complete that feature separately before this history entry is superseded further.
- 2026-08-18: Completed Authentication (Email/Password) MVP — `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, all live and manually verified against a real Neon Postgres. Stack: Spring Security stateless JWT (`jjwt`), BCrypt hashing, `BusinessException`+`ErrorCode` for centralized error handling, `ApiResponseDTO` envelope, Flyway migration for `users`. Package layout is layered-within-feature (`controller/`, `service/`+`service/impl/`, `repository/`, `dto/request/`+`dto/response/`; entities in `entity/`, enums in `enums/`). Swagger UI added (springdoc 3.1.0) with JWT bearer auth and example request bodies at `/swagger-ui.html`. Fixed a Spring Boot 4.1 gotcha: Flyway autoconfiguration moved out of `spring-boot-autoconfigure` into a separate `spring-boot-flyway` module — without it migrations silently never ran. `.env` now gitignored with a `.env.example` template; README added with run instructions. Pet auto-creation on register left as a TODO (no `Pet` entity yet, per spec). Google OAuth, refresh tokens, and rate limiting explicitly out of scope for this pass. Merged to `main` (no PR, direct merge per user request); `feature/authentication-email-password-mvp` branch deleted after merge.
- 2026-08-19: Completed Authentication & Authorization via Google (Google OAuth) — `POST /api/v1/auth/google` verifies a Google ID token (`google-api-client`, `GoogleIdTokenVerifier` audience-checked against `GOOGLE_CLIENT_ID`; no client secret needed, ID-token-verification flow chosen over `spring-boot-starter-oauth2-client` since FE is a Next.js SPA using Google Identity Services client-side, not server redirect). Find-or-create logic: match by `googleId` first, then by email — an email match against an existing LOCAL account is rejected with `409 ACCOUNT_EXISTS_WITH_PASSWORD` rather than silently merged; a legacy GOOGLE row missing `googleId` gets backfilled. New user gets `authProvider = GOOGLE`, `passwordHash = null`. Same JWT issuance/response shape as email+password login (`isNewUser` flag added so FE can trigger onboarding). Schema: `V2__alter_users_for_google_oauth.sql` — `password_hash` now nullable, unique nullable `google_id` column added. New error codes `GOOGLE_TOKEN_INVALID` (401), `GOOGLE_EMAIL_NOT_VERIFIED` (403), `ACCOUNT_EXISTS_WITH_PASSWORD` (409). Email is now lowercased on register/login/google for consistent lookups; LOCAL/GOOGLE `passwordHash` invariant enforced in the service layer. `GoogleTokenVerifier`/`GoogleUserInfo` added as narrow single-caller technical utilities in `auth/service/` (same pattern as `JwtService`, no interface). 11/11 tests passing; migration applied and manually verified live against the real Neon Postgres via a local test harness (`scripts/google-login-test.html`, a static page using Google Identity Services to get a real ID token and POST it to the backend). Also added, at user's request for this verification: a debug-only `GET /api/v1/users` (lists users without leaking `passwordHash`) and a `SecurityConfig` refactor extracting hardcoded public paths / CORS lists into `WHITELIST_ENDPOINTS`, `CORS_ALLOWED_METHODS`, `CORS_ALLOWED_HEADERS` constants — **`GET /api/v1/users` is currently in that public whitelist with no auth, which leaks all user emails; this must be re-secured or removed before anything beyond local dev.** A follow-up idea (explicitly deferred, not done): renaming `GET /api/v1/auth/me` to a `/users/...`-style resource path — left for a separate, dedicated refactor since it would be a breaking change to an already-shipped endpoint outside this feature's scope. Account linking endpoint and LOCAL email verification also remain out of scope per spec. Merged to `main` (no PR, direct merge per user request); `feature/google-oauth-login` branch deleted after merge.
</content>
