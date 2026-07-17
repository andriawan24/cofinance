## Why

Cofinance currently splits authentication and remote data across Supabase while routing native account and transaction operations through PowerSync. Consolidating on Firebase removes that synchronization layer and gives every supported target one backend for authentication, user profiles, files, accounts, and transactions while preserving the existing application flow.

## What Changes

- **BREAKING** Remove Supabase Auth, PostgREST, Storage, configuration, and client code.
- **BREAKING** Remove PowerSync, its local database, synchronization connector, native initialization, and configuration.
- Add Firebase Authentication with Google credentials and persistent session restoration.
- Add Cloud Firestore as the sole account and transaction database, including atomic balance and transfer mutations.
- Add Firebase Storage for avatar upload and Firebase-backed profile metadata.
- Preserve the current screens, navigation, view-model events, use cases, repository contracts, and user-visible flows.
- Configure Firebase for supported Android and iOS targets with the standard `google-services.json` and `GoogleService-Info.plist` platform files.
- Defer compilation until implementation is complete, then run the relevant build once.

## Capabilities

### New Capabilities
- `firebase-data-backend`: Firebase Authentication, Firestore finance persistence, Firebase Storage avatars, ownership boundaries, and atomic finance mutations.

### Modified Capabilities
- `google-authentication`: Exchange native Google credentials for Firebase sessions instead of Supabase sessions.
- `offline-data-sync`: Remove the PowerSync offline-first behavior and require direct Firestore-backed account and transaction operations.
- `dependency-management`: Replace Supabase and PowerSync dependency families and build configuration with supported Firebase KMP dependencies and platform configuration.

## Impact

The change affects common authentication and finance data sources, repository implementations, dependency injection, login and splash session handling, Android and iOS Firebase setup, Gradle dependencies and configuration, native manifests/initializers, documentation, and all Supabase/PowerSync implementation files. Existing domain models and presentation flow remain stable; existing Supabase/PowerSync local data is not migrated on-device.
