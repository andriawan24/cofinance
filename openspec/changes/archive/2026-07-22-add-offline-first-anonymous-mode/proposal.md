## Why

Cofinance currently requires a Firebase session before users can view or change finance data, which makes sign-in a prerequisite and prevents private, offline-first use. Users should be able to start immediately with durable on-device data, then opt into Google sign-in when they want cloud synchronization and authenticated AI features.

## What Changes

- Start the app in the main experience even when no Firebase user is present; the login page becomes an explicit sign-in destination instead of an authentication gate.
- Add durable local account and transaction storage as the source of truth for signed-out use.
- Synchronize local finance data with the signed-in user's Firebase collections after successful sign-in and keep authenticated changes mirrored to the cloud.
- Present clear sign-in/sync calls to action for signed-out users and preserve sign-out as a return to local-only mode rather than ejecting the user to login.
- Gate receipt-scanning AI entry points and execution behind a real signed-in Firebase session, with sign-in guidance when unavailable.
- Add unit coverage for session policy, sync behavior, and AI authorization boundaries; run build-only verification after implementation, without functional/UI test execution.
- Non-goals: background sync scheduling, multi-device conflict-resolution UI, Firebase anonymous accounts, and changes to the Google provider credential flow.

## Capabilities

### New Capabilities
- `offline-first-access`: Local-first app entry, durable anonymous finance data, opt-in cloud synchronization, and authenticated-only AI access.

### Modified Capabilities
- `google-authentication`: Google sign-in becomes an optional in-app action that upgrades local-only use to a cloud-synced session.
- `offline-data-sync`: The shared database contract changes from Firestore-only access to a durable local source of truth with conditional Firebase synchronization.
- `firebase-data-backend`: Firebase finance storage is used only for authenticated synchronization rather than being required for every finance operation.

## Impact

- Navigation, splash/session routing, login/logout behavior, profile and finance UI calls to action, and receipt scan entry points.
- Account and transaction repositories, database composition, Firebase data access, and synchronization orchestration.
- KMP build configuration and platform database construction for a shared SQLite/Room local store.
- Localized English and Indonesian resources and common unit tests.
