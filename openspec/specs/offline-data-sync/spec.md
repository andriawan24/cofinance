# Offline Data Sync Specification

## Purpose

Define offline-first account and transaction behavior on Android and iOS.

## Requirements

### Requirement: Native targets use a local PowerSync database
Android and iOS SHALL read and write accounts and transactions through the local PowerSync-backed `CofinanceDatabase`.

#### Scenario: Read while offline
- **WHEN** a signed-in native user has previously synchronized data and loses connectivity
- **THEN** account and transaction reads SHALL continue from the local database

#### Scenario: Write while offline
- **WHEN** a signed-in native user creates or updates supported finance data without connectivity
- **THEN** the write SHALL be committed locally and queued for later upload

### Requirement: Repositories depend on a shared database contract
Account and transaction repositories SHALL use `CofinanceDatabase` rather than selecting a platform storage implementation directly.

#### Scenario: Platform database is injected
- **WHEN** the application composition root starts
- **THEN** it SHALL provide the appropriate native or web database implementation through Koin

### Requirement: Synchronization follows authentication state
Native synchronization SHALL connect only for an authenticated session and SHALL disconnect and clear user-scoped local data on logout.

#### Scenario: Authenticated startup
- **WHEN** a valid Supabase session is restored
- **THEN** PowerSync SHALL connect with that session's credentials

#### Scenario: Logout
- **WHEN** the user signs out
- **THEN** synchronization SHALL disconnect and locally cached user data SHALL be cleared

### Requirement: Server writes preserve financial integrity
Uploaded account and transaction mutations SHALL preserve atomic balance and transfer behavior and remain safe to retry.

#### Scenario: Upload is retryable
- **WHEN** a transient network or server failure interrupts upload
- **THEN** the queued mutation SHALL remain pending for retry

#### Scenario: Transfer is uploaded
- **WHEN** a transfer mutation reaches the backend
- **THEN** transaction creation and both balance changes SHALL be applied atomically or not applied

