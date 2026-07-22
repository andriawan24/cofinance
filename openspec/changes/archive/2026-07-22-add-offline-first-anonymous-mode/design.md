## Context

Cofinance currently binds `CofinanceDatabase` directly to `FirestoreCofinanceDatabase`, and the account and transaction repositories derive a mandatory user id from `FirebaseAuth`. Splash routes unauthenticated users to login, logout returns there, and receipt scanning calls Gemini without an authorization policy. Android and iOS are the supported runtime targets.

The feature crosses navigation, authentication, persistence, synchronization, UI, localization, dependency injection, and AI authorization. Local data must survive process restarts and must remain usable with no network or Firebase session. Verification is intentionally limited by the user to compilation/build tasks at the end of implementation; unit test sources may be added but are not executed separately.

## Goals / Non-Goals

**Goals:**

- Make the main finance experience available immediately without authentication.
- Persist accounts and transactions locally on Android and iOS and use that store as the UI source of truth.
- After Google sign-in, merge local and remote finance records and mirror subsequent mutations to Firebase.
- Expose session state for UI and policy decisions without scattering raw Firebase checks.
- Provide a prominent sign-in/sync CTA in the profile experience.
- Prevent signed-out users from entering or executing Gemini receipt scanning.
- Preserve authenticated Firebase user isolation and existing finance filtering/balance semantics.

**Non-Goals:**

- Firebase anonymous authentication, automatic background scheduling, live multi-device listeners, tombstone-based deletion sync, sophisticated conflict resolution, or a sync-status history UI.
- Reworking the native Google credential acquisition flow.
- Functional, UI, simulator, or device test execution in this change.

## Decisions

### Use a durable Room KMP database as the local source of truth

Add Room 2.8.4 and bundled SQLite, following the official KMP setup, with a common database/DAO and Android/iOS builders. Repositories will read and observe local data regardless of session state. Room is preferred over an in-memory cache because offline-first data must survive restarts, and over a hand-written file store because transactions, reactive queries, and account balance updates require atomic structured persistence.

The existing `CofinanceDatabase` contract will represent local persistence and no longer require a user id. `FirestoreCofinanceDatabase` will move behind a separate remote-sync contract so cloud authorization cannot leak into local operations.

### Treat signed-out local use as anonymous mode without creating a Firebase identity

Introduce an authentication/session policy exposed by the authentication repository (`isSignedIn` and observable session state). A missing Firebase user means local-only mode. This is preferred over Firebase anonymous auth because anonymous auth still creates a cloud identity and requires connectivity, contradicting the requested offline-first default.

Splash always proceeds to main after lightweight startup work. Login remains a destination opened from explicit sign-in CTAs. Successful login returns to main; logout clears only the Firebase session and leaves the local database intact.

### Synchronize explicitly on sign-in, then use local-first write-through

After Firebase accepts the Google credential, a finance sync coordinator will:

1. fetch the signed-in user's remote accounts and transactions;
2. merge remote-only records into the local store;
3. upload the resulting local account and transaction snapshot to that user's Firestore collections; and
4. return success only after the initial synchronization completes.

UUID identity is retained. On the extremely unlikely same-id conflict, the local record wins during this initial merge because local state is the user's active source of truth. Accounts are uploaded before transactions so references exist. Subsequent local mutations are committed locally first, then mirrored to Firebase while signed in. A cloud mirror failure does not roll back the valid local operation; it is surfaced as a sync failure where the caller has an error channel and will be retried by the next sign-in sync. Remote deletion propagation is excluded because the existing models have no tombstones.

This approach is smaller and more predictable than a bidirectional live listener or background sync engine while satisfying opt-in cloud synchronization.

### Centralize authenticated-only AI policy at UI and repository boundaries

The add-transaction image/receipt action is replaced with a sign-in prompt when signed out, and direct navigation to camera/preview is redirected to login. `TransactionRepository.scanReceipt` also checks the session policy and throws a typed authorization error before invoking Gemini. The repository check is authoritative; UI gating provides a clear experience and prevents unnecessary camera work.

### Adapt profile to session state

Signed-out profile renders a local-mode summary and a prominent “Sign in to sync” CTA. Authenticated profile retains user details, profile editing, cycle settings, and logout. Logout returns to the local-mode profile/main experience. Authenticated-only profile mutations remain unavailable while signed out.

### Verification evidence

At the end only, run the Gradle build/compilation path that covers common code plus Android and iOS source compilation as available on the host. Do not run functional/UI tests or standalone test tasks. The build may compile unit test sources as part of its normal graph, which is acceptable. Also run strict OpenSpec validation and audit the diff against each scenario.

## Risks / Trade-offs

- [Initial merge cannot fully resolve concurrent same-id edits] → Use UUIDs and deterministic local-wins behavior; document richer conflict handling as future work.
- [Local-first write succeeds while Firebase mirroring fails] → Preserve user data locally and retry the full snapshot on the next successful sign-in.
- [No tombstones means remote deletions cannot be reliably merged across devices] → Keep deletion sync out of scope and avoid claiming live multi-device parity.
- [Room/KSP increases build complexity on native targets] → Use the stable Room KMP line, bundled SQLite, explicit per-target KSP configuration, and end-only cross-target build compilation.
- [A stale or bypassed UI could reach AI] → Enforce the session check again at the repository boundary.
- [Existing signed-in users start using a new empty local store] → Initial startup/sign-in sync imports their Firestore snapshot before presenting it as synchronized.

## Migration Plan

1. Add Room/KSP build configuration and platform database builders.
2. Introduce local entities/DAO/database and separate Firestore remote sync access.
3. Switch repositories to local-first access and add the sign-in synchronization coordinator.
4. Change startup, logout, profile CTA, and AI routing/policy.
5. Add unit test sources, localized copy, and perform the end-only build and strict validation.

Rollback can restore the previous Firestore binding and authentication-gated routing. Existing Firestore documents are not destructively migrated, and the new local SQLite file can remain unused if rolled back.

## Open Questions

None. Background retry, conflict UI, and deletion tombstones are intentionally deferred.
