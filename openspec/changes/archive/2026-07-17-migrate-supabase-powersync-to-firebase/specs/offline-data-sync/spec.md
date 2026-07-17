## REMOVED Requirements

### Requirement: Native targets use a local PowerSync database
**Reason**: PowerSync and the app-managed local synchronization layer are being removed in favor of direct Cloud Firestore operations.

**Migration**: Account and transaction reads and writes use the Firestore-backed `CofinanceDatabase`; no explicit offline-first guarantee remains.

### Requirement: Synchronization follows authentication state
**Reason**: Firebase Authentication and Firestore SDKs own their session and connection lifecycle, so the application no longer connects, pauses, resumes, or clears PowerSync.

**Migration**: Login establishes Firebase Authentication only, startup checks the restored Firebase user, and logout signs out through Firebase Authentication.

### Requirement: Server writes preserve financial integrity
**Reason**: The PowerSync upload queue and backend RPC behavior are removed.

**Migration**: The replacement `firebase-data-backend` capability requires transaction documents and related account balances to commit atomically through Firestore transactions.

## MODIFIED Requirements

### Requirement: Repositories depend on a shared database contract
Account and transaction repositories SHALL use `CofinanceDatabase` rather than selecting a platform storage implementation directly.

#### Scenario: Firebase database is injected
- **WHEN** the application composition root starts
- **THEN** it SHALL provide the shared Firestore-backed database implementation through Koin
