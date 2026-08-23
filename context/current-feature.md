# Current Feature: Vocabulary Foundation

## Status
In Progress

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
| GET | `/vocabularies` | Implemented | list/search/filter, paginated; query: `q`, `difficultyLevel`, `partOfSpeech`, `page`, `size`, `sort` (default `word,asc`) |
| GET | `/vocabularies/{id}` | Implemented | full detail incl. `examples[]`; 404 if not found |
| POST | `/users/me/vocabularies` | Implemented | body `{ wordId }` → save word; idempotent (200 if already saved, 201 if new); 404 `WORD_NOT_FOUND`, 400 if malformed |
| DELETE | `/users/me/vocabularies/{wordId}` | Implemented | hard delete; 204; 404 `VOCABULARY_NOT_FOUND` if not saved |
| PATCH | `/users/me/vocabularies/{wordId}/favorite` | Implemented | body `{ isFavorite }`; implicitly creates `UserVocabulary` if missing; 404 `WORD_NOT_FOUND` if word doesn't exist |
| GET | `/users/me/vocabularies` | Implemented | user's saved words, paginated, filter by `isFavorite`/`status`; nested `word: WordSummaryResponse`. *(added to spec — needed for "My vocabulary" list, wasn't in original draft)* |

All `/users/me/*` endpoints require a valid Bearer access token (existing JWT middleware); 401 `UNAUTHORIZED` if missing/invalid. `/vocabularies` and `/vocabularies/{id}` are public reads.

**Response envelope decision:** the spec's `{ data, meta }` shape doesn't match the codebase's existing unified `ApiResponseDTO<T>{success, data, error}` envelope (no `meta` slot). Resolved: add an optional `meta` field to `ApiResponseDTO<T>` itself (`{success, data, error, meta}`, null when not paginated) rather than nesting pagination info inside `data` — keeps one envelope shape for the whole API and matches the spec's data/meta-as-siblings intent.

## Data Model
- **Word** — `id`(UUID PK), `word`(varchar100 not null), `normalizedWord`(varchar100 not null — lowercase+trim+strip-diacritics of `word`, computed server-side, never from client input), `phonetic`(varchar100 nullable), `audioUrl`(varchar500 nullable), `difficultyLevel`(enum `A1,A2,B1,B2,C1,C2`, nullable), `partOfSpeech`(enum `NOUN,VERB,ADJECTIVE,ADVERB,PRONOUN,PREPOSITION,CONJUNCTION,INTERJECTION,OTHER`, not null), `primaryMeaning`(varchar500 not null), `createdAt`/`updatedAt`.
  - Multiple senses of the same word (e.g. "book" noun vs. verb) = multiple `Word` rows, same `word`/`normalizedWord`, different `partOfSpeech` — not modeled as sub-senses of one entity.
  - Indexes: `UNIQUE(normalizedWord, partOfSpeech)` (prevents duplicate seed entries), plus separate indexes on `difficultyLevel` and `partOfSpeech` for filters.
  - Read-only via public API this phase — created via internal seed/import only, no admin CRUD endpoint.
- **WordExample** — `id`(UUID PK), `wordId`(FK → Word, `ON DELETE CASCADE`, not null), `sentence`(varchar500 not null), `translation`(varchar500 nullable), `source`(varchar200 nullable), `orderIndex`(int default 0, display order). Max 10 examples/word enforced at seed/import time only, not DB level. Index on `wordId`.
- **UserVocabulary** — `id`(UUID PK), `userId`(FK → User, not null), `wordId`(FK → Word, not null), `isFavorite`(bool default false), `status`(enum `NEW,LEARNING,MASTERED`, default `NEW` — this phase only ever writes `NEW`, no endpoint updates it, reserved for SRS phase), `createdAt`/`updatedAt`. `UNIQUE(userId, wordId)` enforced at DB level.
- New Flyway migrations, following the one-table-per-file precedent (`V1` users, `V2` pets [dropped by `V4`], `V3` refresh_tokens): `V5__create_words_table.sql`, `V6__create_word_examples_table.sql`, `V7__create_user_vocabularies_table.sql`, plus `V8__seed_words.sql` (sample data via `INSERT`, since no seed/import API exists this phase — decided over "no seed" so the API is actually testable end-to-end).
- DTOs (never expose entities): `WordSummaryResponse`, `WordDetailResponse`, `WordExampleResponse`, `UserVocabularyResponse` (`id, wordId, isFavorite, status, createdAt`, + nested `word` when returned from the "my vocabulary" list), `AddVocabularyRequest{wordId}`, `FavoriteRequest{isFavorite}`.

## Validation
| Case | Status | Error code |
|---|---|---|
| `q` shorter than 2 chars | 400 | `VALIDATION_ERROR` |
| Invalid `difficultyLevel`/`partOfSpeech` enum value | 400 | `VALIDATION_ERROR` |
| `wordId` not a valid UUID | 400 | `VALIDATION_ERROR` |
| `size` > 100 | Clamped to 100, no error — via `spring.data.web.pageable.max-page-size=100` |

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
- Seed data resolved via `V8__seed_words.sql` (11 sample words across all `partOfSpeech`/most `difficultyLevel` values, with 1-2 examples each) — enough to exercise search/filter/pagination manually.

## Implementation Notes
- New `vocab` feature package: `entity/` (`Word`, `WordExample`, `UserVocabulary` — plain UUID FK columns, no JPA relations), `enums/` (`DifficultyLevel`, `PartOfSpeech`, `VocabularyStatus`), `repository/` (`WordRepository` with a JPQL `search` query combining optional prefix/difficulty/part-of-speech filters, `WordExampleRepository`, `UserVocabularyRepository` with a similar optional-filter `search`), `dto/request/` + `dto/response/`, `service/` (`VocabularyService` for public word list/detail, `UserVocabularyService` for save/remove/favorite/list + `AddVocabularyResult` record carrying the created-vs-existing flag for the 200/201 status decision), `controller/` (`VocabularyController`, `UserVocabularyController`).
- `common/response/PageMeta.java` (new) + `ApiResponseDTO` gained a 4th `meta` field (null when not paginated); controllers build it via `PageMeta.of(page)` from Spring Data's `Page<T>`.
- `common/exception/GlobalExceptionHandler` gained a `MethodArgumentTypeMismatchException` handler → 400 `VALIDATION_ERROR` (needed for invalid enum query params like `difficultyLevel=xyz` and malformed UUID path variables, which previously fell through to the generic 500 handler).
- `application.yaml`: `spring.data.web.pageable.default-page-size: 20` / `max-page-size: 100` — implements "clamp to 100" natively via Spring Data's `Pageable` resolver, no custom validation code needed. `sort` query param also uses Spring's native `sort=field,direction` binding (matches the spec's format already).
- `q` min-length (2 chars) validation lives in `VocabularyServiceImpl` (business rule, not bean validation, since it's a plain query param not a request body).
- `SecurityConfig` whitelist gained `/api/v1/vocabularies` + `/api/v1/vocabularies/**` (public reads); `/users/me/vocabularies*` relies on the existing default-authenticated rule (not added to whitelist).
- Bug found and fixed during `/feature review`: `GET /vocabularies` with no `q` threw a 500 (`operator does not exist: character varying ~~ bytea`) — Postgres/Hibernate couldn't infer the parameter type for a null String bound into `LIKE CONCAT(:normalizedPrefix, '%')`. Fixed by casting explicitly in the JPQL: `LIKE CONCAT(CAST(:normalizedPrefix AS string), '%')` in `WordRepository.search`.
- Manually smoke-tested end-to-end against the real dev DB (app run locally on port 8081 to avoid the port-8080 dev instance already running): search (`q`, prefix match, 2-result multi-sense case for "book"), filter (`difficultyLevel`), invalid enum → 400, detail (found/404/malformed-UUID→400), register → add (201) → re-add (200 idempotent) → favorite existing (200) → favorite-implicit-create on unsaved word (200) → list with nested `word` and `isFavorite` → filter `isFavorite=false` (empty) → remove (204) → re-remove (404) → add unknown wordId (404) → add missing wordId (400) → list without token (401). All matched spec exactly.
- Sync of `project-overview.md`'s API contract table still deferred to `/feature complete`, per that workflow's own step.

## Out of Scope (this pass)
- Spaced repetition, quizzes, gamification, AI features.
- Admin CRUD UI/API for `Word` (seed/import only).
- Bulk import from external dictionary APIs.
- Multi-language translation / i18n content.
- Versioning / audit history of `Word` content.
- Editing `status` on `UserVocabulary` via API (reserved for the SRS phase).
- Rate limiting on search — spec says reuse an existing global rate-limit mechanism "if abused"; no such mechanism exists yet in this codebase, so treat as deferred, not a blocker for this pass.

## Open Questions
All resolved before `start`:
- `size` > 100 → clamp to 100 (no error).
- Seed mechanism → Flyway `INSERT` migration (`V8__seed_words.sql`).
- Pagination envelope shape → `meta` added as a sibling field on `ApiResponseDTO<T>`, not nested inside `data`.
- `normalizedWord` and `WordExample.orderIndex`-only-no-timestamps are internal; `WordDetailResponse` omits `normalizedWord` (no FE value, reveals internal search implementation) — deliberate narrowing of "all Word fields" from the spec.
- No JPA relations between entities (`Word`↔`WordExample`↔`UserVocabulary`) — plain UUID FK columns, matching the existing `RefreshToken.userId` convention rather than `@ManyToOne`/`@OneToMany`.

## History

### Design options considered (see chat for full writeup)
- **A — stateless refresh JWT (no DB)**: rejected — a long-lived token with no revocation path is a real security gap once refresh tokens live for weeks.
- **B — opaque refresh token, persisted + hashed + rotated (chosen)**: matches "backend owns all of auth" from `project-overview.md`, gives real revoke/logout/theft-detection, pairs naturally with NextAuth's own encrypted session JWT as the vault on the FE side.
- **C — backend sets refresh token as an HttpOnly cookie directly**: rejected for now — cross-origin cookie semantics between the Next.js app and the Spring Boot API (SameSite/Secure/CORS-credentials) add complexity that buys little once NextAuth is already the session vault sitting in front of the browser.

### Complete Auth (Email/Password + Google) with Access & Refresh Tokens
Added a rotating, revocable opaque refresh token alongside the existing JWT access token for register/login/google login. New `POST /auth/refresh` (rotates on every call, reuse detection revokes the user's entire active token set) and `POST /auth/logout` (revokes one token, works without a valid access token). Access token lifetime shortened from 24h to 15min. New `refresh_tokens` table (`V3__create_refresh_tokens_table.sql`), new `RefreshTokenService`/`RefreshTokenServiceImpl`, new `INVALID_REFRESH_TOKEN` error code. Response contract shaped for `pawlingo-ui` to integrate via NextAuth: refresh token intended to live only inside NextAuth's server-side encrypted session JWT, never sent to the browser.

### Remove pet (to redesign) and expose profile fields on /auth/me
Removed the `pet/` package, `PetService`, and its dependency in `AuthServiceImpl` entirely (register/Google login no longer auto-create a pet) — the feature will be redesigned from scratch with a new approach later. Dropped the `pets` table via a forward `V4__drop_pets_table.sql` migration rather than deleting `V2__create_pets_table.sql`, since V2 was already applied on the shared Neon dev DB (deleting it would break Flyway validation). Removed `ErrorCode.PET_NOT_FOUND` and the now-dead `GET /pet` endpoint entry from the API contract table. Separately, extended `GET /auth/me`'s response with `authProvider` and `createdAt` (previously only `id`/`email`/`goal`) so the FE can render a basic profile page after login — additive fields, no migration required.
