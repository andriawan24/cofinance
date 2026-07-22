---
name: openspec-continue
description: Resume implementation of an active OpenSpec change from its current artifacts, tasks, working tree, and verification evidence. Use when work was interrupted, a partially implemented change must continue in a later turn, or checked tasks and remaining validation need to be reconciled before further edits.
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
---

Resume one existing OpenSpec change without rebuilding its plan or weakening its completion gates.

**Store selection:** If the user names a store (a store is a standalone OpenSpec repo registered on this machine) or the work lives in one, run `openspec store list --json` to discover registered store ids, then pass `--store <id>` on the commands that read or write specs and changes (`status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`). Without a store, commands act on the nearest local `openspec/` root.

**Input**: Optionally specify a change name. If omitted, infer from conversation context or resolve as below.

**Steps**

1. **Resolve the change**

   Run `openspec list --json` and resolve exactly one change:
   - Use an explicitly named change.
   - If no name given, auto-select only when exactly one active change can govern the request.
   - If multiple changes could govern it, show the active choices via **AskUserQuestion** and wait for explicit selection.
   - Do not create a new change merely because the current one is partially complete.

   Announce: "Using change: <name>" and how to override.

2. **Reconstruct current state**

   Run:
   ```bash
   openspec status --change "<name>" --json
   openspec instructions apply --change "<name>" --json
   ```
   Use `planningHome`, `changeRoot`, `artifactPaths`, `actionContext`, and `contextFiles` from the JSON instead of assuming paths. Read every returned context file completely. Then inspect `git status --short`, the relevant diff, and any existing verification evidence — do not discard or overwrite unrelated user changes.

   Before editing implementation files, run:
   ```bash
   openspec validate "<name>" --strict
   ```
   If required artifacts are missing, blocked, or invalid, report the exact state and repair or clarify the artifacts first. Do not edit implementation files while the change is not apply-ready and strictly valid.

3. **Reconcile artifacts, tasks, and evidence**

   Treat task checkboxes as completion claims, not proof by themselves.
   - Compare pending and checked tasks against the proposal, design, delta specs, current diff, and recorded verification.
   - Preserve incomplete interactive, platform, external-service, and runtime gates across turns.
   - If implementation exposes missing scope, a changed requirement, or a design conflict, update the appropriate artifact first and validate strictly before continuing code edits.
   - Preserve unrelated dirty-worktree changes. Stop for direction only when overlapping edits cannot be continued safely.

   Report the schema, `N/M` task progress, pending task overview, and any evidence contradiction before resuming work.

4. **Continue pending implementation**

   Follow the `openspec-apply-change` skill's pending-task loop for the selected change:
   - Keep edits within the selected change's artifacts and allowed roots.
   - Implement the smallest complete unit of work per task.
   - Run verification proportional to the affected requirement and scenario.
   - Mark a task `- [x]` immediately only when implementation and verification both succeed.
   - Continue until all tasks are complete or a genuine blocker requires user or external action.

   Never infer runtime success from compilation alone. When required evidence is unavailable, leave the task unchecked and state the exact command, platform, credentials, service, or user action still required.

5. **Evaluate completion**

   When no pending implementation remains:
   1. Re-run verification required by `tasks.md` and affected specifications.
   2. Compare the final diff and evidence with every applicable requirement and scenario.
   3. Run strict change validation again.
   4. Use `openspec-sync-specs` when delta specs must update main specs, then validate the main specs.
   5. Report any unchecked task or unverified requirement as incomplete.

   Suggest archival only after all completion gates pass. Archive only when the user explicitly requests it (use `openspec-archive-change`).

**Output format**

On completion or pause, report:
- selected change and schema;
- tasks completed during this continuation;
- overall `N/M` progress;
- verification performed;
- remaining unchecked tasks, blockers, or manual evidence;
- whether strict validation and applicable spec synchronization passed.

**Guardrails**
- Never skip status/instructions/validate calls to save time — stale assumptions about paths or progress cause bad edits.
- Don't re-plan or restructure the change; this skill resumes, it doesn't redesign.
- Pause on errors, blockers, or unclear requirements — don't guess.
