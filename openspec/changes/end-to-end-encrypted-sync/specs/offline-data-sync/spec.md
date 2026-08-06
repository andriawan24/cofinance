## MODIFIED Requirements

### Requirement: Sign-in synchronizes local and remote finance data
After successful authentication and completed encryption setup, Cofinance SHALL merge the signed-in user's remote accounts and transactions into local storage by decrypting them, and SHALL upload the complete merged local snapshot in encrypted form to that user's Firebase collections.

#### Scenario: Local-only data exists before first sign-in
- **WHEN** a user signs in with locally stored accounts or transactions and completes encryption setup
- **THEN** those records SHALL be encrypted and uploaded beneath the authenticated user's Firestore document

#### Scenario: Cloud data exists before sign-in
- **WHEN** the authenticated user's Firestore collections contain records absent locally
- **THEN** those records SHALL be decrypted and imported into local storage and exposed through repository observations

#### Scenario: Same identifier exists locally and remotely
- **WHEN** initial synchronization finds different records with the same identifier
- **THEN** the local record SHALL win deterministically

#### Scenario: Encryption setup is incomplete at sign-in
- **WHEN** synchronization is reached before encryption setup has completed
- **THEN** no finance data SHALL be uploaded or imported

### Requirement: Authenticated mutations are mirrored without sacrificing local durability
Finance mutations SHALL commit to local storage first and SHALL be mirrored in encrypted form to the authenticated user's Firebase collections when a session is active and the data key is available.

#### Scenario: Signed-in mutation succeeds locally
- **WHEN** a signed-in user with an available data key creates or updates a finance record
- **THEN** the local result SHALL be available immediately
- **AND** the same record SHALL be encrypted and sent to the authenticated user's Firebase collection

#### Scenario: Cloud mirroring is unavailable
- **WHEN** a local mutation succeeds but its Firebase mirror fails
- **THEN** the local mutation SHALL remain durable
- **AND** a later successful sign-in synchronization SHALL retry the local snapshot

#### Scenario: Data key is unavailable while locked
- **WHEN** a local mutation succeeds while the app is locked and the data key has been cleared from memory
- **THEN** the local mutation SHALL remain durable
- **AND** no plaintext finance data SHALL be uploaded
- **AND** the record SHALL be mirrored after the next successful unlock
