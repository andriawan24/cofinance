## Why

Cofinance currently writes accounts and transactions to Firestore in plaintext: amounts, categories, notes, dates, and account names are all readable by anyone with access to the project's Firestore data, including the operator. For a personal finance app, that data is among the most sensitive a user holds, and users have no way to verify or limit the operator's access. Encrypting finance data on the device before it is synchronized removes the operator from the trust boundary, so that a user can enable cloud sync without granting anyone else the ability to read their savings and transaction history.

## What Changes

- Introduce a client-side key hierarchy. A randomly generated data encryption key encrypts finance records with AES-256-GCM. The data key never leaves the device unwrapped, and is itself wrapped independently by a device key held in platform hardware-backed storage and by a key derived from a user-held recovery phrase.
- Generate a 12-word BIP39 recovery phrase at encryption setup, require the user to confirm it before proceeding, and use it as the sole mechanism for restoring data on a new device. **BREAKING** A user who loses both their device and their recovery phrase permanently loses access to synchronized data; this is the intended trade-off and there is no operator-side recovery path.
- Encrypt accounts and transactions before they are written to Firestore, and decrypt them on read. This includes DRAFT transactions, which the current mirror path uploads.
- **BREAKING** Make encryption mandatory for cloud synchronization. A user cannot sync without completing encryption setup. Encryption setup is gated on sign-in rather than on first launch, so local-only users are unaffected and nothing is required of a user until their data would otherwise leave the device.
- **BREAKING** Migrate existing plaintext Firestore documents to encrypted form in a forced, resumable, one-time flow, removing the plaintext fields as each document is converted.
- **BREAKING** Remove server-side ordering from Firestore finance reads. Ciphertext cannot be ordered by the server; ordering is performed locally, which the app already does from its Room source of truth.
- Add an app lock with a required PIN and an optional biometric shortcut, both toggleable from the profile page, plus an auto-lock timeout. The PIN-derived key is combined with a non-extractable device secret so that a six-digit PIN cannot be attacked offline against synchronized material.
- Add data export and import driven by the recovery phrase, so a user can move their data to a new device without the operator being able to read it in transit.

Non-goals for this change:

- Multi-device use. One active device is supported; a second device is onboarded by restoring from the recovery phrase, not by pairing. The stored key material is shaped to permit additional wrapped copies later without re-encrypting records.
- Key rotation and device revocation.
- Encryption of the local Room database. Local data continues to rely on platform disk encryption. The app lock therefore protects against someone using an unlocked device, not against an attacker with filesystem-level access.
- Encryption of profile metadata, avatars, cycle settings, or the locally learned merchant-category map.
- Hiding synchronization metadata. Document counts, record sizes, and write timing remain observable to the backend.

## Capabilities

### New Capabilities
- `client-side-encryption`: The key hierarchy, the encrypted record envelope, recovery phrase generation and restoration, mandatory encryption setup at sign-in, migration of existing plaintext records, and export/import of encrypted data.
- `app-lock`: PIN and optional biometric gating of access to the decrypted data key, the settings that control them, auto-lock behavior, failed-attempt handling, and clearing key material from memory.

### Modified Capabilities
- `firebase-data-backend`: Synchronized account and transaction documents no longer carry plaintext finance fields, so the requirement that Firebase synchronization preserves the same record fields changes to preserving the same fields after local decryption.
- `offline-data-sync`: Synchronization requires an unlocked data key in addition to an authenticated session, and the sign-in synchronization path decrypts on import and encrypts on upload.

## Impact

Code:

- `composeApp/src/commonMain/.../data/local/FirestoreCofinanceDatabase.kt` — encrypt on write, decrypt on read, drop `orderBy` on `createdAt` and `date`, change the stored document shape.
- `composeApp/src/commonMain/.../data/sync/FinanceSyncCoordinator.kt` — gate synchronization on an unlocked data key; `mirrorAllIfSignedIn` and `syncAfterSignIn` operate on ciphertext.
- `composeApp/src/commonMain/.../data/session/` — session policy gains a lock-state dimension.
- `composeApp/src/commonMain/.../pages/profile/` — security settings section for PIN, biometric, auto-lock, recovery phrase, and export.
- New `commonMain` crypto, keyring, recovery-phrase, and lock packages with `androidMain`/`iosMain` actuals for hardware-backed key storage and biometric prompts.
- New onboarding screens for encryption setup, phrase confirmation, restore, and unlock.

Dependencies:

- Added: a Kotlin Multiplatform cryptography library providing AES-GCM, key derivation, and HKDF across Android and Apple targets, plus a bundled BIP39 wordlist. Android Keystore and iOS Keychain are used through `expect`/`actual` rather than a third-party wrapper.

Data:

- Firestore document shape changes for accounts and transactions. A new per-user key material document stores wrapped copies of the data key.
- Existing plaintext documents require a one-time migration. Previously exported or backed-up copies of that plaintext are outside the app's control and remain readable.

Behavioral:

- Sign-in gains a mandatory setup step for new users and a mandatory migration step for existing users, both requiring network access.
- App launch gains an unlock step once a PIN is configured.
- Firestore reads return unordered documents; ordering happens locally.
