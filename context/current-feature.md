# Current Feature

## Status


## Goals


## Endpoints

## Data Model


## Validation


## Security

## Error Handling


## Dependencies & Blockers


## Implementation Notes


## Out of Scope (this pass)


## Open Questions



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
