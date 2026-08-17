# Load Action

1. Check $ARGUMENTS (after "load"):
   - If it looks like a filename (single word, no spaces): Look for `context/features/{name}.md` OR `context/fixes/{name}.md`
   - If it's multiple words: Use as inline feature description, generate goals
   - If empty: Error - "load" requires a spec filename or feature description

2. Update current-feature.md:
   - Update H1 heading to include feature name (e.g., `# Current Feature: Google OAuth Login`)
   - Write goals as bullet points under ## Goals
   - If the spec mentions API endpoints (new or changed), list them under ## Endpoints with method, path, and status `Planned`
   - Write any additional notes/context under ## Notes (schema/migration implications, security considerations, open questions for the FE team)
   - Set Status to "Not Started"

3. Confirm spec loaded and show the feature summary