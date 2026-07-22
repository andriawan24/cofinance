## MODIFIED Requirements

### Requirement: Firebase is the sole application backend
Cofinance SHALL use Firebase Authentication, Cloud Firestore, and Firebase Storage as its sole cloud backend for authenticated sessions, optional finance synchronization, profile metadata, and avatar files without Supabase or PowerSync runtime dependencies. Signed-out finance data SHALL remain exclusively in the on-device database.

#### Scenario: Application starts with Firebase configuration
- **WHEN** Android has a valid `google-services.json` or iOS has a valid target-associated `GoogleService-Info.plist`
- **THEN** the platform Firebase SDK SHALL initialize its default app before authentication or Firestore services are used

#### Scenario: Removed backends are inspected
- **WHEN** application dependencies and runtime source are inspected
- **THEN** no Supabase or PowerSync client, synchronization lifecycle, or configuration SHALL remain

#### Scenario: Signed-out user stores finance data
- **WHEN** a user without a Firebase session creates or updates accounts or transactions
- **THEN** the app SHALL NOT write that finance data to a cloud backend

### Requirement: Firestore data is isolated by authenticated user
Cloud-synchronized accounts and transactions SHALL be stored beneath the authenticated user's Firestore document and all cloud synchronization reads and writes SHALL target only that user's subcollections. Local-only finance operations SHALL NOT require a Firebase user.

#### Scenario: Signed-in user synchronizes accounts
- **WHEN** a signed-in user synchronizes account data
- **THEN** only documents beneath that user's accounts subcollection SHALL be read or written

#### Scenario: No authenticated user performs a local finance operation
- **WHEN** an account or transaction operation is requested without a Firebase user
- **THEN** the operation SHALL use only on-device storage and SHALL NOT read or write another user's cloud documents

### Requirement: Finance observations preserve existing behavior
Local account and transaction observations SHALL preserve existing sorting, date range, draft, transaction identifier, and account hydration behavior expected by repositories. Authenticated Firebase synchronization SHALL preserve the same record fields.

#### Scenario: Account data changes locally
- **WHEN** a local account is added, updated, removed, or imported from Firebase
- **THEN** account observers SHALL receive the current list ordered by creation time descending

#### Scenario: Transactions are filtered
- **WHEN** a transaction observer supplies date, draft, or identifier filters
- **THEN** emitted local transactions SHALL match those filters, be ordered by date descending, and include sender and receiver account details when present

### Requirement: Balance mutations are atomic
Creating or updating a non-draft transaction and its affected local account balances SHALL execute as one local database transaction. The resulting records SHALL be mirrored to Firebase for authenticated users.

#### Scenario: Transfer is created
- **WHEN** a transfer transaction is created
- **THEN** the local transaction, sender debit including fee, and receiver credit SHALL all commit or all fail

#### Scenario: Transaction is updated
- **WHEN** an existing transaction is updated
- **THEN** its old local balance impact SHALL be reversed and its new balance impact and transaction record SHALL commit atomically
