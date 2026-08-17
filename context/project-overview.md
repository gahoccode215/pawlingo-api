# PawLingo Backend — Project Overview

🐾 **Learn English, Raise a Pet** — repo: `pawlingo-api`

> This is the backend repo (Spring Boot). The frontend lives in a separate repo: `pawlingo-ui` (Next.js). This file is a condensed, backend-focused version of the product spec provided by the FE team.

---

## 1. Core idea

Most English-learning apps rely on abstract motivators (streaks, XP, leaderboards) that feel like obligations rather than something users genuinely care about. PawLingo ties **the pet's growth directly to real language progress** — the pet levels up, learns new tricks, and evolves based on vocabulary mastered and skills improved, not just coins spent.

The backend owns **all business logic, the database, and auth**. Next.js only calls the API — it has no direct DB access.

---

## 2. Backend's role in the system

- Owns **all data**: User, Pet, VocabWord, Progress.
- Owns **authentication**: Email/Password + Google OAuth, issues JWT/session. The FE does not manage credentials itself.
- Exposes REST API for the FE (`pawlingo-ui`) consumed via `src/lib/api.ts`.
- Owns business rules: XP calculation, pet energy decay over time, spaced repetition, etc.

---

## 3. Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 (already set in `pom.xml`) |
| Build | Maven |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Migration | Flyway (recommended addition) |
| Auth | Spring Security + JWT, Google OAuth2 Client |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Docs | springdoc-openapi (Swagger UI) |
| Test | JUnit 5, Spring Boot Test, Testcontainers (recommended for integration tests against Postgres) |

> **Note:** `pom.xml` currently only has `spring-boot-starter`. Still need to add: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-oauth2-client`, `postgresql` driver, `flyway-core`, `lombok`, `springdoc-openapi-starter-webmvc-ui`.

---

## 4. Architecture

```
Next.js (pawlingo-ui)  --HTTP/JSON-->  Spring Boot (pawlingo-api)  --JPA-->  PostgreSQL
```

- Layered architecture in the backend: `Controller → Service → Repository → Entity`.
- A unified response envelope `{ success, data, error }` so it matches how the FE handles errors in `src/lib/api.ts` (details in `coding-standards.md`).

---

## 5. Data model (JPA entities — source of truth owned by backend)

```
User
- id
- email
- passwordHash        // null if signed up via Google
- goal                 // beginner | test-prep | professional | for-child
- authProvider         // LOCAL | GOOGLE
- pet                  (1:1)
- progresses           (1:N)
- createdAt / updatedAt

Pet
- id
- name
- stage                // evolution stage
- energy
- listeningXp / speakingXp / readingXp / writingXp
- coins
- outfits[]            // cosmetic item ids owned
- userId
- updatedAt

VocabWord
- id
- word
- definition
- imageUrl
- topic

Progress
- id
- correctCount / wrongCount
- lastReviewed
- userId
- wordId
- unique(userId, wordId)
```

---

## 6. API contract (initial direction — to be refined)

- Base path: `/api/v1`
- Auth: JWT (access token, plus refresh token if needed); FE stores it via httpOnly cookie or a session bridge.
- Response envelope: `{ success: boolean, data: T | null, error: { code, message } | null }`

Priority endpoints for MVP:

| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Register with email/password |
| POST | `/auth/login` | Login with email/password |
| GET/POST | `/auth/google` | Google OAuth login/callback |
| GET | `/pet` | Get the current user's pet |
| GET | `/vocab/topics/{topic}` | Get vocabulary words for a topic |
| POST | `/progress` | Record a learning result (correct/wrong) |

---

## 7. Roadmap (backend portion)

**MVP (after the FE validation phase):**
- Stand up the Spring Boot backend: auth (email/password + Google OAuth) + database.
- Entities + migrations for User, Pet, VocabWord, Progress.
- API for FE integration via `src/lib/api.ts`.
- Multiple vocab topics, basic spaced repetition logic.
- Daily reminders (push/email) — could be a scheduled job or an external service integration.

**Phase 2:**
- Pronunciation scoring API (AI-based; could be a separate service or an external call — reference: ELSA Speak).
- Parent dashboard API.
- Premium tier + billing.

**Future:**
- Social features (visit friends' pets), leaderboards, seasonal events, multi-language support.

---

## 8. Current status

- Backend: project just scaffolded (`pawlingo-api`), no business modules yet.
- FE (`pawlingo-ui`): in the validation MVP phase (landing page + static demo, not yet calling the backend).
- Next step for backend: see `current-feature.md`.