# Current Feature


## Goals


## Endpoints

## Data Model


## Validation


## Security

## Error Handling


## Dependencies & Blockers


## Out of Scope (this pass)


## Open Questions



## History

### Design options considered (see chat for full writeup)
- **A — stateless refresh JWT (no DB)**: rejected — a long-lived token with no revocation path is a real security gap once refresh tokens live for weeks.
- **B — opaque refresh token, persisted + hashed + rotated (chosen)**: matches "backend owns all of auth" from `project-overview.md`, gives real revoke/logout/theft-detection, pairs naturally with NextAuth's own encrypted session JWT as the vault on the FE side.
- **C — backend sets refresh token as an HttpOnly cookie directly**: rejected for now — cross-origin cookie semantics between the Next.js app and the Spring Boot API (SameSite/Secure/CORS-credentials) add complexity that buys little once NextAuth is already the session vault sitting in front of the browser.

### Complete Auth (Email/Password + Google) with Access & Refresh Tokens
Added a rotating, revocable opaque refresh token alongside the existing JWT access token for register/login/google login. New `POST /auth/refresh` (rotates on every call, reuse detection revokes the user's entire active token set) and `POST /auth/logout` (revokes one token, works without a valid access token). Access token lifetime shortened from 24h to 15min. New `refresh_tokens` table (`V3__create_refresh_tokens_table.sql`), new `RefreshTokenService`/`RefreshTokenServiceImpl`, new `INVALID_REFRESH_TOKEN` error code. Response contract shaped for `pawlingo-ui` to integrate via NextAuth: refresh token intended to live only inside NextAuth's server-side encrypted session JWT, never sent to the browser.
