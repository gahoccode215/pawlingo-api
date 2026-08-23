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