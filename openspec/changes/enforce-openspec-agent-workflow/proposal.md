## Why

Implementation requests and persistent goals can currently begin without a durable contract tying agent edits, delegated work, task status, and completion claims to OpenSpec. A repository-wide workflow and specialized agents are needed so every behavioral change follows the same proposal-to-verification lifecycle.

## What Changes

- Make OpenSpec mandatory for every implementation change performed by root agents, built-in workers, custom agents, subagents, and goal continuations.
- Require one named, apply-ready, strictly valid OpenSpec change before implementation files may be edited.
- Add explicit goal completion gates covering tasks, requirements, tests/builds, strict validation, and applicable delta-spec synchronization.
- Add project-scoped Codex planner, implementer, and verifier agents with separated responsibilities.
- Add equivalent Claude entry-point guidance and embed workflow context and artifact rules in OpenSpec configuration.
- Ensure project-scoped Codex agent definitions are not ignored and are eligible for Git tracking.

## Capabilities

### New Capabilities

- `agent-openspec-workflow`: Defines mandatory planning, implementation, delegation, goal-continuation, and completion behavior for repository agents.

### Modified Capabilities

None.

## Impact

This affects repository agent instructions, Codex custom-agent configuration, Claude guidance, OpenSpec artifact generation rules, and Git ignore behavior. It does not change Cofinance application runtime behavior.
