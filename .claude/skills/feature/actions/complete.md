# Complete Action

1. Stage all changes and commit with a descriptive message
2. Switch to main and merge the feature branch (no push yet)
3. Delete the local feature branch
4. Reset current-feature.md:
   - Change H1 back to `# Current Feature`
   - Clear Goals, Endpoints, and Notes sections (keep placeholder comments)
   - Add feature summary to the END of History
5. If any endpoints were added/changed in this feature, remind me to sync the API contract table in `project-overview.md` before pushing (don't edit it automatically — the FE team relies on it)
6. Commit the reset: `chore: reset current-feature.md after completing [feature]`
7. Push main to origin ONCE (single push with all changes)
8. If feature branch was previously pushed, delete it from origin