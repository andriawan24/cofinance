## 1. Cryptographic foundation

- [ ] 1.1 Add a Kotlin Multiplatform cryptography dependency providing AES-256-GCM, HKDF, ECDH over P-256, and a memory-hard password derivation across Android and Apple targets, and add a bundled BIP39 wordlist resource. Verify: both targets compile and a round-trip AES-GCM test passes in `commonTest`.
- [ ] 1.2 Implement the record envelope in `commonMain`: version, key identifier, nonce, ciphertext, with serialization to and from the stored document shape. Verify: `commonTest` round-trips a record and asserts the envelope carries all four elements.
- [ ] 1.3 Implement encryption and decryption of `AccountResponse` and `TransactionResponse` payloads against the envelope. Verify: `commonTest` asserts a decrypted record equals the original and that a modified ciphertext or envelope fails decryption without importing anything.
- [ ] 1.4 Implement fresh random nonce generation per operation, with no content or counter derivation. Verify: `commonTest` encrypts identical input twice and asserts the nonces differ.

## 2. Key hierarchy and platform key storage

- [ ] 2.1 Define the key material model as a collection of wrapped copies, each tagged by wrap type, with the data key never represented unwrapped in any stored form. Verify: `commonTest` asserts the serialized model contains no unwrapped key and accepts more than two entries.
- [ ] 2.2 Implement the `expect` device key vault interface for creating a non-extractable P-256 key, performing ECDH, and holding a device secret. Verify: `commonMain` compiles and a fake implementation satisfies the interface for common tests.
- [ ] 2.3 Implement the Android `actual` over Android Keystore, using StrongBox when the device reports it and hardware-backed Keystore otherwise. Verify: an instrumented test asserts the key is hardware-backed and that private key export fails.
- [ ] 2.4 Implement the iOS `actual` over the Keychain and Secure Enclave, restricted to this device and non-extractable. Verify: an `iosSimulatorArm64` test asserts the access-control attributes and export failure, with the Secure Enclave path exercised on a physical device where the simulator cannot represent it.
- [ ] 2.5 Implement wrapping and unwrapping of the data key by the device key via ECDH plus HKDF. Verify: `commonTest` covers wrap then unwrap, and asserts unwrapping fails with a different device key.

## 3. Recovery phrase

- [ ] 3.1 Implement 12-word BIP39 phrase generation with at least 128 bits of entropy, plus checksum validation on entry. Verify: `commonTest` covers generation, valid-phrase acceptance, and rejection of an altered word.
- [ ] 3.2 Implement derivation of the phrase wrapping key and wrapping and unwrapping of the data key with it. Verify: `commonTest` covers wrap then unwrap and asserts a wrong phrase fails to unwrap.
- [ ] 3.3 Build the setup screens: phrase display, the consequence-of-loss statement, and mandatory confirmation by re-entering requested words. Verify: setup cannot complete and synchronization cannot begin until the words are re-entered correctly.
- [ ] 3.4 Build the restore flow for a device holding no key material: read cloud key material, unwrap with the entered phrase, decrypt and import records, then create a device-bound wrapped copy. Verify: a clean-install restore test imports records, and an invalid phrase imports nothing and permits retry.

## 4. Encrypted synchronization

- [ ] 4.1 Change `FirestoreCofinanceDatabase` to write encrypted documents for accounts and transactions and to decrypt on read. Verify: a fake-backed test asserts stored documents carry no readable finance fields and that a read returns the original values.
- [ ] 4.2 Remove the `orderBy` calls on `createdAt` and `date` from the Firestore finance reads and confirm ordering is applied locally. Verify: existing ordering tests over Room observations still pass and no finance-field ordering remains in the Firestore queries.
- [ ] 4.3 Store the recovery-phrase wrapped key material beneath the authenticated user's document, written before the first encrypted record. Verify: a test asserts key material exists prior to any encrypted record write and that only the phrase wrap is uploaded.
- [ ] 4.4 Gate `FinanceSyncCoordinator.syncAfterSignIn` and `mirrorAllIfSignedIn` on completed encryption setup and an available data key, keeping local mutations durable when the key is unavailable. Verify: tests cover setup-incomplete, locked, and unlocked paths, asserting no plaintext upload in the first two and a mirror after the next unlock.
- [ ] 4.5 Confirm DRAFT and CYCLE_RESET rows reaching the mirror through `getAllTransactions` are encrypted on the same terms. Verify: a test uploads a draft and asserts its stored document carries no readable finance fields.

## 5. Migration of existing plaintext records

- [ ] 5.1 Implement detection of unmigrated records by absence of the envelope version. Verify: `commonTest` distinguishes plaintext from encrypted documents in a mixed fake collection.
- [ ] 5.2 Implement per-record conversion that writes encrypted fields before removing plaintext fields, and is idempotent per record. Verify: `commonTest` asserts write-then-delete ordering and that re-running over a converted record is a no-op.
- [ ] 5.3 Implement the blocking, resumable migration flow for signed-in users with plaintext data, including setup of key material if absent. Verify: a test interrupts migration mid-run, relaunches, and asserts only still-plaintext records are converted and that the run completes.
- [ ] 5.4 Verify migration completion leaves no plaintext finance fields. Verify: a post-migration scan over a seeded fake collection asserts every account and transaction document carries an envelope and no readable finance values.

## 6. App lock

- [ ] 6.1 Implement PIN-derived key material composed with the device-held secret, using a memory-hard derivation before composition, and store the PIN-wrapped copy locally only. Verify: `commonTest` asserts unlock is derivation-based rather than comparison-based, and that the PIN wrap plus correct PIN yields no key under a different device secret.
- [ ] 6.2 Implement the failed-attempt counter in secure storage with escalating delay, destruction of local key material at the threshold, reset on success, and persistence across reinstall. Verify: platform tests cover escalation, threshold destruction, reset, reinstall persistence, and that phrase restoration still works after destruction.
- [ ] 6.3 Implement biometric unlock as a shortcut requiring a PIN, with a biometric-invalidating key access policy and PIN fallback. Verify: platform tests cover enabling without a PIN being refused, successful biometric unlock, cancellation falling back to PIN, and a simulated enrollment change leaving the PIN path working.
- [ ] 6.4 Implement in-memory-only data key handling with clearing on background past the auto-lock timeout, and unlock required before finance data is displayed. Verify: tests cover backgrounding past and within the timeout, and assert no unwrapped key is persisted outside process memory while unlocked.
- [ ] 6.5 Build the profile security section for setting and changing the PIN, toggling biometric, and choosing the auto-lock timeout, requiring the current PIN for changes. Verify: UI tests cover each control and assert the current-PIN requirement.
- [ ] 6.6 Confirm no unlock screen is presented to a user who has not completed encryption setup. Verify: a local-only launch test asserts direct entry to the main experience.

## 7. Verification and specification closure

- [ ] 7.1 Run the full `commonTest` suite plus the Android and iOS platform checks from groups 2 and 6. Verify: all suites pass on both targets.
- [ ] 7.2 Perform a manual end-to-end pass: sign in, complete setup, record a transaction, then inspect the stored document directly in the backend console. Verify: no finance value is readable.
- [ ] 7.3 Perform a manual restore pass on a clean install using only the recovery phrase. Verify: synchronized data returns and a device-bound wrapped copy is created.
- [ ] 7.4 Perform a manual migration pass against a project seeded with plaintext documents, including one interrupted mid-run and resumed. Verify: migration completes and no plaintext finance fields remain.
- [ ] 7.5 Resolve the open questions recorded in `design.md` — failed-attempt threshold, phrase re-display policy, optional export file, auto-lock default and options — and record the decisions in the design document before the affected tasks are marked complete. Verify: no open question remains that governs a shipped behavior.
- [ ] 7.6 Run `openspec validate "end-to-end-encrypted-sync" --strict`. Verify: validation passes.
- [ ] 7.7 Synchronize the delta specs for `client-side-encryption`, `app-lock`, `firebase-data-backend`, and `offline-data-sync` into `openspec/specs/`, then validate the main specs. Verify: main specs validate and reflect the encrypted synchronization behavior.
