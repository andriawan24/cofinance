## MODIFIED Requirements

### Requirement: Repositories depend on a shared database contract
Account and transaction repositories SHALL use durable local `CofinanceDatabase` storage as their source of truth, while authenticated cloud mirroring SHALL use a separate Firebase synchronization boundary.

#### Scenario: Local database is injected
- **WHEN** the application composition root starts
- **THEN** it SHALL provide the shared Room-backed local database implementation through Koin

#### Scenario: No authenticated user performs a finance operation
- **WHEN** an account or transaction operation is requested without a Firebase user
- **THEN** it SHALL read or write the local database without requiring Firebase

## ADDED Requirements

### Requirement: Sign-in synchronizes local and remote finance data
After successful authentication, Cofinance SHALL merge the signed-in user's remote accounts and transactions into local storage and upload the complete merged local snapshot to that user's Firebase collections.

#### Scenario: Local-only data exists before first sign-in
- **WHEN** a user signs in with locally stored accounts or transactions
- **THEN** those records SHALL be uploaded beneath the authenticated user's Firestore document

#### Scenario: Cloud data exists before sign-in
- **WHEN** the authenticated user's Firestore collections contain records absent locally
- **THEN** those records SHALL be imported into local storage and exposed through repository observations

#### Scenario: Same identifier exists locally and remotely
- **WHEN** initial synchronization finds different records with the same identifier
- **THEN** the local record SHALL win deterministically

### Requirement: Authenticated mutations are mirrored without sacrificing local durability
Finance mutations SHALL commit to local storage first and SHALL be mirrored to the authenticated user's Firebase collections when a session is active.

#### Scenario: Signed-in mutation succeeds locally
- **WHEN** a signed-in user creates or updates a finance record
- **THEN** the local result SHALL be available immediately
- **AND** the same record SHALL be sent to the authenticated user's Firebase collection

#### Scenario: Cloud mirroring is unavailable
- **WHEN** a local mutation succeeds but its Firebase mirror fails
- **THEN** the local mutation SHALL remain durable
- **AND** a later successful sign-in synchronization SHALL retry the local snapshot

