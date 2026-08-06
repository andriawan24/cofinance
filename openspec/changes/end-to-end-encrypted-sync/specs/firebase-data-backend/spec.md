## MODIFIED Requirements

### Requirement: Finance observations preserve existing behavior
Local account and transaction observations SHALL preserve existing sorting, date range, draft, transaction identifier, and account hydration behavior expected by repositories. Authenticated Firebase synchronization SHALL preserve the same record fields after local decryption, while the stored cloud documents SHALL carry only encrypted payloads and non-finance envelope metadata. Ordering of synchronized records SHALL be performed locally rather than by the backend.

#### Scenario: Account data changes locally
- **WHEN** a local account is added, updated, removed, or imported from Firebase
- **THEN** account observers SHALL receive the current list ordered by creation time descending

#### Scenario: Transactions are filtered
- **WHEN** a transaction observer supplies date, draft, or identifier filters
- **THEN** emitted local transactions SHALL match those filters, be ordered by date descending, and include sender and receiver account details when present

#### Scenario: Synchronized record round-trips through the backend
- **WHEN** a finance record is uploaded and later imported on a device holding the data key
- **THEN** the decrypted record SHALL carry the same field values that were uploaded

#### Scenario: Cloud finance reads request no server ordering
- **WHEN** synchronized accounts or transactions are read from Firestore
- **THEN** the query SHALL NOT order by a finance field, and ordering SHALL be applied locally

## ADDED Requirements

### Requirement: Firestore finance documents carry no readable finance fields
Cloud-synchronized account and transaction documents SHALL contain only the document identifier, encryption envelope metadata, and encrypted payload.

#### Scenario: Stored finance document is inspected in the backend console
- **WHEN** a synchronized account or transaction document is inspected directly in the backend
- **THEN** no amount, balance, category, date, notes, name, group, or account type SHALL be readable

#### Scenario: Envelope metadata is present
- **WHEN** a synchronized finance document is inspected
- **THEN** it SHALL carry an envelope version, a key identifier, a nonce, and an encrypted payload

### Requirement: Per-user key material is stored beneath the authenticated user's document
Cofinance SHALL store the recovery-phrase wrapped copy of the data key beneath the authenticated user's Firestore document, subject to the same per-user isolation as finance data.

#### Scenario: Key material is written
- **WHEN** encryption setup completes for a signed-in user
- **THEN** the wrapped key material SHALL be written beneath that user's document only

#### Scenario: Key material is read during restore
- **WHEN** a user restores on a new device
- **THEN** only that user's key material SHALL be read
