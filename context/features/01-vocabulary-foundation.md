# Vocabulary Feature — Spec 01: Vocabulary Foundation

## 1. Goal

Build the foundational vocabulary domain for the English-learning system.

This phase focuses on storing reusable vocabulary data and allowing users to discover, view, save, search, and organize words.

**Out of scope for this phase** (explicit):
- Spaced repetition, quizzes, gamification, AI features
- Admin CRUD UI for `Word` (words are seeded via script/import, not via public API)
- Bulk import from external dictionary APIs
- Multi-language translation / i18n content
- Versioning / audit history of Word content
- Editing `status` on `UserVocabulary` via API (reserved for the SRS phase)

## 2. Data Model

### 2.1 Word

Represents vocabulary content shared across all users. Read-only via public API in this phase (created via internal seed/import only).

| Field | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| word | varchar(100) | not null |
| normalizedWord | varchar(100) | not null, **unique index** |
| phonetic | varchar(100) | nullable (IPA notation) |
| audioUrl | varchar(500) | nullable |
| difficultyLevel | enum: `A1,A2,B1,B2,C1,C2` | nullable |
| partOfSpeech | enum: `NOUN,VERB,ADJECTIVE,ADVERB,PRONOUN,PREPOSITION,CONJUNCTION,INTERJECTION,OTHER` | not null |
| primaryMeaning | varchar(500) | not null |
| createdAt | timestamp | not null, auto |
| updatedAt | timestamp | not null, auto |

Notes:
- `normalizedWord` = lowercase + trim + strip diacritics of `word`. Computed on write, never accepted from client input.
- A word with multiple senses (e.g. "book" as noun and verb) is represented as **two separate `Word` rows**, same `word`/`normalizedWord`, different `partOfSpeech`. This keeps the model simple for this phase; merging senses under one entity is deferred.
- Index: `UNIQUE(normalizedWord, partOfSpeech)` — prevents duplicate seed entries for the same word+sense.
- Additional indexes: `difficultyLevel`, `partOfSpeech` (used by filters).

### 2.2 WordExample

A word can have multiple examples.

| Field | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| wordId | UUID | FK → Word, not null, `ON DELETE CASCADE` |
| sentence | varchar(500) | not null |
| translation | varchar(500) | nullable |
| source | varchar(200) | nullable (e.g. "Oxford Dictionary") |
| orderIndex | int | default 0, used for display order |

Constraint: max 10 examples per word enforced at seed/import time (not enforced at DB level).

### 2.3 UserVocabulary

Represents a user's relationship with a word.

| Field | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| userId | UUID | not null, FK → User |
| wordId | UUID | not null, FK → Word |
| isFavorite | boolean | default false |
| status | enum: `NEW,LEARNING,MASTERED` | default `NEW` |
| createdAt | timestamp | not null, auto |
| updatedAt | timestamp | not null, auto |

Constraint: `UNIQUE(userId, wordId)` — enforced at DB level, not just application level.

Note: `status` is written by this phase's default only (`NEW`). No endpoint in this spec updates it — reserved for the SRS phase.

## 3. Functional Requirements

### 3.1 Vocabulary Detail

The user can:
- View word, pronunciation, part of speech, difficulty level, meaning, examples
- Play audio when `audioUrl` is present (frontend concern; backend just returns the URL)
- Add word to personal vocabulary
- Remove word from personal vocabulary
- Favorite / unfavorite a word

**Behavior rules (must implement exactly as specified):**

| Action | Rule |
|---|---|
| Add word already saved | Idempotent — return `200 OK` with the existing `UserVocabulary`, do **not** throw 409. |
| Favorite a word not yet saved | Favoriting **implicitly creates** the `UserVocabulary` record (status `NEW`, `isFavorite = true`). Favorite does not require a prior "add" call. |
| Unfavorite | Sets `isFavorite = false`. Does **not** delete the `UserVocabulary` record. |
| Remove saved word | Hard delete the `UserVocabulary` row (this also clears favorite status, since the row is gone). |
| Remove a word that isn't saved | Return `404 Not Found`. |

### 3.2 Search

- Search by `word` field, **case-insensitive**, **prefix match only** in this phase (`normalizedWord LIKE 'query%'`). No fuzzy matching, no stemming, no full-text search engine in this phase.
- Query param: `q`, min length 2 characters (return `400` if shorter).
- Combine with `page`/`size` pagination (see §4).

### 3.3 Filter

- `difficultyLevel` (single value, from enum)
- `partOfSpeech` (single value, from enum)
- Both are combinable with each other and with `q` search on the same list endpoint (see §4.1) — no separate filter endpoint.

## 4. API Specification

All endpoints prefixed `/api/v1`. All `/users/me/*` endpoints require a valid Bearer token (existing auth middleware); return `401` if missing/invalid.

Standard response envelope:
```json
{ "data": ..., "meta": { "page": 0, "size": 20, "totalElements": 137, "totalPages": 7 } }
```
`meta` is omitted for single-object responses.

Standard error envelope:
```json
{ "error": { "code": "VALIDATION_ERROR", "message": "Word not found", "timestamp": "2026-08-24T10:00:00Z" } }
```

### 4.1 `GET /vocabularies`

List/browse/search/filter words in one endpoint.

Query params:
| Param | Type | Required | Notes |
|---|---|---|---|
| q | string | no | prefix search, min 2 chars if present |
| difficultyLevel | enum | no | |
| partOfSpeech | enum | no | |
| page | int | no | default 0 |
| size | int | no | default 20, max 100 |
| sort | string | no | default `word,asc`; allowed: `word`, `difficultyLevel`, `createdAt` |

Response: `200` — paginated list of `WordSummaryResponse`:
```json
{ "id": "...", "word": "environment", "phonetic": "/ɪnˈvaɪrənmənt/", "difficultyLevel": "B1", "partOfSpeech": "NOUN", "primaryMeaning": "..." }
```

### 4.2 `GET /vocabularies/{id}`

Response: `200` — `WordDetailResponse` (all Word fields + `examples: WordExampleResponse[]`).
`404` if not found.

### 4.3 `POST /users/me/vocabularies`

Request body: `{ "wordId": "uuid" }`
Response: `201` (or `200` if already existed — idempotent, see §3.1) — `UserVocabularyResponse`.
`404` if `wordId` doesn't exist. `400` if `wordId` missing/malformed.

### 4.4 `DELETE /users/me/vocabularies/{wordId}`

Response: `204 No Content`.
`404` if the user has no `UserVocabulary` for this `wordId`.

### 4.5 `PATCH /users/me/vocabularies/{wordId}/favorite`

Request body: `{ "isFavorite": true }`
Response: `200` — `UserVocabularyResponse`.
Creates the `UserVocabulary` record implicitly if it doesn't exist yet (see §3.1).
`404` if `wordId` doesn't exist as a `Word`.

### 4.6 `GET /users/me/vocabularies`

*(added — needed to render "My vocabulary" list; missing from the original draft)*

Query params: `isFavorite` (bool, optional), `status` (enum, optional), `page`, `size`.
Response: `200` — paginated list of `UserVocabularyResponse` (each including nested `word: WordSummaryResponse`).

### DTOs — never expose JPA entities directly

- `WordSummaryResponse`, `WordDetailResponse`, `WordExampleResponse`
- `UserVocabularyResponse` (includes `id, wordId, isFavorite, status, createdAt`, nested `word` when returned from §4.6)
- `AddVocabularyRequest { wordId }`
- `FavoriteRequest { isFavorite }`

## 5. Validation & Error Handling

| Case | Status | Error code |
|---|---|---|
| `q` shorter than 2 chars | 400 | `VALIDATION_ERROR` |
| Invalid `difficultyLevel`/`partOfSpeech` enum value | 400 | `VALIDATION_ERROR` |
| `wordId` not a valid UUID | 400 | `VALIDATION_ERROR` |
| `wordId` not found (add/favorite) | 404 | `WORD_NOT_FOUND` |
| Remove word not in user's list | 404 | `VOCABULARY_NOT_FOUND` |
| Missing/invalid auth token on `/users/me/*` | 401 | `UNAUTHORIZED` |
| `size` > 100 | 400 | `VALIDATION_ERROR` (or clamp to 100 — pick one, document the choice) |

All error responses use the standard error envelope from §4.

## 6. Non-Functional Requirements

- Indexes: `Word.normalizedWord`, `Word.difficultyLevel`, `Word.partOfSpeech`, `UserVocabulary(userId, wordId)` unique, `WordExample.wordId`.
- `GET /vocabularies` and `/search` expected p95 response time < 300ms at 100k `Word` rows.
- Rate limit search endpoint if abused (reuse existing global rate-limit middleware — no new mechanism needed this phase).

## 7. Frontend

Create:
- Vocabulary list page (search input, difficulty filter, part-of-speech filter)
- Vocabulary detail page (meaning, pronunciation, audio, examples, add/remove, favorite toggle)
- My vocabulary page (consumes `GET /users/me/vocabularies`)

Keep UI simple — the objective is to validate the domain and API, not visual polish.

## 8. Acceptance Criteria

- A word can be created (via seed) and retrieved via API.
- A word can have multiple examples, returned in `orderIndex` order.
- Authenticated users can save a word; saving twice is idempotent, not an error.
- Users cannot have duplicate `UserVocabulary` rows for the same word (DB-enforced).
- Users can favorite/unfavorite a word; favoriting an unsaved word auto-saves it.
- Removing a word deletes the `UserVocabulary` row entirely.
- Search works case-insensitively via prefix match.
- Filtering by difficulty level and part of speech works, combinable with search.
- API uses request/response DTOs; no JPA entity is ever serialized directly.
- All error cases in §5 return the documented status code and error envelope.
- Frontend consumes the real backend API (list, detail, add/remove, favorite, my-vocabulary).