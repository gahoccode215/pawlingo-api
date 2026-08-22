# Start Action

1. Read current-feature.md - verify Goals are populated
2. If empty, error: "Run /feature load first"
3. Set Status to "In Progress"
4. Create and checkout the feature branch (derive name from H1 heading, e.g. `feature/google-oauth-login`)
5. List the goals, then implement them one by one, following `coding-standards.md` (package-by-feature, DTO/Entity separation, response envelope, Flyway for schema changes)
6. Do not write tests during this pass — implement-code-first policy. Tests are added later only when explicitly requested for a specific feature.