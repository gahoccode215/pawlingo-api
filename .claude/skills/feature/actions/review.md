# Review Action

1. Read current-feature.md to understand the goals and planned endpoints
2. Review all code changes made for this feature
3. Check for:
   - ✅ Goals met
   - ❌ Goals missing or incomplete
   - ⚠️ Code quality issues or bugs
   - 🚫 Scope creep (code beyond goals)
4. Backend-specific checks:
   - Controllers stay thin (no business logic) — logic lives in Services
   - Entities are never returned directly from Controllers (DTOs used)
   - New/changed endpoints return the `{success, data, error}` envelope
   - Schema changes have a corresponding Flyway migration, and the migration matches the entity
   - Endpoints requiring auth are actually protected in `SecurityConfig`
   - No hardcoded secrets or credentials introduced
   - Tests are **not** required by default for this pass — current policy is implement-first, tests only when explicitly requested for a specific feature. Don't flag missing tests as a review issue unless the user asked for tests on this feature. If tests already exist (requested earlier, or pre-existing), they should still pass (`./mvnw test`).
5. Final verdict: Ready to complete or needs changes