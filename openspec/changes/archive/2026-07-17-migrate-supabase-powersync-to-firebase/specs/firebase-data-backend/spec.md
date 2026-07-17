## ADDED Requirements

### Requirement: Firebase is the sole application backend
Cofinance SHALL use Firebase Authentication, Cloud Firestore, and Firebase Storage for authenticated sessions, finance data, profile metadata, and avatar files without Supabase or PowerSync runtime dependencies.

#### Scenario: Application starts with Firebase configuration
- **WHEN** Android has a valid `google-services.json` or iOS has a valid target-associated `GoogleService-Info.plist`
- **THEN** the platform Firebase SDK SHALL initialize its default app before authentication or Firestore services are used

#### Scenario: Removed backends are inspected
- **WHEN** application dependencies and runtime source are inspected
- **THEN** no Supabase or PowerSync client, synchronization lifecycle, or configuration SHALL remain

### Requirement: Firestore data is isolated by authenticated user
Accounts and transactions SHALL be stored beneath the authenticated user's Firestore document and all application reads and writes SHALL target only that user's subcollections.

#### Scenario: Signed-in user observes accounts
- **WHEN** a signed-in user observes account data
- **THEN** only documents beneath that user's accounts subcollection SHALL be emitted

#### Scenario: No authenticated user performs a finance operation
- **WHEN** an account or transaction operation is requested without a Firebase user
- **THEN** the operation SHALL fail without reading or writing another user's documents

### Requirement: Finance observations preserve existing behavior
Firestore account and transaction observations SHALL preserve existing sorting, date range, draft, transaction identifier, and account hydration behavior expected by repositories.

#### Scenario: Account snapshot changes
- **WHEN** an account document is added, updated, or removed
- **THEN** account observers SHALL receive the current user-scoped list ordered by creation time descending

#### Scenario: Transactions are filtered
- **WHEN** a transaction observer supplies date, draft, or identifier filters
- **THEN** emitted transactions SHALL match those filters, be ordered by date descending, and include sender and receiver account details when present

### Requirement: Balance mutations are atomic
Creating or updating a non-draft transaction and its affected account balances SHALL execute as one Firestore transaction.

#### Scenario: Transfer is created
- **WHEN** a transfer transaction is created
- **THEN** the transaction document, sender debit including fee, and receiver credit SHALL all commit or all fail

#### Scenario: Transaction is updated
- **WHEN** an existing transaction is updated
- **THEN** its old balance impact SHALL be reversed and its new balance impact and document SHALL commit atomically

### Requirement: Firebase profile data preserves Cofinance fields
Firebase Auth and the user's Firestore profile document SHALL together provide the current name, email, avatar URL, cycle start day, and last cycle reset date.

#### Scenario: Google user signs in for the first time
- **WHEN** Firebase accepts a Google credential and no profile document exists
- **THEN** Cofinance SHALL create profile defaults while retaining the Firebase provider name, email, and photo

#### Scenario: User updates an avatar
- **WHEN** a signed-in user saves profile changes with avatar bytes
- **THEN** the bytes SHALL be uploaded to Firebase Storage and the resulting URL SHALL be stored in the user's Firestore profile
