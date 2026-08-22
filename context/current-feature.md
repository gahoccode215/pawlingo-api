# Current Feature: Complete Auth (Email/Password + Google) with Access & Refresh Tokens

## Status
Complete

## Goals
- Add a rotating, revocable refresh token alongside the existing access token for `register` / `login` / `google` login.
- Add `POST /auth/refresh` to exchange a valid refresh token for a new access+refresh pair (rotation on every use).
- Add `POST /auth/logout` to revoke a single refresh token.
- Shorten access-token lifetime (recommend 15 min) now that the refresh flow covers session continuity, instead of today's 24h access token with no revocation.
- Shape the response contract so `pawlingo-ui` can integrate via NextAuth/Auth.js: Credentials provider for email/password calling `/auth/login`, Google provider forwarding its `id_token` to `/auth/google`, and the refresh token held only inside NextAuth's server-side encrypted session JWT — never sent to the browser.

## Endpoints
| Method | Path | Status | Notes |
|---|---|---|---|
| POST | `/auth/register` | Implemented | response gains `refreshToken` (+ `expiresIn`, missing before) |
| POST | `/auth/login` | Implemented | response gains `refreshToken` |
| POST | `/auth/google` | Implemented | response gains `refreshToken` |
| POST | `/auth/refresh` | Implemented | body `{ refreshToken }` → new `{ accessToken, refreshToken, expiresIn }` (rotates) |
| POST | `/auth/logout` | Implemented | body `{ refreshToken }` → revokes it; public endpoint, callable without a valid access token; idempotent (unknown/already-revoked token still returns success) |

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
- Resolved: migrations were squashed to `V1__create_users_table.sql` (users, incl. Google OAuth) + `V2__create_pets_table.sql` (pets) as part of removing vocab/progress (see `project-overview.md` §7). This feature adds `V3__create_refresh_tokens_table.sql` on top of that.
- FE wiring (NextAuth provider config, session/jwt callbacks) lives in `pawlingo-ui`, out of scope for this repo, but the response shapes above are the contract it will integrate against.

## Implementation Notes
- `auth/entity/RefreshToken.java` + `auth/repository/RefreshTokenRepository.java` — new.
- `auth/service/RefreshTokenService.java` (+ `impl/RefreshTokenServiceImpl.java`) — issues/rotates/revokes opaque tokens; SHA-256 hash via `HexFormat`, plaintext generated via `SecureRandom` (32 bytes, URL-safe base64). Records `RefreshTokenIssuance`/`RotatedRefreshToken` carry data back to `AuthServiceImpl`.
- `AuthServiceImpl` now depends on `RefreshTokenService`; `register`/`login`/`loginWithGoogle` each call `refreshTokenService.issue(...)` alongside the existing access-token JWT. New `refresh()`/`logout()` methods added to `AuthService`/`AuthServiceImpl`/`AuthController`.
- `SecurityConfig` whitelist gained `/api/v1/auth/refresh` and `/api/v1/auth/logout` (both public — they authenticate via the refresh token in the body, not a Bearer access token).
- `application.yaml`: `app.jwt.expiration-seconds` default dropped 86400 → 900; new `app.jwt.refresh-expiration-seconds` (default 2592000 = 30 days), both env-overridable.
- `user_agent`/`ip_address` columns exist on `refresh_tokens` but are **not populated yet** — no capture wiring added this pass (kept out of scope, see below).
- Existing `AuthServiceImplTest` updated (new mock + stubs for the added constructor param) so it keeps passing — no new tests written, per current testing policy.

## Out of Scope (this pass)
- Logout-all / revoke-all-sessions for a user.
- Password reset, email verification, account lockout.
- Any "manage active sessions" UI/endpoint (beyond storing the columns that would support it later).

## Open Questions
- Refresh token TTL — implemented at 30 days (`JWT_REFRESH_EXPIRATION_SECONDS`, env-overridable) since no pushback was raised; revisit if 7 days is preferred.
- `user_agent`/`ip` columns added to `refresh_tokens` as proposed, but left unpopulated this pass (no capture wiring) — confirm whether that's worth doing now or stays deferred with the "manage sessions" UI.

## History

### Design options considered (see chat for full writeup)
- **A — stateless refresh JWT (no DB)**: rejected — a long-lived token with no revocation path is a real security gap once refresh tokens live for weeks.
- **B — opaque refresh token, persisted + hashed + rotated (chosen)**: matches "backend owns all of auth" from `project-overview.md`, gives real revoke/logout/theft-detection, pairs naturally with NextAuth's own encrypted session JWT as the vault on the FE side.
- **C — backend sets refresh token as an HttpOnly cookie directly**: rejected for now — cross-origin cookie semantics between the Next.js app and the Spring Boot API (SameSite/Secure/CORS-credentials) add complexity that buys little once NextAuth is already the session vault sitting in front of the browser.
