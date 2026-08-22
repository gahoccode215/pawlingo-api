# Complete Action

1. If `current-feature.md`'s `## Endpoints` section is non-empty, update `docs/api-changelog.md` first: add a new `## YYYY-MM-DD — <feature name>` section (today's date), populated straight from `current-feature.md` — no re-deriving from a diff:
   - Endpoints marked `Planned`/new → **Added**
   - Endpoints marked `Changed` → **Changed** (state the shape/behavior change from the Endpoints/Notes/Data Model sections)
   - Anything from `## Security` or `## Error Handling` that's FE-visible (new error codes, new required headers, revocation/token behavior, etc.) → **Security**
   - Anything removed/deprecated → **Removed**/**Deprecated**
   - Leave a category's `- None.` line alone if this feature has nothing for it
   This keeps the changelog populated as a side effect of the workflow instead of a separate manual step.
2. Stage all changes (including the changelog update) and commit with a descriptive message
3. Switch to main and merge the feature branch (no push yet)
4. Delete the local feature branch
5. Reset current-feature.md:
   - Change H1 back to `# Current Feature`
   - Clear Goals, Endpoints, and Notes sections (keep placeholder comments)
   - Add feature summary to the END of History
6. If any endpoints were added/changed in this feature, remind me to sync the API contract table in `project-overview.md` before pushing (don't edit it automatically — the FE team relies on it, and unlike the changelog this table reflects current state so it needs a judgment call about wording, not a mechanical append)
7. Commit the reset: `chore: reset current-feature.md after completing [feature]`
8. Push main to origin ONCE (single push with all changes)
9. If feature branch was previously pushed, delete it from origin