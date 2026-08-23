# PawLingo API Changelog

This file tracks changes to backend APIs that may affect the PawLingo frontend.

Only frontend-relevant API changes should be recorded here.

---

## Unreleased

### Added

- None.

### Changed

- None.

### Deprecated

- None.

### Removed

- None.

### Breaking Changes

- None.

### Security

- None.

---

## 2026-08-24 — Vocabulary Foundation

#### Added

- `GET /api/v1/vocabularies`
  - Description: Browse/search/filter vocabulary words, paginated. Query params: `q` (case-insensitive prefix search on the word, min 2 chars, 400 if shorter), `difficultyLevel`, `partOfSpeech` (both combinable with each other and with `q`), `page`, `size` (default 20, clamped to 100 — no error above 100), `sort` (default `word,asc`; also accepts `difficultyLevel`/`createdAt`). Public — no auth required.
  - Frontend impact: paginated response now carries a `meta` object alongside `data`: `{ page, size, totalElements, totalPages }` (see Changed below for the envelope shape).
- `GET /api/v1/vocabularies/{id}`
  - Description: Full word detail — phonetic, difficulty, part of speech, meaning, and ordered `examples[]`. Public. `404 WORD_NOT_FOUND` if not found, `400 VALIDATION_ERROR` if `id` isn't a valid UUID.
- `POST /api/v1/users/me/vocabularies`
  - Description: Save a word to the current user's vocabulary. Body `{ wordId }`. Idempotent — `200` with the existing entry if already saved, `201` if newly created. `404 WORD_NOT_FOUND` if `wordId` doesn't exist, `400 VALIDATION_ERROR` if missing/malformed. Requires Bearer token.
- `DELETE /api/v1/users/me/vocabularies/{wordId}`
  - Description: Remove a word from the current user's vocabulary (hard delete). `204` on success, `404 VOCABULARY_NOT_FOUND` if not saved. Requires Bearer token.
- `PATCH /api/v1/users/me/vocabularies/{wordId}/favorite`
  - Description: Favorite/unfavorite a word. Body `{ isFavorite }`. Favoriting a word not yet saved implicitly creates the saved entry (status `NEW`); unfavoriting does not delete it. `404 WORD_NOT_FOUND` if the word doesn't exist. Requires Bearer token.
- `GET /api/v1/users/me/vocabularies`
  - Description: List the current user's saved words, paginated, filterable by `isFavorite`/`status`; each entry includes a nested `word` summary. Requires Bearer token.

#### Changed

- Response envelope `ApiResponseDTO<T>` gained a 4th field, `meta`: `{ success, data, error, meta }`. `meta` is `null` for every existing/non-paginated endpoint and only populated (`{ page, size, totalElements, totalPages }`) by the two paginated endpoints above — no impact on any other endpoint's response shape.

#### Security

- New error codes: `WORD_NOT_FOUND` (404), `VOCABULARY_NOT_FOUND` (404).
- `/vocabularies` and `/vocabularies/{id}` are intentionally public — shared, non-sensitive content, not an oversight. All `/users/me/vocabularies*` endpoints require a valid Bearer access token like other authenticated endpoints.

---

## 2026-08-24 — Remove pet (to redesign); expose profile fields on /auth/me

#### Changed

- `GET /api/v1/auth/me`
  - What changed: response now also includes `authProvider` and `createdAt` (previously only `id`, `email`, `goal`), so the FE can render a basic profile page after login (e.g. hide "change password" for Google accounts, show "member since").
  - Frontend impact: additive fields only, existing consumers unaffected.
  - Migration required: No.

#### Removed

- `GET /api/v1/pet`
  - Reason: pet feature is being redesigned from scratch with a new approach; old implementation (`pet/` package, `PetService`, `pets` table) removed entirely.
  - Frontend migration: temporarily unavailable; do not call. Will return with a different shape once redesigned.

---

## 2026-08-23 — Auth: access + refresh tokens

#### Changed

- `POST /api/v1/auth/register`
  - What changed: response now also includes `refreshToken` and `expiresIn` (`expiresIn` was missing entirely before).
  - Frontend impact: store the refresh token (server-side only, e.g. inside NextAuth's encrypted session JWT) and use it to renew the session instead of forcing re-login.
  - Migration required: Yes.
- `POST /api/v1/auth/login`
  - What changed: response now also includes `refreshToken`.
  - Frontend impact: same as above.
  - Migration required: Yes.
- `POST /api/v1/auth/google`
  - What changed: response now also includes `refreshToken`.
  - Frontend impact: same as above.
  - Migration required: Yes.

#### Added

- `POST /api/v1/auth/refresh`
  - Description: exchange a refresh token for a new access + refresh token pair. Rotates on every call — the presented refresh token is invalidated and a new one issued.
  - Frontend impact: call this when the access token expires instead of forcing the user to log in again.
- `POST /api/v1/auth/logout`
  - Description: revokes a single refresh token. Public endpoint — works even if the access token has already expired.
  - Frontend impact: call this on sign-out (before clearing local/session state) so the session is actually revoked server-side, not just abandoned client-side.

#### Security

- Access token lifetime shortened from 24h to 15 min (`JWT_EXPIRATION_SECONDS`, still env-overridable) now that refresh covers session continuity.
- New refresh token: opaque (not a JWT), 30-day default lifetime (`JWT_REFRESH_EXPIRATION_SECONDS`), stored server-side only as a SHA-256 hash, rotated on every `/auth/refresh` call.
- Reusing an already-rotated (previously-used) refresh token is treated as theft/replay and revokes **every** active refresh token for that user — expect a forced re-login on all devices if this triggers unexpectedly (e.g. a client retrying a stale token after a successful rotation already happened).
- New error code `INVALID_REFRESH_TOKEN` (401) — deliberately generic across not-found/expired/revoked/reused, so a client can't distinguish the failure reason from the response.

---

## 2026-08-23 — Remove vocab & progress (reset before rebuild)

#### Removed

- `GET /api/v1/vocabularies`
  - Reason: vocab data model was mid-refactor (consolidating `Topic`/`VocabWord` into a single `Vocabulary` entity) and got pulled entirely instead, to rebuild cleanly after auth is finished.
  - Frontend migration: temporarily unavailable; do not call. Will return with the same or a revised shape once vocab is rebuilt.
- `GET /api/v1/vocabularies/{id}`
  - Reason: same as above.
  - Frontend migration: temporarily unavailable.
- `POST /api/v1/progress`
  - Reason: depended on the vocab data model above; removed alongside it.
  - Frontend migration: temporarily unavailable.

---

## Change Format

Use the following format for future entries:

### YYYY-MM-DD

#### Added

- `METHOD /api/example`
  - Description
  - Frontend impact

#### Changed

- `METHOD /api/example`
  - What changed
  - Frontend impact
  - Migration required: Yes/No

#### Deprecated

- `METHOD /api/example`
  - Reason
  - Replacement

#### Removed

- `METHOD /api/example`
  - Reason
  - Frontend migration

#### Breaking Changes

- `METHOD /api/example`
  - Previous behavior
  - New behavior
  - Required frontend changes

#### Security

- Description of security-related API changes.