---
name: list-endpoints
description: List project API endpoints
argument-hint: "[controller]"
---

## Task

List all REST endpoints defined in `*Controller.java` files under `src/main/java` — i.e. methods annotated with `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`, or `@RequestMapping`.

Combine the class-level `@RequestMapping` base path (if any) with the method-level mapping to produce the full path.

If a [controller] name is provided via $ARGUMENTS, only list endpoints from the controller matching that name (case-insensitive, partial match allowed — e.g. `auth` matches `AuthController`).

## Output Format

Group by controller. For each endpoint show:

- HTTP method + full path
- Java method name and file (relative path)
- Brief one-line description (infer from method name, e.g. `login` → "Authenticates a user and returns a JWT")

End with a summary count (total controllers, total endpoints).

If no controllers/endpoints found, say "No endpoints found."