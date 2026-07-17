## Context

Cofinance authenticates Google ID tokens with Supabase Auth, stores profile metadata and avatars through Supabase, and exposes account/transaction operations through `CofinanceDatabase`. Native targets instantiate a PowerSync implementation of that contract; repositories then apply transaction balance effects as separate local statements. Android and iOS are the supported runtime targets, and current presentation/domain flows must remain unchanged.

The replacement must compile from shared Kotlin, link the official Firebase native SDKs, use each platform's standard Firebase configuration file, and preserve realtime observations currently supplied by PowerSync. The user requested implementation first and one build attempt only after all edits.

## Goals / Non-Goals

**Goals:**

- Use Firebase Authentication for the existing native Google ID-token flow and session restoration.
- Use Cloud Firestore as the sole database for user profiles, accounts, and transactions.
- Use Firebase Storage for avatar bytes while keeping the current profile editing flow.
- Preserve current screens, navigation, view-model events, domain models, use-case interfaces, sorting, filters, draft behavior, and balance semantics.
- Remove Supabase and PowerSync dependencies, configuration, initialization, and source code.
- Make transaction creation/update and all associated balance effects atomic in Firestore.
- Support Android and iOS through the GitLive Firebase Kotlin SDK backed by official platform Firebase SDKs.

**Non-Goals:**

- Import existing Supabase or PowerSync data into Firestore.
- Preserve an explicit offline-first queue or app-managed local database.
- Change UI/UX, navigation, finance calculation rules, or receipt scanning.
- Add Firebase emulator tests, production security-rule deployment, or CI provisioning in this change.

## Decisions

### Firestore is the sole finance database

Firestore is selected over Realtime Database because the existing account and transaction model benefits from collection queries, date filters, ordered realtime snapshots, and multi-document transactions. Data is scoped as `users/{uid}`, `users/{uid}/accounts/{accountId}`, and `users/{uid}/transactions/{transactionId}`. User subcollections make ownership explicit and align naturally with Firebase security rules.

Alternative considered: Realtime Database. It would require more denormalized indexes and manual query composition for the existing filtered transaction views.

### Keep the database contract but replace its implementation

`CofinanceDatabase` remains the repository-facing boundary so presentation and domain layers do not change. The PowerSync lifecycle methods are removed. A common `FirestoreCofinanceDatabase` provides snapshot flows and direct suspend operations. App lifecycle hooks no longer pause/resume storage because Firestore owns its network lifecycle.

Alternative considered: rewrite repositories to call Firestore directly. That would broaden changes and weaken the existing clean-architecture boundary.

### Move finance mutation integrity into Firestore transactions

Transaction creation and update operations are elevated on the database contract so each Firestore transaction writes the transaction document and all affected account balances together. Updating a transaction reads the stored old transaction, reverses its balance impact, applies the replacement, and writes all documents atomically. Account balance adjustments use current values read inside the same transaction.

Alternative considered: retain separate repository calls. That can leave balances inconsistent after a partial network failure and does not satisfy atomic transfer behavior.

### Store queryable fields as stable serialized document fields

Internal serializable Firestore DTOs use camelCase fields and ISO-8601 strings already produced by the app. Account and transaction domain response mapping remains centralized in the database implementation. Transaction snapshots hydrate sender and receiver account objects from the same user's account collection so current UI models remain unchanged.

### Firebase Authentication owns sessions; Firestore owns extended profile fields

Google ID tokens are converted with `GoogleAuthProvider.credential` and passed to Firebase Auth. Firebase Auth supplies uid, email, display name, and provider photo URL. Cofinance-specific profile fields (`customName`, `customAvatarUrl`, `cycleStartDay`, and `lastCycleResetDate`) live in `users/{uid}` and are merged on updates. Avatar bytes are uploaded to Firebase Storage at `avatars/{uid}/avatar.jpg`.

### Initialize from standard platform Firebase configuration files

Android reads `androidApp/google-services.json` through the `com.google.gms.google-services` Gradle plugin, allowing Firebase's Android startup provider to initialize the default app. iOS reads `iosApp/iosApp/GoogleService-Info.plist` from the application target and calls `FirebaseApp.configure()` during Swift app startup. Firebase application options are not duplicated in BuildKonfig or initialized from shared Compose code. Existing Google client IDs remain unchanged.

Alternative considered: programmatic initialization from BuildKonfig values. This duplicates the platform configuration generated by Firebase, deviates from the official platform setup, and requires maintaining separate application option fields manually.

### Link Firebase on iOS through Swift Package Manager

The iOS app already uses Swift Package Manager. Add the official `firebase-ios-sdk` package products needed by GitLive (`FirebaseCore`, `FirebaseAuth`, `FirebaseFirestore`, and `FirebaseStorage`) to the application target and initialize Firebase from Swift before shared services are resolved.

## Risks / Trade-offs

- [No app-managed offline database] → Firestore may retain SDK-managed cache behavior, but the app provides no guaranteed offline-first contract or PowerSync queue.
- [Existing user data is not migrated] → Document the new Firestore schema and treat Firebase as a clean backend cutover.
- [Firestore composite indexes may be required for combined date/type queries] → Prefer client-side draft/type filtering after user-scoped ordered snapshots where practical; document any index surfaced by Firebase.
- [Transaction hydration performs account reads] → Deduplicate account IDs per emission and fetch each account once before mapping transactions.
- [Missing Firebase configuration prevents platform startup/build] → Document the exact Android and iOS file locations and let the official Firebase tooling report a missing or mismatched configuration file.
- [iOS native linking is sensitive to package products] → Link every official SDK required by the GitLive modules and perform the requested iOS-aware Gradle build at the end.

## Migration Plan

1. Add Firebase dependencies, platform links, and standard platform configuration-file integration.
2. Add platform Firebase initialization, authentication/profile/storage data source, and Firestore database implementation.
3. Switch repositories, login, splash, profile, dependency injection, and app composition to Firebase while preserving their outputs/events.
4. Remove PowerSync and Supabase source, manifest entries, dependencies, and configuration.
5. Document Firebase project setup and Firestore collection/security expectations.
6. Validate OpenSpec and run one final build attempt. If a platform Firebase configuration file is absent, report that external prerequisite distinctly from source compilation failures.

Rollback is a source-level revert to the pre-change Supabase/PowerSync implementation. Backend data written after cutover is not automatically copied back.

## Open Questions

None. Firestore is selected as the requested Firebase database, and existing backend data migration is explicitly outside scope.
