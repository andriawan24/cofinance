## 1. Create the continuation skill

- [x] 1.1 Initialize the project-local `openspec-continue` skill with generated Codex interface metadata.
- [x] 1.2 Write concise continuation instructions covering change selection, state reconstruction, artifact validation, and working-tree inspection.
- [x] 1.3 Exempt project-local skill interface metadata from the generic `agents/` ignore rule and verify `openai.yaml` is repository-visible.

## 2. Preserve implementation and completion gates

- [x] 2.1 Require pending work to continue through apply-change semantics and update artifacts before coding when scope or design has drifted.
- [x] 2.2 Require proportional verification, explicit incomplete runtime gates, strict validation, applicable spec synchronization, and user-requested archival.

## 3. Verify the workflow

- [x] 3.1 Run the skill validator and confirm the generated interface metadata matches `SKILL.md`.
- [x] 3.2 Review the skill against named, ambiguous, blocked, partially complete, dirty-worktree, and all-done continuation scenarios.
- [x] 3.3 Run strict OpenSpec validation and `git diff --check`, then record final task evidence.
