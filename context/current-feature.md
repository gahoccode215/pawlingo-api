# Current Feature: Complete Auth (Email/Password + Google) with Access & Refresh Tokens

## Status
Not Started

## Goals
- Add a rotating, revocable refresh token alongside the existing access token for `register` / `login` / `google` login.
- Add `POST /auth/refresh` to exchange a valid refresh token for a new access+refresh pair (rotation on every use).
- Add `POST /auth/logout` to revoke a single refresh token.
- Shorten access-token lifetime (recommend 15 min) now that the refresh flow covers session continuity, instead of today's 24h access token with no revocation.
- Shape the response contract so `pawlingo-ui` can integrate via NextAuth/Auth.js: Credentials provider for email/password calling `/auth/login`, Google provider forwarding its `id_token` to `/auth/google`, and the refresh token held only inside NextAuth's server-side encrypted session JWT — never sent to the browser.

## Endpoints
| Method | Path | Status | Notes |
|---|---|---|---|
| POST | `/auth/register` | Changed | response gains `refreshToken` (+ `expiresIn`, missing today) |
| POST | `/auth/login` | Changed | response gains `refreshToken` |
| POST | `/auth/google` | Changed | response gains `refreshToken` |
| POST | `/auth/refresh` | Planned | body `{ refreshToken }` → new `{ accessToken, refreshToken, expiresIn }` (rotates) |
| POST | `/auth/logout` | Planned | body `{ refreshToken }` → revokes it; callable without a valid access token |

## Data Model
New `refresh_tokens` table: `id`, `user_id` (FK), `token_hash` (SHA-256 of the opaque token, never store plaintext), `issued_at`, `expires_at`, `revoked_at`, `replaced_by_token_id` (self-FK, for rotation chains). Optional `user_agent`/`ip` columns now (cheap to add) to support a future "manage sessions" view, even though that UI is out of scope this pass.

## Validation
- Refresh token request body: non-blank string.
- Lookup hashes the incoming token (SHA-256) and compares against `token_hash` — plaintext refresh tokens are never persisted or logged.

## Security
- **Access token**: JWT, short-lived (recommend dropping `app.jwt.expiration-seconds` default from 86400 to 900 = 15 min).
- **Refresh token**: opaque random value (256-bit), *not* a JWT — no server state means no revocation, which is the whole point of adding this table.
- **Rotation**: every `/auth/refresh` call issues a new refresh token and marks the old one `revoked_at` + `replaced_by_token_id`.
- **Reuse detection**: presenting an already-revoked/replaced refresh token revokes the *entire* token chain for that user (signals theft) and forces re-login on all devices.
- `logout` only revokes the single presented token (one device/session).

## Error Handling
New `ErrorCode` entries:
- `INVALID_REFRESH_TOKEN` (401) — deliberately generic, covers not-found/expired/revoked/reused so a caller can't distinguish "expired" from "stolen and detected" by the response.

## Dependencies & Blockers
- This branch (`feature/vocabulary-content-refactor`) currently has **no Flyway migration files at all** (V1–V4 were deleted as part of the in-progress vocab consolidation and not yet replaced). The `refresh_tokens` migration needs a migration that (re)creates `users` to exist first — sequence the new migration after that one lands, don't add it in isolation.
- FE wiring (NextAuth provider config, session/jwt callbacks) lives in `pawlingo-ui`, out of scope for this repo, but the response shapes above are the contract it will integrate against.

## Out of Scope (this pass)
- Logout-all / revoke-all-sessions for a user.
- Password reset, email verification, account lockout.
- Any "manage active sessions" UI/endpoint (beyond storing the columns that would support it later).

## Open Questions
- Refresh token TTL — proposing 30 days; confirm vs. a shorter window (e.g. 7 days).
- Storing `user_agent`/`ip` on `refresh_tokens` now (recommended, cheap) vs. adding later.

## History

### Design options considered (see chat for full writeup)
- **A — stateless refresh JWT (no DB)**: rejected — a long-lived token with no revocation path is a real security gap once refresh tokens live for weeks.
- **B — opaque refresh token, persisted + hashed + rotated (chosen)**: matches "backend owns all of auth" from `project-overview.md`, gives real revoke/logout/theft-detection, pairs naturally with NextAuth's own encrypted session JWT as the vault on the FE side.
- **C — backend sets refresh token as an HttpOnly cookie directly**: rejected for now — cross-origin cookie semantics between the Next.js app and the Spring Boot API (SameSite/Secure/CORS-credentials) add complexity that buys little once NextAuth is already the session vault sitting in front of the browser.
