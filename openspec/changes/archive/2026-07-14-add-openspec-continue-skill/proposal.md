## Why

Agents can apply a named change, but there is no repository-local skill dedicated to resuming an already-started change from its current task and verification state. A continuation workflow is needed so interrupted or partially complete implementation resumes safely without rebuilding context, inventing a competing plan, or overstating completion.

## What Changes

- Add a project-local `openspec-continue` skill for continuing one active OpenSpec change.
- Require explicit selection when the change cannot be identified unambiguously.
- Re-resolve status, apply instructions, artifacts, tasks, and repository state before continuing implementation.
- Preserve incomplete verification and runtime gates instead of marking blocked work complete.
- Provide discoverable skill metadata and validate the skill structure.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `agent-openspec-workflow`: Add a durable continuation workflow for resuming partially implemented OpenSpec changes from current artifacts and evidence.

## Impact

- Adds `.codex/skills/openspec-continue/` with skill instructions and Codex UI metadata.
- Extends the repository agent workflow specification; it does not change Cofinance runtime behavior or dependencies.
