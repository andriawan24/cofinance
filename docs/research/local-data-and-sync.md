# Local data and sync

**Question:** Where does finance data live, and how does it move between the device and Firebase?

**Last verified:** 2026-11-16 against `ac66286`. Established by reading the source; synchronization was not observed against a live Firebase project. Path shorthand is defined in [README.md](README.md).

## Summary

Room on the device is the source of truth. Firebase is an optional encrypted mirror for signed-in users. Synchronization is whole-snapshot and local-wins: there is no incremental sync, no outbox, no listener, and no tombstone.

## Local database

`CofinanceRoomDatabase` is at schema version 2 with an auto-migration from 1 (`app/data/local/CofinanceRoomDatabase.kt:15-35`). Schemas are exported to `composeApp/schemas/`.

| Table | Columns | Synced |
|-------|---------|--------|
| `accounts` | `id`, `name`, `group`, `balance`, `accountType`, `createdAt` | Yes |
| `transactions` | `id`, `amount`, `category`, `date`, `fee`, `notes`, `senderAccountId`, `receiverAccountId`, `type`, `createdAt`, `updatedAt` | Yes |
| `merchant_categories` | `merchantKey`, `category`, `updatedAt` | No — device-local by design |

No table has a pending, dirty, or deleted column.

The database file is `cofinance.db`, in the app database directory on Android and the documents directory on iOS (`app-android/data/local/DatabaseBuilder.android.kt:7-11`, `app-ios/data/local/DatabaseBuilder.ios.kt:12-21`). It uses the bundled SQLite driver and is **not encrypted**; it relies on platform disk encryption.

### Behaviour the local layer guarantees

- Account observers receive the current list ordered by creation time, newest first. Transaction observers apply date, draft, and identifier filters, order by date descending, and include sender and receiver account details.
- **Balance changes are atomic.** Creating, updating, or deleting a non-draft transaction and adjusting the affected account balances happen in one database transaction. An update reverses the old impact before applying the new one; a transfer debits the sender including the fee and credits the receiver, or does neither (`app/data/local/transaction/RoomTransactionLocalDataSource.kt:84-143, 229-250`).

## Firebase

Only Firebase Authentication and Cloud Firestore are used. Everything is scoped beneath the authenticated user's document, and the user ID is read from the session on every call.

```text
users/{uid}                         Profile: name, avatar URL, cycle start day, last cycle reset date. Plaintext.
users/{uid}/key_material/current    Wrapped copies of the data key.
users/{uid}/accounts/{id}           Encrypted account.
users/{uid}/transactions/{id}       Encrypted transaction.
```

Account and transaction documents hold only an encrypted envelope; see [client-side-encryption.md](client-side-encryption.md) for the shape. Reads fetch the whole collection with no server-side ordering, because the server cannot order ciphertext (`app/data/remote/FirestoreFinanceDocumentStore.kt:34-63`). Ordering always comes from Room.

A new profile document is created with defaults at first sign-in, keeping the provider's name, email, and photo (`app/data/datasource/FirebaseDataSource.kt:100-136`).

### Avatars

Avatars are **not** in Firebase Storage. `FirebaseDataSource.uploadAvatar` uploads to `avatars/{uid}/avatar.jpg` in an S3-compatible store through `GarageStorageDataSource`, configured by the `storage.*` build values (`app/data/datasource/FirebaseDataSource.kt:46-52`). Android uses the AWS SDK for Kotlin with static credentials; iOS issues a plain HTTP `PUT`.

## Synchronization

`FirebaseSyncCoordinator` owns all of it (`app/data/sync/FirebaseSyncCoordinator.kt:21-79`).

| Operation | When it runs | What it does |
|-----------|--------------|--------------|
| `syncDataAfterSignIn` | At sign-in, and on every launch that reaches an unlocked session | Imports remote records whose IDs are absent locally, then mirrors |
| `mirrorDataIfSignedIn` | After every local mutation | Re-encrypts and uploads the entire local snapshot |
| `deleteTransactionIfSignedIn`, `deleteAccountIfSignedIn` | When a record is deleted | Deletes the remote document immediately |
| `clearLocalData` | At sign-out | Deletes every local account and transaction |

### Rules

- **Local first.** A mutation commits to Room before anything is sent.
- **Local wins by identifier.** Import only inserts records that do not exist locally. A record present on both sides is never compared; the local copy overwrites the remote one on the next mirror. `updatedAt` is not consulted.
- **Both directions need the data key.** Import and mirror return silently when the encryption session holds no key, so a locked or not-yet-set-up device uploads nothing. Uploads also require the user's key material to exist remotely first (`app/data/remote/KeyMaterialGate.kt:62-73`).
- **Deletes need no key.** A deletion produces no ciphertext, so a locked device can still propagate it.
- **Everything is uploaded.** `DRAFT` and `CYCLE_RESET` rows are mirrored on the same encrypted terms as real transactions.
- **Failure is tolerated at launch.** The launch sequence swallows a failed synchronization so the app still opens offline.

There is no outbox. The retry mechanism for a failed upload is that the next mirror uploads everything again.

## Deleting

- **A transaction:** one database transaction reverses its balance impact and deletes the row; then the remote document is deleted (`app/data/local/transaction/RoomTransactionLocalDataSource.kt:135-143`).
- **An account:** `DeleteAccountUseCase` deletes the account's transactions first, reversing balances only on accounts that still exist — the other side of a transfer — and then deletes the account. Each removed identifier is sent to the remote delete (`app/domain/usecases/accounts/DeleteAccountUseCase.kt:17-30`).

Both are confirmed by the user before they run.

## Open

- **Deleted records can come back.** Deletes are hard deletes with no tombstone. A delete made while signed out never reaches the cloud, and a remote delete that fails is not retried. Because import inserts any remote ID that is missing locally, the next sign-in or launch restores the record.
- **Deletes do not reach other devices.** A second device learns nothing from a remote delete — its import never removes local rows — and it uploads the record again on its next mirror. This matters as soon as a user restores onto a second device.
- **A failed mirror reports the whole operation as failed.** The mirror is awaited inside the repository, so an upload error after a successful local write surfaces as an error to the user, although the local change is durable. The same applies to a failed remote delete after the local row is gone.
- **`fallbackToDestructiveMigration(dropAllTables = true)` is still armed** (`app/data/local/DatabaseBuilder.kt:11-16`). A future schema bump without a migration would drop every local record, which is unrecoverable for a local-only user. Version 2 was only safe because an auto-migration was added alongside it.
- **Abandoned scan drafts accumulate.** Backing out of the review screen leaves the `DRAFT` row in Room, and it is mirrored to Firestore. Nothing cleans them up.
- **No Firestore security rules are in the repository.** There is no `firestore.rules` or `firebase.json`, so the rule that a user may reach only paths under their own `uid` exists only in the Firebase console, unreviewed.
- **iOS avatar upload is unauthenticated.** The iOS actual sends an unsigned `PUT` with no credentials (`app-ios/data/datasource/GarageStorageDataSource.ios.kt:20-45`). It only works if the bucket accepts anonymous writes.
- **The stored avatar URL omits the bucket.** Both uploaders write to `{base}/{bucket}/{key}`, but the URL saved to the profile is `{base}/{key}`. `uploadAvatar` also ignores the upload result, so a failed upload still saves a URL.
- **Firebase Storage is still wired in but unused**: a Gradle dependency, a Koin binding, and an iOS Swift package product, with nothing that injects it.
