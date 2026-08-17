# Current Feature

## Status



## Goals



## Endpoints



## Notes



## History

- 2026-08-16: Created static landing page prototype at `docs/pawlingo-landing/index.html` per Week 1 waitlist roadmap — hero, problem/why, features, personas, and waitlist CTA sections, styled with Tailwind (CDN) per project-overview.md.
- 2026-08-16: Reworked header to logo-left/centered-nav/login-register-right layout with mobile menu, and rebuilt footer into a 4-column layout (brand+social, Product, Support, copyright bar) with dynamic year.
- 2026-08-16: Landing Page Implementation feature left "In Progress" (branch `feature/landing-page-implementation`) — app/ moved to src/app/, all sections rebuilt as React/Tailwind v4 components, build+lint verified — when Vocabulary Learning spec was loaded on top of it. Resume/complete that feature separately before this history entry is superseded further.
- 2026-08-17: Completed Vocabulary Learning (Week 1 MVP) — topic intro, flashcard, mandatory 4-option quiz, Leitner-style 3-box repetition (wrong answers reset to box 1, capped at 5 repeats), and session summary screens, all wired via `VocabSession` at route `src/app/learn/page.tsx`. Local in-memory state only, no backend/DB. Added `src/types/vocab.ts`, `src/data/vocab/animals.ts`, `src/lib/vocab/{leitner,quiz}.ts`, `src/components/vocab/{TopicIntro,Flashcard,QuizCard,SessionSummary,VocabSession}.tsx`. Build and lint verified; UI not visually tested in-browser by Claude (user tests UI themselves).
- 2026-08-18: Vocabulary Learning — Phase 2 (persistence, multi-topic, backend-ready) left "In Progress" — localStorage persistence, second topic ("Everyday Food"), topic picker UI (`/learn`, `/learn/[topicId]`), and mock-data isolation (`src/lib/vocab/topics.ts`) all implemented; build+lint verified but UI not yet visually verified in-browser — when Auth spec was loaded on top of it. Resume/complete that feature separately before this history entry is superseded further.
- 2026-08-18: Completed Authentication (Email/Password) MVP — `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, all live and manually verified against a real Neon Postgres. Stack: Spring Security stateless JWT (`jjwt`), BCrypt hashing, `BusinessException`+`ErrorCode` for centralized error handling, `ApiResponseDTO` envelope, Flyway migration for `users`. Package layout is layered-within-feature (`controller/`, `service/`+`service/impl/`, `repository/`, `dto/request/`+`dto/response/`; entities in `entity/`, enums in `enums/`). Swagger UI added (springdoc 3.1.0) with JWT bearer auth and example request bodies at `/swagger-ui.html`. Fixed a Spring Boot 4.1 gotcha: Flyway autoconfiguration moved out of `spring-boot-autoconfigure` into a separate `spring-boot-flyway` module — without it migrations silently never ran. `.env` now gitignored with a `.env.example` template; README added with run instructions. Pet auto-creation on register left as a TODO (no `Pet` entity yet, per spec). Google OAuth, refresh tokens, and rate limiting explicitly out of scope for this pass. Merged to `main` (no PR, direct merge per user request); `feature/authentication-email-password-mvp` branch deleted after merge.
</content>
