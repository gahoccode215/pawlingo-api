# Current Feature: Vocabulary Foundation

## Status
Not Started

## Goals
- Build the foundational vocabulary domain: reusable `Word` content (+ `WordExample`s), seeded/imported only, read-only via public API this phase.
- Let users browse/search/filter words in one paginated endpoint (case-insensitive prefix search on `word`, min 2 chars; filter by `difficultyLevel`/`partOfSpeech`, combinable with each other and with search).
- Let users view full word detail (meaning, phonetic, part of speech, difficulty, examples in `orderIndex` order, `audioUrl` passthrough — playback is a frontend concern).
- Let users save a word to their personal vocabulary (idempotent add — 200 + existing record if already saved, never 409), remove it (hard delete), and favorite/unfavorite it (favoriting an unsaved word implicitly creates the `UserVocabulary` record; unfavoriting does not delete the record).
- Let users list their personal vocabulary (`GET /users/me/vocabularies`), filterable by `isFavorite`/`status`.
- All endpoints use request/response DTOs — no JPA entity ever serialized directly.

## Endpoints
| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/vocabularies` | Planned | list/search/filter, paginated; query: `q`, `difficultyLevel`, `partOfSpeech`, `page`, `size`, `sort` (default `word,asc`) |
| GET | `/vocabularies/{id}` | Planned | full detail incl. `examples[]`; 404 if not found |
| POST | `/users/me/vocabularies` | Planned | body `{ wordId }` → save word; idempotent (200 if already saved, 201 if new); 404 `WORD_NOT_FOUND`, 400 if malformed |
| DELETE | `/users/me/vocabularies/{wordId}` | Planned | hard delete; 204; 404 `VOCABULARY_NOT_FOUND` if not saved |
| PATCH | `/users/me/vocabularies/{wordId}/favorite` | Planned | body `{ isFavorite }`; implicitly creates `UserVocabulary` if missing; 404 `WORD_NOT_FOUND` if word doesn't exist |
| GET | `/users/me/vocabularies` | Planned | user's saved words, paginated, filter by `isFavorite`/`status`; nested `word: WordSummaryResponse`. *(added to spec — needed for "My vocabulary" list, wasn't in original draft)* |

All `/users/me/*` endpoints require a valid Bearer access token (existing JWT middleware); 401 `UNAUTHORIZED` if missing/invalid. `/vocabularies` and `/vocabularies/{id}` are public reads.

## Data Model
- **Word** — `id`(UUID PK), `word`(varchar100 not null), `normalizedWord`(varchar100 not null — lowercase+trim+strip-diacritics of `word`, computed server-side, never from client input), `phonetic`(varchar100 nullable), `audioUrl`(varchar500 nullable), `difficultyLevel`(enum `A1,A2,B1,B2,C1,C2`, nullable), `partOfSpeech`(enum `NOUN,VERB,ADJECTIVE,ADVERB,PRONOUN,PREPOSITION,CONJUNCTION,INTERJECTION,OTHER`, not null), `primaryMeaning`(varchar500 not null), `createdAt`/`updatedAt`.
  - Multiple senses of the same word (e.g. "book" noun vs. verb) = multiple `Word` rows, same `word`/`normalizedWord`, different `partOfSpeech` — not modeled as sub-senses of one entity.
  - Indexes: `UNIQUE(normalizedWord, partOfSpeech)` (prevents duplicate seed entries), plus separate indexes on `difficultyLevel` and `partOfSpeech` for filters.
  - Read-only via public API this phase — created via internal seed/import only, no admin CRUD endpoint.
- **WordExample** — `id`(UUID PK), `wordId`(FK → Word, `ON DELETE CASCADE`, not null), `sentence`(varchar500 not null), `translation`(varchar500 nullable), `source`(varchar200 nullable), `orderIndex`(int default 0, display order). Max 10 examples/word enforced at seed/import time only, not DB level. Index on `wordId`.
- **UserVocabulary** — `id`(UUID PK), `userId`(FK → User, not null), `wordId`(FK → Word, not null), `isFavorite`(bool default false), `status`(enum `NEW,LEARNING,MASTERED`, default `NEW` — this phase only ever writes `NEW`, no endpoint updates it, reserved for SRS phase), `createdAt`/`updatedAt`. `UNIQUE(userId, wordId)` enforced at DB level.
- New Flyway migrations, following the one-table-per-file precedent (`V1` users, `V2` pets [dropped by `V4`], `V3` refresh_tokens): next available version is `V5` — one file each for `words`, `word_examples`, `user_vocabularies` (confirm exact numbering at `start` time against `flyway_schema_history` on the shared dev DB, per the lesson in `project-overview.md` §7).
- DTOs (never expose entities): `WordSummaryResponse`, `WordDetailResponse`, `WordExampleResponse`, `UserVocabularyResponse` (`id, wordId, isFavorite, status, createdAt`, + nested `word` when returned from the "my vocabulary" list), `AddVocabularyRequest{wordId}`, `FavoriteRequest{isFavorite}`.

## Validation
| Case | Status | Error code |
|---|---|---|
| `q` shorter than 2 chars | 400 | `VALIDATION_ERROR` |
| Invalid `difficultyLevel`/`partOfSpeech` enum value | 400 | `VALIDATION_ERROR` |
| `wordId` not a valid UUID | 400 | `VALIDATION_ERROR` |
| `size` > 100 | 400 `VALIDATION_ERROR`, **or** clamp to 100 — spec leaves this open, see Open Questions |

## Security
- `/vocabularies`, `/vocabularies/{id}` — public, no auth (shared, non-sensitive content). Must NOT require the whitelist workaround used for the debug `/users` endpoint — these are intentionally public by design, not a leftover.
- All `/users/me/vocabularies*` — require Bearer JWT via existing `common/security` filter; add to `SecurityConfig` as protected (default), not to the public whitelist.

## Error Handling
New `ErrorCode` entries needed:
- `WORD_NOT_FOUND` (404) — `wordId` doesn't exist, on add/favorite.
- `VOCABULARY_NOT_FOUND` (404) — removing a word not in the user's saved list.
- Existing `VALIDATION_ERROR` (400) / `UNAUTHORIZED` (401) reused for the cases above.

## Dependencies & Blockers
- None on the auth side — reuses the existing `@AuthenticationPrincipal User` pattern already used by `AuthController`.
- Needs a decision on how seed data for `Word`/`WordExample` gets in before this is testable end-to-end (spec says "seeded via script/import", explicitly not a public API) — Flyway `INSERT` migration vs. a separate script; not yet decided, see Open Questions.

## Out of Scope (this pass)
- Spaced repetition, quizzes, gamification, AI features.
- Admin CRUD UI/API for `Word` (seed/import only).
- Bulk import from external dictionary APIs.
- Multi-language translation / i18n content.
- Versioning / audit history of `Word` content.
- Editing `status` on `UserVocabulary` via API (reserved for the SRS phase).
- Rate limiting on search — spec says reuse an existing global rate-limit mechanism "if abused"; no such mechanism exists yet in this codebase, so treat as deferred, not a blocker for this pass.

## Open Questions
- `size` > 100: return `400 VALIDATION_ERROR` or silently clamp to 100? Spec explicitly defers this choice to implementation.
- Seed/import mechanism for `Word`/`WordExample`: Flyway `INSERT` migration, Spring `data.sql`, or a separate CLI/script? Needed before manual/API testing is possible; spec doesn't specify.
- Exact next Flyway version numbers (`V5`+) — confirm against `flyway_schema_history` on the shared Neon dev DB at `start` time, not just local file state (per the existing lesson in `project-overview.md` §7).

## History

### Design options considered (see chat for full writeup)
- **A — stateless refresh JWT (no DB)**: rejected — a long-lived token with no revocation path is a real security gap once refresh tokens live for weeks.
- **B — opaque refresh token, persisted + hashed + rotated (chosen)**: matches "backend owns all of auth" from `project-overview.md`, gives real revoke/logout/theft-detection, pairs naturally with NextAuth's own encrypted session JWT as the vault on the FE side.
- **C — backend sets refresh token as an HttpOnly cookie directly**: rejected for now — cross-origin cookie semantics between the Next.js app and the Spring Boot API (SameSite/Secure/CORS-credentials) add complexity that buys little once NextAuth is already the session vault sitting in front of the browser.

### Complete Auth (Email/Password + Google) with Access & Refresh Tokens
Added a rotating, revocable opaque refresh token alongside the existing JWT access token for register/login/google login. New `POST /auth/refresh` (rotates on every call, reuse detection revokes the user's entire active token set) and `POST /auth/logout` (revokes one token, works without a valid access token). Access token lifetime shortened from 24h to 15min. New `refresh_tokens` table (`V3__create_refresh_tokens_table.sql`), new `RefreshTokenService`/`RefreshTokenServiceImpl`, new `INVALID_REFRESH_TOKEN` error code. Response contract shaped for `pawlingo-ui` to integrate via NextAuth: refresh token intended to live only inside NextAuth's server-side encrypted session JWT, never sent to the browser.

### Remove pet (to redesign) and expose profile fields on /auth/me
Removed the `pet/` package, `PetService`, and its dependency in `AuthServiceImpl` entirely (register/Google login no longer auto-create a pet) — the feature will be redesigned from scratch with a new approach later. Dropped the `pets` table via a forward `V4__drop_pets_table.sql` migration rather than deleting `V2__create_pets_table.sql`, since V2 was already applied on the shared Neon dev DB (deleting it would break Flyway validation). Removed `ErrorCode.PET_NOT_FOUND` and the now-dead `GET /pet` endpoint entry from the API contract table. Separately, extended `GET /auth/me`'s response with `authProvider` and `createdAt` (previously only `id`/`email`/`goal`) so the FE can render a basic profile page after login — additive fields, no migration required.
