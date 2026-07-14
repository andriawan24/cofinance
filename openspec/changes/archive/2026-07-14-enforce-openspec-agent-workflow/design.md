## Context

The repository already includes OpenSpec skills and commands for Codex, Claude, and OpenCode, but their use is optional unless a request explicitly names them. Persistent goal continuations and generic implementation requests can therefore bypass proposal artifacts, perform work from a chat-only plan, or claim completion from partial evidence.

Codex officially discovers durable project guidance from root `AGENTS.md` and project-scoped custom agents from `.codex/agents/*.toml`. OpenSpec also injects `openspec/config.yaml` context and rules into artifact instructions. These three layers provide a durable policy, bounded delegated roles, and workflow-native artifact constraints.

## Goals / Non-Goals

**Goals:**

- Require OpenSpec for every behavior-changing implementation regardless of entry point.
- Make goal creation, resumption, continuation, and completion follow the same gates.
- Separate planning, implementation, and independent verification responsibilities.
- Ensure every repository contributor can receive the policy from repository files eligible for Git tracking.
- Preserve OpenSpec as the only implementation planning source of truth.

**Non-Goals:**

- Automatically archive changes without user authorization.
- Require OpenSpec for read-only exploration or purely editorial corrections.
- Introduce a shell hook that attempts to infer whether every file edit belongs to a change.
- Change application runtime behavior or dependencies.

## Decisions

### Root `AGENTS.md` is the authoritative enforcement layer

All Codex agents read root repository guidance, including built-in and spawned agents. The policy defines implementation scope, before/during/completion gates, goal integration, delegation requirements, and supported role names. This is preferred over relying only on a skill because skills are task-triggered, while repository instructions are durable defaults.

Alternative considered: encode the workflow only in custom agents. Rejected because the built-in worker and root agent could still implement without selecting those roles.

### Use three project-scoped Codex agents

`openspec-planner` may investigate and write artifacts but not implementation. `openspec-implementer` may work only from one validated apply-ready change. `openspec-verifier` independently audits evidence and cannot fix code or complete the parent goal. Responsibility separation reduces self-verification bias and makes delegated prompts predictable.

Alternative considered: one general OpenSpec agent. Rejected because it would combine proposal authorship, implementation, and completion judgment.

### Repeat the entry gate in `CLAUDE.md` and OpenSpec config

Claude has its own repository guidance entry point, so it receives a concise mandatory reference to `AGENTS.md`. OpenSpec context and artifact-specific rules preserve full goal scope and verification requirements whenever artifact instructions are generated. OpenCode-compatible tooling can consume root `AGENTS.md` and the existing OpenSpec commands/skills.

Alternative considered: duplicate the entire policy in every tool directory. Rejected because duplicated full policies drift; only tool-specific adapters should repeat the essential entry gate.

### Completion is evidence-based and archival remains explicit

Task checkboxes, a clean build, or partial success are insufficient. Completion requires task, requirement, test/build, strict validation, and applicable sync evidence. Archival remains a separate explicit finalization action because it changes the lifecycle and may require a user choice about spec synchronization.

## Risks / Trade-offs

- **Risk:** Small implementation requests incur artifact overhead. → **Mitigation:** Keep artifacts proportional while preserving the gate; editorial-only work is exempt.
- **Risk:** Agents loaded before these files changed may retain stale instructions. → **Mitigation:** Codex reloads repository instructions and project agents on a new task/session; document that activation boundary.
- **Risk:** Instruction-only enforcement can still be violated by non-compliant tooling. → **Mitigation:** Keep OpenSpec checks explicit in every role and use independent verification; consider a future hook only if a reliable change association signal is designed.
- **Risk:** Multiple active changes make automatic selection unsafe. → **Mitigation:** Require explicit user selection rather than guessing.

## Migration Plan

1. Add the mandatory workflow to root and tool-specific repository guidance.
2. Add planner, implementer, and verifier definitions under `.codex/agents` and make them eligible for Git tracking.
3. Add OpenSpec project context and artifact rules.
4. Validate TOML, Git tracking, OpenSpec artifacts, existing changes, and main specs.
5. Start a new Codex task/session when testing agent discovery because instruction chains are assembled at startup.

Rollback consists of reverting the instruction, agent, OpenSpec config, and ignore-rule changes; application code is unaffected.

## Open Questions

None. A mechanical hook may be evaluated later if policy compliance proves insufficient, but it is not required for this change.
