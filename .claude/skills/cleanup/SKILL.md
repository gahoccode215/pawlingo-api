---
name: cleanup
description: Clean up project housekeeping tasks (add "run" to execute fixes)
argument-hint: run|check
---

Review the codebase for cleanup tasks:

1. Make sure that the history in @context/current-feature.md is in order from oldest to newest
2. Find leftover debug statements in `src/main/java` — `System.out.println`, `printStackTrace()`, or `log.debug`/`log.info` calls that look like temporary debugging rather than real logging
3. Find unused imports
4. Check for stale TODO comments
5. Find orphaned/unused files — entities, DTOs, services, or repositories not referenced anywhere
6. Check that context files match actual project state — compare the API contract table in `project-overview.md` against the real `@RequestMapping`/`@GetMapping`/etc. endpoints in the code, and flag mismatches
7. Check that `application-prod.yml` (or `.properties` equivalent) has the same configuration keys as `application.yml`/`application-local.yml` (values don't need to match, keys should). If something is missing, tell me
8. Find `@SuppressWarnings`, raw (non-generic) type usage, or commented-out code blocks that might be stale

**Mode: $ARGUMENTS**

If no argument or argument is "check":

- Only report findings, don't modify anything
- List what WOULD be cleaned up

If the argument is "run" or "fix":

- First, report all findings with numbered items
- Then ask: "Which items would you like me to fix? (enter numbers like 1,3,5 or 'all' or 'none')"
- Wait for user response before making any changes
- Only fix the items the user specifies
- Report what you changed