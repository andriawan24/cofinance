## ADDED Requirements

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
