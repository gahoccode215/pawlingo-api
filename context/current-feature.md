# Current Feature: Deploy to Render (Free Tier) for FE Dev Testing

## Status
In Progress

## Goals
- Get a publicly reachable HTTPS URL for `pawlingo-api` running on Render's free Web Service tier, so `pawlingo-ui` can point at a live backend during dev instead of the dev running `:8080` locally alongside the FE's `:3000`.
- Zero cost — free tier only. Reuse the existing Neon dev Postgres DB (already used for local dev) rather than provisioning Render's own free Postgres, which auto-expires after 90 days.
- This is a dev/test convenience deploy, not a production release — no scope for prod-grade hardening, custom domain, or paid always-on tier.

## Endpoints
None — infra/deploy only, no API changes.

## Data Model
None — same Neon dev DB, already migrated. No new migrations.

## Validation
N/A

## Security
- `JWT_SECRET` must be set as a real secret directly in Render's dashboard env vars — never reuse the `.env` placeholder or commit it.
- `CORS_ALLOWED_ORIGINS` needs to include whatever origin the FE actually runs from while testing (typically still `http://localhost:3000`, since CORS is about the browser's origin, not where the API is hosted). Add the FE's deployed preview origin too if it's ever tested from somewhere other than localhost.
- `GOOGLE_CLIENT_ID` verification is ID-token based (no OAuth redirect URI), so no Google Cloud Console changes are needed for the new Render URL.

## Error Handling
N/A

## Dependencies & Blockers
- **No Dockerfile in the repo yet** — Render has no native Java buildpack, so this needs a multi-stage Dockerfile (Maven build stage → slim JRE runtime, e.g. `eclipse-temurin:21-jre`) before a Web Service can be created.
- **Port binding mismatch**: Render injects the port to listen on via a `PORT` env var, but `application.yaml` currently reads `SERVER_PORT` (`server.port: ${SERVER_PORT:8080}`). Needs a fallback chain (e.g. `${PORT:${SERVER_PORT:8080}}`) so the same config works locally (`SERVER_PORT`) and on Render (`PORT`).
- Needs a Render account with the GitHub repo connected (or manual image deploys).

## Implementation Notes
- Done: `server.port` in `application.yaml` now reads `${PORT:${SERVER_PORT:8080}}` — Render sets `PORT`, local dev keeps using `SERVER_PORT`/default `8080`.
- Done: added a multi-stage `Dockerfile` (Maven wrapper build stage on `eclipse-temurin:21-jdk` → `eclipse-temurin:21-jre` runtime) and a `.dockerignore`. Verified locally: `docker build` succeeds, and a full run against a real Postgres container confirmed Flyway migrates cleanly, Tomcat binds to the port given via `PORT` (tested with `PORT=10000`), and `GET /api/v1/vocabularies` returns 200.
- Env vars to set in Render's dashboard (mirroring `.env.example`): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (existing Neon dev credentials), `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `JWT_REFRESH_EXPIRATION_SECONDS`, `CORS_ALLOWED_ORIGINS`, `GOOGLE_CLIENT_ID`.
- Flyway runs on boot against the same already-migrated Neon dev DB — no migration changes needed, just confirm nothing else is mid-migration against that shared DB when this deploys.
- Render's free-tier health check just needs the port to open and accept TCP/HTTP — no Actuator dependency needed to add for this pass, but worth a quick check that Spring Boot's default error page on `/` doesn't 404 in a way Render treats as unhealthy.
- Optional: a `render.yaml` (Render "Blueprint") could codify the service config for reproducibility, instead of clicking through the dashboard once.

## Out of Scope (this pass)
- Render's own free Postgres (using existing Neon dev DB instead).
- Custom domain / production hardening / paid always-on instance.
- CI/CD auto-deploy pipeline beyond Render's built-in "deploy on push" (can be enabled trivially, not a custom pipeline).

## Open Questions
- Free tier services spin down after ~15 min idle and cold-start on the next request — for a JVM app this can take 30–60s+. Is that latency acceptable for FE dev testing, or does it need a workaround (e.g. a cheap uptime ping) later?
- Neon's own free tier also scales-to-zero on inactivity, so a cold Render instance + a cold Neon DB could stack into a slow first request after idle — worth confirming this is tolerable before relying on it daily.
- Does `pawlingo-ui` need a new env var (e.g. `NEXT_PUBLIC_API_URL`) pointed at the Render URL, or does it already externalize the API base URL?

## History

### Design options considered (see chat for full writeup)
- **A — stateless refresh JWT (no DB)**: rejected — a long-lived token with no revocation path is a real security gap once refresh tokens live for weeks.
- **B — opaque refresh token, persisted + hashed + rotated (chosen)**: matches "backend owns all of auth" from `project-overview.md`, gives real revoke/logout/theft-detection, pairs naturally with NextAuth's own encrypted session JWT as the vault on the FE side.
- **C — backend sets refresh token as an HttpOnly cookie directly**: rejected for now — cross-origin cookie semantics between the Next.js app and the Spring Boot API (SameSite/Secure/CORS-credentials) add complexity that buys little once NextAuth is already the session vault sitting in front of the browser.

### Complete Auth (Email/Password + Google) with Access & Refresh Tokens
Added a rotating, revocable opaque refresh token alongside the existing JWT access token for register/login/google login. New `POST /auth/refresh` (rotates on every call, reuse detection revokes the user's entire active token set) and `POST /auth/logout` (revokes one token, works without a valid access token). Access token lifetime shortened from 24h to 15min. New `refresh_tokens` table (`V3__create_refresh_tokens_table.sql`), new `RefreshTokenService`/`RefreshTokenServiceImpl`, new `INVALID_REFRESH_TOKEN` error code. Response contract shaped for `pawlingo-ui` to integrate via NextAuth: refresh token intended to live only inside NextAuth's server-side encrypted session JWT, never sent to the browser.

### Remove pet (to redesign) and expose profile fields on /auth/me
Removed the `pet/` package, `PetService`, and its dependency in `AuthServiceImpl` entirely (register/Google login no longer auto-create a pet) — the feature will be redesigned from scratch with a new approach later. Dropped the `pets` table via a forward `V4__drop_pets_table.sql` migration rather than deleting `V2__create_pets_table.sql`, since V2 was already applied on the shared Neon dev DB (deleting it would break Flyway validation). Removed `ErrorCode.PET_NOT_FOUND` and the now-dead `GET /pet` endpoint entry from the API contract table. Separately, extended `GET /auth/me`'s response with `authProvider` and `createdAt` (previously only `id`/`email`/`goal`) so the FE can render a basic profile page after login — additive fields, no migration required.

### Vocabulary Foundation
Added the foundational vocabulary domain from `context/features/01-vocabulary-foundation.md`: read-only `Word`/`WordExample` content (seeded via `V8__seed_words.sql`, 11 sample words — no public write API this phase) plus `UserVocabulary` for per-user save/remove/favorite. New `vocab` feature package (`entity`, `enums`, `repository`, `dto`, `service`, `controller`) with 6 endpoints — `GET /vocabularies` (search/filter/paginate), `GET /vocabularies/{id}`, and `POST`/`DELETE`/`PATCH`/`GET /users/me/vocabularies*` — implementing the idempotent-add (200/201), implicit-create-on-favorite, and hard-delete-on-remove behavior rules exactly. New migrations `V5`-`V8`. `ApiResponseDTO` gained an optional `meta` field for pagination (`page`/`size`/`totalElements`/`totalPages`) rather than nesting it inside `data`, keeping one envelope shape API-wide. `GlobalExceptionHandler` gained a `MethodArgumentTypeMismatchException` handler (400, previously fell through to a 500) needed for invalid enum query params and malformed UUID path variables. `size` > 100 clamps to 100 via `spring.data.web.pageable.max-page-size` rather than erroring. New error codes `WORD_NOT_FOUND`/`VOCABULARY_NOT_FOUND`. A real bug was found and fixed during review: searching with no `q` threw a 500 (Postgres couldn't infer the parameter type for a null String in `LIKE CONCAT(...)`) — fixed with an explicit `CAST(:normalizedPrefix AS string)` in the JPQL. Verified end-to-end with live HTTP smoke tests against the real dev DB (search/filter/detail/error cases, plus the full register→add→re-add→favorite→list→remove flow), not just unit-level review.
