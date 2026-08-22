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

## 2026-08-23 — Remove vocab & progress (reset before rebuild)

#### Removed

- `GET /api/v1/vocabularies`
  - Reason: vocab data model was mid-refactor (consolidating `Topic`/`VocabWord` into a single `Vocabulary` entity) and got pulled entirely instead, to rebuild cleanly after auth is finished.
  - Frontend migration: temporarily unavailable; do not call. Will return with the same or a revised shape once vocab is rebuilt.
- `GET /api/v1/vocabularies/{id}`
  - Reason: same as above.
  - Frontend migration: temporarily unavailable.
- `POST /api/v1/progress`
  - Reason: depended on the vocab data model above; removed alongside it. Pet XP/energy update logic (`PetService.applyProgressResult`) stays in the codebase and will be reused once progress is rebuilt.
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