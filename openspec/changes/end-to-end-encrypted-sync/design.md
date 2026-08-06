## Context

`FirestoreCofinanceDatabase` writes `AccountDocument` and `TransactionDocument` to `users/{uid}/accounts` and `users/{uid}/transactions` as plaintext, and reads them back with server-side `orderBy` on `createdAt` and `date`. `FinanceSyncCoordinator.syncAfterSignIn` merges remote records absent locally, then `mirrorAllIfSignedIn` uploads the entire local snapshot. `getAllTransactions` applies no type filter, so DRAFT and CYCLE_RESET rows are uploaded alongside real transactions.

Three properties of the current code shape this design.

First, Room is the source of truth and holds a complete local copy. Firestore reads happen only in `syncAfterSignIn`. Nothing in the app depends on Firestore's ability to sort, filter, or paginate finance data, so giving up server-side ordering costs nothing functionally.

Second, the app is offline-first and usable with no account. Encryption is only meaningful at the point data would leave the device, which means encryption setup belongs at sign-in, not at first launch. Gating at first launch would force every local-only user to safeguard a recovery phrase that protects nothing, because the local database is deliberately not encrypted by this change.

Third, `mirrorAllIfSignedIn` re-uploads everything on every sync. Under encryption this means re-encrypting the full snapshot each time, which makes nonce discipline a correctness requirement rather than a detail.

Constraints: Android and iOS only, `minSdk` 24. The user base is Indonesia-only and single-device at launch. The stated product decision is that losing both the device and the recovery phrase means losing the data, with no operator-side recovery.

## Goals / Non-Goals

**Goals:**

- The operator cannot read synchronized accounts or transactions, including from a full Firestore export.
- Key material sufficient to decrypt is never transmitted in a form the backend can use.
- A user can move to a new device using only their recovery phrase.
- Encryption is not optional for anyone who syncs, and existing plaintext is migrated away rather than left behind.
- A six-digit PIN is not brute-forceable against material the backend holds.
- Cryptographic composition, envelope handling, and phrase encoding are testable in `commonTest` without a device.
- The local-only experience is unchanged for users who never sign in.

**Non-Goals:**

- Multi-device pairing, key rotation, device revocation. The stored key material accommodates additional wrapped copies so these are additive later.
- Local database encryption. Explicitly deferred; the consequence is stated in the risks.
- Encryption of profile metadata, avatars, or cycle settings.
- Metadata privacy. Record counts, ciphertext sizes, and write timing remain visible to the backend.
- Any operator-side recovery, escrow, or reset path. Adding one would reintroduce the operator into the trust boundary and defeat the change.

## Decisions

### Decision 1: Symmetric data key wrapped by asymmetric and derived keys, not direct asymmetric encryption

Records are encrypted with a single 32-byte data encryption key under AES-256-GCM. That key is wrapped independently by:

- a device key held in Android Keystore or the iOS Keychain and Secure Enclave, non-extractable, used for ordinary unlock;
- a key derived from the user's 12-word recovery phrase, used for restore on a new device.

Public-key cryptography appears only in the wrapping layer, not in bulk record encryption. Encrypting records directly to a public key was rejected: asymmetric primitives are size-limited and slow for per-record data, and every practical end-to-end encrypted system uses this envelope shape for the same reason. The wrapping layer is what makes it possible to add a second device later without re-encrypting a single record.

Stored key material is a list of wrapped copies rather than a single value, even though only two entries exist at launch. This is the whole reason a future multi-device change is additive.

Only the recovery-phrase wrap is written to Firestore. The device wrap is device-bound and useless elsewhere, so uploading it would add attack surface for no benefit.

### Decision 2: Device keys are P-256, because that is what the hardware supports

The Secure Enclave supports P-256 only, and Android StrongBox's hardware-backed elliptic curve support is similarly constrained. Curve25519 would be the more conventional choice for a greenfield protocol but cannot be hardware-sealed on iOS. Hardware sealing is the property that makes "only their device can open it" true against an attacker who has the filesystem, so the curve follows the hardware: P-256 ECDH, with the shared secret run through HKDF to produce the wrapping key.

StrongBox requires API 28 and the floor is 24, so hardware-backed Keystore is the baseline and StrongBox is used opportunistically when the device reports it.

### Decision 3: The PIN is bound to a device secret

A six-digit PIN has 10^6 possible values. The recovery-phrase wrap is stored in Firestore, so an attacker with backend access must be assumed to hold ciphertext. If the PIN alone derived a wrapping key, any memory-hard derivation would still fall to exhaustive search over a million candidates.

The PIN-derived key is therefore composed with a non-extractable secret held in Keystore or the Keychain, so that deriving it requires possession of the device in addition to knowledge of the PIN. A memory-hard derivation is applied to the PIN before composition to raise the cost of on-device guessing. The PIN-wrapped copy of the data key is stored locally only and is never uploaded, so backend material alone reveals nothing that a PIN could unlock.

Failed attempts are counted in secure storage with escalating delay, and the counter's persistence is tied to the device key's lifetime so that reinstalling the app does not reset it. After a configured number of failures the local key material is destroyed; the data remains recoverable through the recovery phrase, which is the point of having two independent wraps.

Alternatives considered. Relying on the OS passcode via Keystore's user-authentication requirement alone was rejected because it offers no in-app lock for a device whose owner shares it unlocked, which is the threat the user asked to address. Storing a PIN hash for comparison was rejected outright: comparison-based unlock leaves the data key accessible to any code path that skips the comparison, whereas derivation makes the PIN structurally necessary.

### Decision 4: PIN is required, biometric is an optional shortcut over it

Enabling biometric requires a PIN to already be set. The device key is created with a biometric-invalidating access policy, so enrolling a new fingerprint or face destroys it — correct behavior, and a hard failure if biometric is the only path. With a PIN as the floor, that event degrades to re-entering six digits instead of forcing a 12-word restore.

Both toggles live in the profile page, alongside an auto-lock timeout. The decrypted data key is held in memory only and is zeroed when the app backgrounds past the timeout, so lock state is a property of held key material rather than a screen the user might navigate around.

### Decision 5: Encryption setup is gated at sign-in, and is mandatory there

New users completing sign-in generate a data key and a 12-word phrase, must confirm the phrase by re-entering words drawn from it, and cannot reach synchronization until they do. Users with existing plaintext data enter a blocking migration flow at next launch.

Gating at first launch was rejected: it would impose phrase custody on local-only users to protect data that never leaves their device and that this change deliberately leaves unencrypted at rest.

12 words encode 128 bits, which matches the security level of the wrapping and is materially easier to transcribe than 24. The additional entropy of 24 words protects nothing that 128 bits does not.

### Decision 6: Migration converts documents individually, encrypt-then-delete

Migration is per document and idempotent: write the encrypted fields, then remove the plaintext fields, then move on. Never batch-delete plaintext ahead of writing ciphertext, because a process death between those steps would destroy data. A document is identifiable as migrated by the presence of the envelope version field, so an interrupted migration resumes by scanning for documents lacking it.

Migration requires network access and is blocking. A user who never opens the app again leaves plaintext in Firestore indefinitely; this is a known residue, called out in the risks rather than silently accepted.

### Decision 7: The envelope carries a version and a key identifier, and ordering moves to the client

Each encrypted record stores an envelope version, the identifier of the key that encrypted it, a per-record nonce, and the ciphertext. The document identifier stays plaintext because it is a random identifier the app generates and is required to address the document.

Because `mirrorAllIfSignedIn` re-encrypts the full snapshot on every sync, a fresh random nonce is generated per encryption operation, never derived from record content or a counter. Nonce reuse under GCM is catastrophic rather than degrading, so this is a correctness requirement with an explicit test.

Server-side `orderBy` on `createdAt` and `date` is removed, since those fields no longer exist in plaintext. Records are ordered locally from Room, which is where every user-facing ordering already comes from.

`updatedAt` is also encrypted. Retaining it in plaintext would enable incremental sync, but the current coordinator does not perform incremental sync, and leaving it exposed would leak the user's app-usage cadence for no present benefit. If incremental sync is added later, exposing `updatedAt` becomes a deliberate trade to reconsider then.

### Decision 8: Verification evidence per platform

- `commonTest` covers envelope round-tripping, nonce uniqueness across repeated encryptions of identical input, phrase generation and restoration, wrap and unwrap for both wrap types, rejection of tampered ciphertext, and the migration state machine driven by a fake remote source.
- Android: an instrumented check that the device key is hardware-backed and non-extractable, that a biometric-invalidating key is destroyed by simulated enrollment change, and that the failed-attempt counter survives app data clearing as designed.
- iOS: an `iosSimulatorArm64` check of Keychain access-control attributes and key non-extractability, with the Secure Enclave path exercised on device where the simulator cannot represent it.
- Both: a manual end-to-end pass — sign in, set up encryption, record a transaction, confirm through the Firebase console that the stored document contains no readable finance values, then restore onto a clean install using only the recovery phrase and confirm the data returns.
- A migration pass against a project seeded with plaintext documents, including one interrupted mid-run and resumed.

## Risks / Trade-offs

- **A user loses their device and their recovery phrase, and their data is unrecoverable.** → This is the accepted product decision and the direct cost of removing the operator from the trust boundary. Mitigated only by making phrase custody prominent: mandatory confirmation at setup, and a re-display path in settings behind the app lock. No operator recovery path is added, because adding one would negate the change.
- **The local Room database remains plaintext, so the app lock protects less than users may assume.** → Platform disk encryption protects a locked device, which is the common loss scenario. The lock protects against someone handed an unlocked phone. Both statements are true and neither is "your local data is encrypted"; the user-facing copy must not overclaim. Encrypting Room is a candidate follow-up change.
- **The operator ships the client, so a future build could exfiltrate the data key.** → Unavoidable for any client-side encryption where the operator controls distribution. Mitigation is verifiability rather than mechanism: keeping the crypto module small, self-contained, and open to inspection. Worth stating honestly in user-facing copy rather than implying a stronger guarantee.
- **Migration leaves plaintext behind for users who never return.** → Documented residue. A backend-side cleanup after a defined period is possible but is an operator action outside this change; the alternative of deleting unmigrated data would destroy the records of returning users.
- **Nonce reuse under AES-GCM would be catastrophic, and the full-snapshot mirror re-encrypts constantly.** → Fresh random nonce per operation, never content-derived or counter-derived, with a `commonTest` asserting that encrypting identical input twice yields different nonces.
- **Losing the wrapped key material document makes all synchronized records undecryptable.** → The device holds its own wrap independently, so the Firestore document is not the sole copy for the active device, and the phrase reconstructs the wrap on restore. Key material is written before the first encrypted record.
- **Removing server-side ordering degrades a future feature that wants server-side queries.** → Accepted. No such feature exists, Room already answers every ordering question, and any future server-side query over encrypted data would require a fundamentally different design regardless.
- **Biometric enrollment change destroys the device key mid-use.** → Requiring a PIN before biometric makes this a six-digit recovery rather than a phrase restore. Covered by an explicit platform test.
- **Mandatory setup at sign-in adds friction and may reduce sign-in completion.** → Deliberate: the alternative is optional encryption, which means maintaining a plaintext sync path forever and being unable to make the privacy claim at all.

## Migration Plan

Deployment order within the change: key material handling ships before any write path is switched, so that no encrypted record can be written before its key material exists.

For users:

1. Existing signed-in users with plaintext documents meet a blocking setup and migration flow at next launch, requiring network.
2. New users meet setup at sign-in.
3. Local-only users are unaffected until they sign in.

Rollback is constrained and must be understood before release. Once records are encrypted, reverting the app does not restore readability, because a reverted client cannot decrypt them and the plaintext fields are gone. Rollback is therefore only safe before migration runs for a given user. This asymmetry argues for a staged release and for treating the migration step as the point of no return.

Operator follow-up outside this repository: purge or age out any operator-held backups or exports containing plaintext finance data, since encrypting the live collection does not reach copies made earlier.

## Open Questions

- What is the failed-PIN-attempt threshold before local key material is destroyed, and should destruction be automatic or user-configurable?
- Should the recovery phrase be re-displayable from settings behind the app lock, or shown only once at setup? Re-display is better for users who lose a written copy; show-once is stronger against someone with a briefly unlocked device.
- Should export produce a password-protected file in addition to the phrase-based restore, for users who want an offline copy independent of the backend?
- What is the auto-lock timeout default, and which options are offered?
- Should DRAFT and CYCLE_RESET rows continue to be uploaded at all? They are uploaded today and are therefore in scope for encryption, but excluding drafts from synchronization entirely would reduce both cost and exposure. That is a synchronization-scope question, separable from this change.
