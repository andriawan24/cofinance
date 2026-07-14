## Context

The existing `openspec-apply-change` skill can begin or continue tasks, but it is optimized around applying pending tasks in the current context. Repository policy additionally requires resumed work—especially later turns or goals—to reconstruct the governing change, inspect current repository evidence, retain incomplete gates, and avoid parallel plans. A focused project-local continuation skill can encode those repository-specific recovery rules without changing the generated OpenSpec skill.

## Goals / Non-Goals

**Goals:**

- Resume exactly one named active change from its current artifacts, task state, working tree, and verification evidence.
- Handle ambiguous selection, blocked artifacts, completed changes, dirty worktrees, and stale task claims explicitly.
- Continue through pending tasks using the existing apply workflow and repository completion gates.
- Keep the skill concise, discoverable, and structurally validated.

**Non-Goals:**

- Create proposals for unrelated new work.
- Archive changes automatically.
- Mark runtime or external-service checks complete from compilation evidence.
- Replace or modify the generated `openspec-apply-change` skill.

## Decisions

### 1. Implement a project-local orchestration skill

Create `.codex/skills/openspec-continue` so the workflow travels with Cofinance and can reference its mandatory repository policy. A global skill would be less reliable because other repositories may use different completion gates.

### 2. Reconstruct state before delegating to apply semantics

The skill first reads repository instructions, lists changes, resolves exactly one selection, loads status and apply instructions, reads every returned context file, and inspects the working tree. Only then does it continue pending tasks. This makes continuation resilient to later turns and partially applied edits.

### 3. Treat task checkboxes as claims that require evidence

The continuation workflow compares checked tasks and stated verification against the current artifacts and repository state. It does not silently uncheck tasks, but it reports contradictions and updates artifacts before continuing when requirements or designs have drifted.

### 4. Reuse OpenSpec CLI and existing workflow skills

The skill uses `openspec status`, `openspec instructions apply`, strict validation, and the `openspec-apply-change` workflow rather than introducing a script. The procedure is primarily judgment-driven, so a concise `SKILL.md` and generated `agents/openai.yaml` are sufficient.

### 5. Keep generated skill metadata repository-visible

The repository's generic `agents/` ignore rule also matches nested skill metadata. Add a narrow `.gitignore` exception for `.codex/skills/*/agents/openai.yaml` so the continuation skill's generated interface remains tracked without exposing unrelated agent directories.

## Risks / Trade-offs

- **Overlap with `openspec-apply-change`** → Define continuation as state recovery and evidence reconciliation, then hand off to apply semantics for task execution.
- **Ambiguous active changes** → Require explicit user selection; never infer when multiple plausible changes remain.
- **Dirty worktree ownership confusion** → Inspect and preserve existing changes, stopping only when overlap makes safe continuation impossible.
- **False completion after resumed work** → Require task, requirement, test/build, strict validation, and applicable spec-sync gates before claiming completion.
- **Generated metadata remains ignored** → Add and verify a path-specific ignore exception for skill interface metadata.

## Verification Evidence

- The skill-creator initializer generated `.codex/skills/openspec-continue/SKILL.md` and matching `agents/openai.yaml` metadata.
- `git status --untracked-files=all` lists both skill files after the narrow `.gitignore` exception, confirming the generated metadata is repository-visible.
- `quick_validate.py` passed using an isolated `uv` environment with PyYAML.
- A scenario audit confirmed explicit coverage for named and ambiguous selection, blocked artifacts, pending work, dirty worktrees, unavailable runtime evidence, all-done audits, strict validation, spec synchronization, and explicit-only archival.
- `openspec validate add-openspec-continue-skill --strict` and `git diff --check` passed after implementation.
