# Agent OpenSpec Workflow Specification

## Purpose

Define the mandatory OpenSpec lifecycle, delegated roles, goal integration, and evidence gates for every repository implementation agent.

## Requirements

### Requirement: OpenSpec gates every implementation change
Repository agents SHALL associate every implementation change with exactly one named, apply-ready OpenSpec change before editing implementation files.

#### Scenario: Matching change exists
- **WHEN** an agent receives an implementation request that matches one active OpenSpec change
- **THEN** the agent SHALL resolve its status and apply instructions, read its context artifacts, and validate it strictly before editing implementation files

#### Scenario: No matching change exists
- **WHEN** an implementation request has no matching OpenSpec change
- **THEN** the agent SHALL create the required proposal, specifications, design, and tasks and SHALL NOT edit implementation files until the change is apply-ready and strictly valid

#### Scenario: Multiple changes could match
- **WHEN** more than one active OpenSpec change could govern an implementation request
- **THEN** the agent SHALL ask the user to select the change and SHALL NOT guess or begin implementation

### Requirement: Implementation follows change artifacts
Repository agents SHALL implement only work represented by the selected change's proposal, design, specifications, and tasks.

#### Scenario: Task is implemented and verified
- **WHEN** an agent completes the implementation described by a task and its proportional verification succeeds
- **THEN** the agent SHALL mark that task complete and record the verification outcome

#### Scenario: Implementation reveals an artifact gap
- **WHEN** implementation reveals missing scope, a missing requirement, or a design conflict
- **THEN** the agent SHALL update and strictly validate the appropriate OpenSpec artifact before continuing implementation

### Requirement: Goal-driven implementation preserves full scope
Agents SHALL map the full objective of every created, resumed, or automatically continued goal to its governing OpenSpec change and SHALL retain the change name in the working plan.

#### Scenario: Goal is resumed
- **WHEN** an implementation goal resumes in a later turn
- **THEN** the agent SHALL re-resolve the governing change and continue from its current artifacts and task state rather than creating an independent plan

#### Scenario: Goal completion is evaluated
- **WHEN** an agent considers marking a goal complete
- **THEN** it SHALL require all in-scope tasks complete, requirement-by-requirement evidence, relevant tests and builds passing, strict change validation passing, and applicable delta specs synchronized

### Requirement: Delegated agents retain OpenSpec context
Every delegated implementation agent SHALL receive the governing change name and SHALL independently read repository instructions, change status, apply instructions, and returned context files before editing.

#### Scenario: Implementation is delegated
- **WHEN** the root agent delegates a bounded implementation task
- **THEN** the delegated prompt SHALL identify the governing OpenSpec change and the subagent SHALL remain within its artifacts

### Requirement: Specialized Codex roles separate responsibilities
The repository SHALL provide project-scoped OpenSpec planner, implementer, and verifier roles with distinct responsibilities.

#### Scenario: Planner is selected
- **WHEN** the planner handles a request
- **THEN** it SHALL create or refine OpenSpec artifacts without editing implementation files

#### Scenario: Implementer is selected
- **WHEN** the implementer handles a request
- **THEN** it SHALL edit only from one named, apply-ready change and report task and verification status

#### Scenario: Verifier is selected
- **WHEN** the verifier audits completion
- **THEN** it SHALL independently inspect artifacts, implementation, and evidence without implementing fixes or marking the parent goal complete

### Requirement: Agent workflow configuration is repository-durable
The mandatory workflow and project-scoped agent definitions SHALL be stored in repository-scoped files, excluded from ignore rules, and available to supported agent entry points.

#### Scenario: Codex starts a new repository task
- **WHEN** Codex discovers root instructions and project agent definitions
- **THEN** it SHALL load the mandatory workflow from `AGENTS.md` and make the `.codex/agents` roles available for delegation

#### Scenario: OpenSpec generates artifacts
- **WHEN** OpenSpec returns artifact instructions
- **THEN** the project context and artifact rules SHALL include the mandatory goal, verification, and scope-preservation constraints

### Requirement: Agents can safely continue an active OpenSpec change
The repository SHALL provide a project-local continuation workflow that reconstructs one active change's current planning, implementation, task, repository, and verification state before resuming edits.

#### Scenario: One change is explicitly named
- **WHEN** a user invokes continuation with an active change name
- **THEN** the workflow SHALL load its status, apply instructions, every returned context artifact, current tasks, and working-tree state before continuing implementation

#### Scenario: Change selection is ambiguous
- **WHEN** no change is named and more than one active change could govern the request
- **THEN** the workflow SHALL list active changes and require explicit user selection before editing implementation files

#### Scenario: Change cannot be applied
- **WHEN** required artifacts are missing, invalid, or blocked
- **THEN** the workflow SHALL report the blocking state and SHALL NOT edit implementation files until the artifacts are apply-ready and strictly valid

### Requirement: Continued implementation preserves evidence gates
The continuation workflow SHALL execute only pending work represented in the selected change and SHALL preserve incomplete verification or runtime gates across turns.

#### Scenario: Pending implementation remains
- **WHEN** the selected change is apply-ready and has pending tasks
- **THEN** the workflow SHALL continue through the existing apply-change process, mark tasks complete only after proportional verification, and update artifacts before coding if scope or design has drifted

#### Scenario: Automated checks cannot prove a runtime requirement
- **WHEN** a task requires an interactive platform, configured external service, or other unavailable runtime evidence
- **THEN** the workflow SHALL leave that task incomplete and report the exact validation still required

#### Scenario: All work appears complete
- **WHEN** no pending tasks remain
- **THEN** the workflow SHALL compare the final diff and evidence with every applicable requirement, run strict validation, synchronize delta specs when applicable, and suggest archival without archiving automatically
