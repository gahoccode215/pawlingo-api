# AI Interaction Guide — pawlingo-api

This file is for an AI assistant (Claude Code or similar) working in this backend repo. Goal: keep the codebase consistent and avoid the AI making large architectural decisions unilaterally without developer sign-off.

---

## 1. Read before doing anything

Before writing code, always read (in this order):

1. `project-overview.md` — product context, backend's role.
2. `current-feature.md` — what feature is currently in progress and its scope.
3. `coding-standards.md` — mandatory conventions.

If these three files conflict with a request in chat, **ask the user** rather than silently picking a side.

---

## 2. General coding principles

- Always follow `coding-standards.md` (package-by-feature, DTO/Entity separation, `{success, data, error}` response envelope, exceptions handled via `GlobalExceptionHandler`, etc.).
- Do not introduce new architecture/patterns (e.g. switching to CQRS, adding a message queue, changing ORM) unless explicitly requested — propose first, don't implement unprompted.
- When adding a new dependency to `pom.xml`, briefly explain why before adding it, since `pom.xml` is currently minimal.
- Prefer simple solutions appropriate to the current MVP stage — avoid over-engineering for an app still in the validation phase.

---

## 3. When implementing an API endpoint

Each new endpoint should typically come with:

1. Request DTO (with validation) + response DTO.
2. Controller method (thin, no business logic).
3. Service method (business logic).
4. Repository method if needed (extends `JpaRepository`, derived query or `@Query` as appropriate).
5. Flyway migration if the schema changes.
6. At least minimal tests for the Service (unit) and/or Controller (integration).
7. An update to the relevant "API contract" section in `project-overview.md` if the endpoint differs from what was planned — the FE (`pawlingo-ui`) relies on it to build `src/lib/api.ts`.

---

## 4. When to STOP and ask the user before proceeding

- Changing an existing table structure in a way that breaks existing data (breaking migration).
- Changing the response shape of an endpoint already consumed by the FE.
- Adding/changing auth libraries, security config, or anything affecting how JWT/OAuth works.
- Deleting or overwriting important config files (`application.yml`, `pom.xml`) when the scope isn't clear.
- A request that's ambiguous enough to be interpreted in multiple, architecturally different ways.

When unsure but the situation isn't one of the above, state a reasonable assumption and proceed — only stop and ask for the cases listed above.

---

## 5. Security & sensitive data

- Never hardcode secrets, API keys, or DB passwords in code or commits — always use environment variables.
- Never log passwords, tokens, or personal data (especially relevant since PawLingo has a "parent buying for child" persona — child-related data may be involved later).
- Don't add tracking/analytics that sends data externally unless explicitly requested.

---

## 6. After finishing a task

- Update `current-feature.md`: mark what's done, what's left, and any blockers.
- If the task introduces changes affecting the FE (new endpoint, changed field), call that out clearly in the response summary so the dev can relay it to `pawlingo-ui`.
- Run relevant tests before considering the task "done" (if the environment allows running Maven/tests).

---

## 7. Preferred tone / response style

- Keep responses concise and focused on what was done/proposed — no need to re-explain context already covered by the other three files.
- When proposing a technical decision (e.g. Testcontainers instead of H2), give a brief reason, not a long writeup.
- If a conflict is found between context files (e.g. `project-overview.md` is stale compared to reality), flag it and suggest an update rather than silently following the outdated version.