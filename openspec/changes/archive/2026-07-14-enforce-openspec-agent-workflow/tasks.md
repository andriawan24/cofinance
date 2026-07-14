## 1. Establish the repository policy

- [x] 1.1 Add the mandatory before, during, goal-driven, delegation, and completion gates to root `AGENTS.md`; verify every workflow phase and role is named.
- [x] 1.2 Add the mandatory OpenSpec entry gate to `CLAUDE.md`; verify it delegates authoritative details to `AGENTS.md` without weakening them.

## 2. Add specialized Codex agents

- [x] 2.1 Add project-scoped `openspec-planner`, `openspec-implementer`, and `openspec-verifier` TOML definitions with separated permissions and handoff expectations.
- [x] 2.2 Exempt `.codex/agents/*.toml` from the generic `agents/` ignore rule and verify all definitions appear in Git status.

## 3. Integrate OpenSpec artifact guidance

- [x] 3.1 Add repository architecture, mandatory workflow, goal-completion, and secret-handling context to `openspec/config.yaml`.
- [x] 3.2 Add proposal, design, specification, and task rules that preserve full scope and require proportional verification.

## 4. Verify completion

- [x] 4.1 Parse every custom agent TOML file and verify all required Codex fields are present.
- [x] 4.2 Verify root instruction size remains within Codex's default discovery limit and confirm mandatory workflow coverage by search.
- [x] 4.3 Run strict validation for `enforce-openspec-agent-workflow`, all existing active changes, and all main specs.
- [x] 4.4 Run `git diff --check`, confirm agent files are not ignored, and review the final diff against every workflow requirement.
