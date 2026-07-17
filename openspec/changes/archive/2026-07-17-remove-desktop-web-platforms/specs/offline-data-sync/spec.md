## MODIFIED Requirements

### Requirement: Native targets use a local PowerSync database
Android and iOS SHALL read and write accounts and transactions through the local PowerSync-backed `CofinanceDatabase`.

#### Scenario: Read while offline
- **WHEN** a signed-in native user has previously synchronized data and loses connectivity
- **THEN** account and transaction reads SHALL continue from the local database

#### Scenario: Write while offline
- **WHEN** a signed-in native user creates or updates supported finance data without connectivity
- **THEN** the write SHALL be committed locally and queued for later upload

## REMOVED Requirements

### Requirement: Web targets remain online-only
**Reason**: JS and WasmJS targets were removed from the project; there is no web target left to serve, so the Supabase-backed `OnlineOnlyDatabase` implementation and its dedicated requirement no longer apply.
**Migration**: `OnlineOnlyDatabase` and its now-orphaned Supabase request/response scaffolding were deleted. Android and iOS continue unaffected through `CofinanceDatabase`.
