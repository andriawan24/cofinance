---
name: openspec-continue
description: Resume implementation of an active OpenSpec change from its current artifacts, tasks, working tree, and verification evidence. Use when work was interrupted, a partially implemented change must continue in a later turn, a goal resumes, or checked tasks and remaining validation need to be reconciled before further edits.
---

# Continue an OpenSpec Change

Resume one existing change without rebuilding its plan or weakening its completion gates.

## 1. Resolve the planning home and change

Read the repository's `AGENTS.md` before acting.

If the user names an OpenSpec store, or the work is in a registered standalone store, run `openspec store list --json` and preserve `--store <id>` on commands that accept it. Otherwise use the nearest local `openspec/` root.

Run `openspec list --json` and resolve exactly one change:

- Use an explicitly named change.
- If no name is given, auto-select only when exactly one active change can govern the request.
- If multiple changes could govern the request, show the active choices and wait for explicit selection.
- Do not create a new change merely because the current one is partially complete.

Announce the selected change and keep its name visible in progress updates and any working plan.

## 2. Reconstruct current state

Run:

```bash
openspec status --change "<name>" --json
openspec instructions apply --change "<name>" --json
```

Use `planningHome`, `changeRoot`, `artifactPaths`, `actionContext`, and `contextFiles` from the JSON instead of assuming paths. Read every returned context file completely. Then inspect `git status --short`, the relevant diff, and existing verification evidence without discarding or overwriting user changes.

Before editing implementation files, run:

```bash
openspec validate "<name>" --strict
```

If required artifacts are missing, blocked, or invalid, report the exact state and repair or clarify the artifacts before implementation. Do not edit implementation files while the change is not apply-ready and strictly valid.

## 3. Reconcile artifacts, tasks, and evidence

Treat task checkboxes as completion claims, not proof by themselves.

- Compare pending and checked tasks with the proposal, design, delta specs, current diff, and recorded verification.
- Preserve incomplete interactive, platform, external-service, and runtime gates across turns.
- If implementation exposes missing scope, a changed requirement, or a design conflict, update the appropriate artifact first and validate strictly before continuing code edits.
- Preserve unrelated dirty-worktree changes. Stop for direction only when overlapping edits cannot be continued safely.

Report the schema, `N/M` task progress, pending task overview, and any evidence contradiction before resuming work.

## 4. Continue pending implementation

Use the `openspec-apply-change` workflow for the selected change's pending task loop.

For each pending task:

1. Keep edits within the selected change's artifacts and allowed roots.
2. Implement the smallest complete unit of work.
3. Run verification proportional to the affected requirement and scenario.
4. Mark the task `- [x]` immediately only when implementation and verification both succeed.
5. Continue until all tasks are complete or a genuine blocker requires user or external action.

Never infer runtime success from compilation alone. When required evidence is unavailable, leave the task unchecked and state the exact command, platform, credentials, service, or user action still required.

## 5. Evaluate completion

When no pending implementation remains:

1. Re-run verification required by `tasks.md` and affected specifications.
2. Compare the final diff and evidence with every applicable requirement and scenario.
3. Run strict change validation again.
4. Use `openspec-sync-specs` when delta specs must update main specs, then validate the main specs.
5. Report any unchecked task or unverified requirement as incomplete.

Suggest archival only after all completion gates pass. Archive only when the user explicitly requests it.

## Result format

On completion or pause, report:

- selected change and schema;
- tasks completed during this continuation;
- overall `N/M` progress;
- verification performed;
- remaining unchecked tasks, blockers, or manual evidence;
- whether strict validation and applicable spec synchronization passed.
