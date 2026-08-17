---
name: feature
description: Manage current feature workflow - start, review, explain or complete
argument-hint: load|start|review|explain|complete
---

# Feature Workflow

Manages the full lifecycle of a backend feature from spec to merge.

## Working File

@context/current-feature.md

### File Structure

current-feature.md has these sections:

- `# Current Feature` - H1 heading with feature name when active
- `## Status` - Not Started | In Progress | Complete
- `## Goals` - Bullet points of what success looks like
- `## Endpoints` - REST endpoints added/changed by this feature (method, path, status), so `project-overview.md`'s API contract can be updated in sync with the FE
- `## Notes` - Additional context, constraints, or details from spec (schema/migration notes, security considerations, open questions for FE)
- `## History` - Completed features (append only)

## Task

Execute the requested action: $ARGUMENTS

| Action | Description |
|--------|-------------|
| `load` | Load a feature spec or inline description |
| `start` | Begin implementation, create branch |
| `review` | Check goals met, run tests, verify migrations match entities, check code quality |
| `explain` | Document what changed and why, including any API contract changes the FE needs to know about |
| `complete` | Commit, push, merge, reset — and remind to update the API contract table in `project-overview.md` if endpoints changed |

See [actions/](actions/) for detailed instructions.

If no action provided, explain the available options.