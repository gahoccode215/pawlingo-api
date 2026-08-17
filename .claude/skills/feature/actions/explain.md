# Explain Action

1. Read current-feature.md to understand what was implemented
2. Run `git diff main --name-only` to get list of files changed
3. For each file created or modified:
   - Show the file path
   - Give a 1-2 sentence explanation of what it does / what changed
   - Highlight any key classes, methods, or patterns used (e.g. `@Service`, `@RestControllerAdvice`, DTO mapping, Flyway migration, JWT filter)
4. If any REST endpoints were added or changed, list them explicitly (method + path) since the FE depends on this
5. End with a brief summary of how the pieces fit together (request flow through Controller → Service → Repository → DB)

## Output Format

## Files Changed

**path/to/File.java** (new)
Brief explanation of what this file does and why it was added.

**path/to/Other.java** (modified)
What changed and why.

## Endpoints Affected

- `POST /api/v1/auth/login` — new
- `GET /api/v1/pet` — response shape changed (added `stage` field)

(omit this section if no endpoints changed)

## How It All Connects

Brief summary of the request/data flow between these files.