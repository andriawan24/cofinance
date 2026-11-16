# Client-side encryption

**Question:** How is synchronized data encrypted, and how is the key recovered?

**Last verified:** 2026-11-16 against `ac66286`. The mechanism was established by reading the source. The Android hardware properties were observed on a physical device in August 2026, before the test suite was removed; that evidence is summarized under [Evidence](#evidence). Path shorthand is defined in [README.md](README.md).

## Summary

Accounts and transactions are encrypted on the device before they reach Firestore, so the operator cannot read them, including from a full database export. One random data key encrypts every record. That key is never stored unwrapped; it is wrapped separately by a device-bound key, by a key derived from the user's recovery phrase, and optionally by a key derived from the user's PIN. Only the recovery-phrase wrap is uploaded.

Encryption is mandatory for anyone who syncs and is set up at sign-in. A local-only user never meets it.

**Losing both the device and the recovery phrase means losing the synchronized data.** There is no operator-side recovery, by design.

## What is and is not encrypted

| Encrypted | Not encrypted |
|-----------|---------------|
| Every finance field of accounts and transactions, including `updatedAt` | The profile document (name, avatar URL, cycle settings) |
| `DRAFT` and `CYCLE_RESET` transactions, on the same terms | Document identifiers, which are random and needed to address the document |
| | The local Room database, which relies on platform disk encryption |
| | Avatars, and the device-local merchant-category map |

Record counts, ciphertext sizes, and write timing remain visible to the backend.

## Record encryption

`RecordCipher` seals each record with AES-256-GCM under the data key, with a fresh random 12-byte nonce per operation (`core/crypto/RecordCipher.kt:15-47`). The nonce is never derived from content or a counter: every mirror re-encrypts the whole snapshot, and nonce reuse under GCM is catastrophic rather than merely weakening.

A stored document looks like this (`core/crypto/EncryptedEnvelopeDocument.kt:16-37`):

```text
{ envelope_version: 1, key_id, nonce: <base64>, ciphertext: <base64, GCM tag appended> }
```

Opening checks the envelope version and key identifier. A record that fails to open is skipped rather than imported (`app/data/remote/EncryptedAccountDataSource.kt:37-42`).

The library is `dev.whyoleg.cryptography` 0.6.0 with its platform-native providers.

## Key hierarchy

The data key is one AES-256 key generated in memory, with a random 8-byte identifier (`core/crypto/DataKey.kt:20-52`). Every wrap seals it with AES-256-GCM under a key derived through HKDF-SHA256, with a distinct `info` string per wrap type so the derivations cannot be confused (`core/crypto/KeyWrapping.kt:34-138`).

| Wrap | Wrapping key derived from | Stored | Uploaded |
|------|---------------------------|--------|----------|
| Device | Ephemeral-static ECDH over P-256 against the device key | Locally | No |
| Recovery phrase | The phrase's 128 bits of entropy, with a random 16-byte salt | Locally and in Firestore | Yes |
| PIN | `scrypt(PIN)` concatenated with a device-held secret | Locally | No |

Stored key material is a list of wrapped copies (`core/crypto/KeyMaterialDocument.kt:16-48`). `uploadableKeyMaterial()` is the only path to the uploadable subset and keeps only recovery-phrase wraps. An unknown wrap type decodes rather than failing, so a future wrap type does not break an older client.

In Firestore:

```text
users/{uid}/key_material/current
{ key_material_version: 1,
  wrapped_keys: [ { wrap_type: "recovery_phrase", key_id, wrapped_key, wrap_parameters: { nonce, salt } } ] }
```

Key material lives in its own document so profile writes cannot overwrite it. Every encrypted write passes `KeyMaterialGate.requireKeyMaterial()` first, so no encrypted record can be written before the key that opens it has been published. The gate remembers which user it confirmed, so a second account on the same device cannot inherit the first account's answer (`app/data/remote/KeyMaterialGate.kt:43-73`).

### The unwrapped key

`EncryptionSession` holds the unwrapped key in process memory only and reports `SetupIncomplete`, `Locked`, or `Unlocked` (`core/keyring/EncryptionSession.kt:44-106`). Locking clears the only copy.

## Device key storage

| | Android | iOS |
|---|---------|-----|
| Device key | P-256 in the Keystore with `PURPOSE_AGREE_KEY` on API 31 and above. Below that, a software key sealed under a non-extractable Keystore AES key. StrongBox is used when available. | P-256 in the Secure Enclave, falling back to a Keychain-resident key where there is no Enclave, which is always the case on the simulator. |
| Device secret | HMAC-SHA256 of a fixed label under a non-extractable Keystore HMAC key. Nothing is stored. | A 32-byte random Keychain item. |
| Local key material | A file in app-private storage, written through a temporary file and renamed. | A Keychain generic password, `WhenUnlockedThisDeviceOnly`. |
| Survives uninstall | No. | Yes — Keychain items outlive the app. |

Sources: `core-android/crypto/DeviceKeyVault.android.kt:73-568`, `core-ios/crypto/DeviceKeyVault.ios.kt:128-437`, `core-android/lock/KeyMaterialStorage.android.kt:38-73`, `core-ios/lock/KeyMaterialStorage.ios.kt:30-138`.

The local key material is not encrypted a second time. Each wrap in it is already sealed against key material that cannot leave the device, so a second layer would protect nothing and would add a key whose loss strands the wraps behind it.

## Recovery phrase

The phrase is **six groups of four characters**, drawn from a 58-character alphabet that omits `0`, `O`, `I`, and `l` (`core/crypto/RecoveryPhrase.kt:25-200`). Each group carries 22 bits; together they hold 128 bits of entropy and a 4-bit SHA-256 checksum. It is case-sensitive.

The phrase began as a 12-word BIP39 mnemonic and was changed to alphanumeric groups in commit `57182fc`. The entropy is unchanged at 128 bits, which matches the strength of the wrapping.

Parsing distinguishes a wrong group count, a malformed group, and a checksum failure, so the screen can say which.

### Keeping it

There is no re-entry quiz. Asking the user to type words back was satisfied by copying from the screen above it, so it tested nothing. Instead the phrase can be:

- **Copied.** Android marks the clipboard entry sensitive; iOS expires the pasteboard entry after ten minutes.
- **Saved to a file.** Downloads through MediaStore on Android, the document picker on iOS.

A cancelled picker or a refused clipboard is reported as a failure, not as a saved file (`core-android/crypto/RecoveryPhraseExporter.android.kt:32-92`, `core-ios/crypto/RecoveryPhraseExporter.ios.kt:40-81`).

### Showing it again

The phrase can be re-displayed from security settings, behind a fresh PIN entry even when the app is already unlocked.

The recovery-phrase wrap cannot produce the phrase — it only proves a supplied phrase correct. Re-display therefore needs the entropy stored. `SealedRecoveryPhraseVault` keeps it sealed under the data key, in ordinary app-private storage (`core/crypto/RecoveryPhraseVault.kt:57-132`). That blob is worth exactly what the data key is worth, and it is deliberately not bound to platform key storage: the phrase exists to outlive the device.

The cost is that a set-up device holds a recoverable copy of the phrase, where otherwise it would hold only something that could check one.

## Flows

### Setup

Reached from sign-in, never from first launch (`app/pages/encryption/EncryptionSetupViewModel.kt:138-202`).

1. If the account already has published key material, hand over to restore instead of generating a second phrase.
2. Otherwise generate the data key and phrase and show the phrase, with the consequence of losing it stated above the button.
3. On acknowledgement: publish the recovery-phrase wrap, store the device and phrase wraps locally, store the phrase in the vault, then unlock.

Key material reaches the backend before the session can permit an encrypted write. No PIN is set during setup; the PIN is optional and set later.

### Restore

For a signed-in device holding no key material (`app/pages/encryption/RecoveryPhraseRestoreViewModel.kt:119-222`).

1. Parse the phrase and unwrap the published key material.
2. Read and decrypt both collections into memory.
3. Create and store a device-bound wrap.
4. Import the records.

It is ordered so it cannot import partially, and any failure after the unwrap rolls the session back.

### Launch

For a signed-in user, the launch sequence adopts the locally stored key material (`app/pages/splash/SplashViewModel.kt:107-161`).

- No local key material: route to setup.
- A PIN is set: route to unlock.
- No PIN: unlock silently through the device wrap.
- The device wrap no longer opens and there is no PIN: forget setup, which leads to restore.

## Decisions and why

1. **A symmetric data key under several wraps, rather than encrypting records to a public key.** Asymmetric primitives are slow and size-limited for bulk data. The wrapping layer is also what allows a second device to be added later without re-encrypting any record.
2. **P-256 for device keys.** The Secure Enclave supports only P-256. Curve25519 would be the conventional choice but cannot be hardware-sealed on iOS, and hardware sealing is what makes "only this device can open it" true.
3. **The PIN is bound to a device secret.** A six-digit PIN has a million values. Composing it with a non-extractable secret means that deriving the key needs the device as well as the PIN, and the PIN wrap is never uploaded.
4. **scrypt is implemented in common code** over the library's PBKDF2, as RFC 7914 defines it, because no Kotlin Multiplatform library provides a memory-hard derivation on both platforms. Parameters are N=16384, r=8, p=1. PBKDF2 alone was rejected: against a low-entropy PIN, iteration count buys far less than memory hardness.
5. **Setup is gated at sign-in, not first launch.** Gating at first launch would make local-only users safeguard a phrase that protects nothing.
6. **Server-side ordering was given up.** The server cannot order ciphertext, and Room already answers every ordering question.
7. **`updatedAt` is encrypted too.** Leaving it readable would leak how often the user opens the app, and nothing uses it for incremental sync.
8. **Restore is by recovery phrase only.** A password-protected export file was rejected: it would add a second derivation, a versioned file format, and an artifact users could leak while believing it safe.
9. **No operator recovery path.** Adding one would put the operator back inside the trust boundary and defeat the purpose.
10. **No plaintext migration.** A flow to convert existing plaintext documents was built and then removed in August 2026, because the backend held no user data to convert.

## Not in scope

Multi-device pairing, key rotation, device revocation, local database encryption, and encryption of profile data or avatars. The key material is shaped as a list so that additional wrapped copies are additive later.

## Risks that are accepted

- **The local database is plaintext**, so the app lock protects against someone handed an unlocked phone, not against filesystem access. User-facing copy must not claim otherwise.
- **The operator ships the client**, so a future build could exfiltrate the data key. This is unavoidable for client-side encryption where the operator controls distribution; the mitigation is keeping the crypto module small and inspectable.
- **On Android API 24 to 30 the device wrap is not hardware-sealed.** The Keystore cannot perform EC key agreement before API 31, so the fallback seals a software key under a hardware-backed key. That defeats an attacker holding only the filesystem, but not one with live root on an unlocked device.
- **Rollback is one-way.** Once records are encrypted, an older client cannot read them.

## Evidence

Before the test suite was removed (commit `56bcd9a`), the following had been observed. None of it is currently re-runnable.

- Host tests covered envelope round-tripping, nonce uniqueness, tamper rejection, all three wraps, the phrase, setup, restore, and sync gating.
- On a physical Android device (API 36), instrumented tests confirmed that the device key is hardware-backed and that its private half cannot be exported.
- iOS tests ran in the simulator for the first time after the `:core` extraction, but every Keychain test failed with `errSecNotAvailable`: a Kotlin/Native test binary has no app bundle and therefore no keychain. Running them needs an XCTest bundle with a host application. Embedding entitlements with `-sectcreate`, and ad-hoc signing the test binary, were both tried and do not work.

Never observed, on either platform:

- That a document stored in a real Firebase project contains no readable finance value.
- That a clean install restores its data from the recovery phrase alone.

## Open

- **Should `minSdk` rise to 31?** That would make hardware-sealed key agreement hold for every user, at the cost of older devices. Keeping 24 requires user-facing copy that states the weaker guarantee. Until decided, the code can report the protection it actually obtained (`AndroidDeviceKeyVault.protection()`), but nothing calls it.
- **The two end-to-end checks above have never been done.** Both need a device and a live Firebase project.
- **Comments still describe a 12-word phrase** in `LocalKeyMaterialDestroyer.kt`, `BiometricKeyBox.kt`, `KeyMaterialStorage.kt`, and the Android `FailedAttemptStore`, `KeyMaterialStorage`, and `BiometricKeyBox` actuals.
- **Comments still describe the removed plaintext migration** in `EncryptedAccountDataSource.kt`, `EncryptedEnvelopeDocument.kt`, `MainNavigation.kt`, and the `SplashViewModel` documentation, which promises four launch rules and lists three.
- **Encryption state survives sign-out.** See [authentication.md](authentication.md).
