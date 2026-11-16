# Authentication

**Question:** How does sign-in work, and what can a signed-out user do?

**Last verified:** 2026-11-16 against `ac66286`. Established by reading the source; the flows were not run. Path shorthand is defined in [README.md](README.md).

## Summary

Cofinance is usable with no account. Google sign-in is an opt-in upgrade that turns on encrypted cloud synchronization. There is no anonymous Firebase user: a signed-out user is simply a local-only user with no Firebase identity, and nothing is linked on sign-in.

## Google sign-in

`GoogleAuthManager` is an `expect class` returning `Success(idToken, accessToken, email)`, `Error`, or `Cancelled` (`app/auth/GoogleAuthManager.kt:5-19`).

| Platform | Mechanism | Returns |
|----------|-----------|---------|
| Android | AndroidX Credential Manager with `GetGoogleIdOption`; the server client ID is `BuildKonfig.GOOGLE_AUTH_API_KEY` (`app-android/auth/GoogleAuthManager.android.kt:25-35`) | ID token only |
| iOS | Google Sign-In SDK in Swift, reached through `GoogleSignInBridge`; the client ID comes from the Firebase options (`iosApp/iosApp/GoogleSignInHelper.swift:15-57`) | ID token and access token |

The iOS bridge is a Kotlin interface implemented in Swift and registered at startup, with no cinterop (`app-ios/auth/GoogleSignInBridge.kt:22-29`, `iosApp/iosApp/iOSApp.swift:11-25`). If no bridge is registered, sign-in returns an error.

### Exchange for a Firebase session

`FirebaseDataSource.login` builds `GoogleAuthProvider.credential(idToken, accessToken)`, calls `signInWithCredential`, and then loads or creates the profile document (`app/data/datasource/FirebaseDataSource.kt:29-39, 100-112`).

The full path is `LoginViewModel.signInWithIdToken` → `LoginIdTokenUseCase` → `AuthenticationRepositoryImpl.login`, which signs in and then calls `syncDataAfterSignIn()` (`app/data/repository/AuthenticationRepositoryImpl.kt:36-39`). On success the screen re-enters the launch sequence, which routes a first-time user to encryption setup (`app/navigations/MainNavigation.kt:145-151`).

## Session state

`SessionPolicy` answers `isSignedIn()` and `userIdOrNull()` from `auth.currentUser` (`app/data/session/SessionPolicy.kt:5-16`). There is no auth-state listener; callers ask when they need to know. The profile is cached in memory in `FirebaseDataSource`.

Routing, synchronization, and scanning decisions depend on this injectable policy rather than on Firebase directly, so they can be exercised with a fake session.

## Signed-out use

- The launch sequence sends a signed-out user straight to Main, with no setup and no unlock (`app/pages/splash/SplashViewModel.kt:110-112`).
- Account and transaction operations read and write Room only. Nothing is sent to the cloud.
- Profile shows a sign-in card instead of profile editing, security settings, and sign-out (`app/pages/profile/ProfileScreen.kt:168-260`).

Two entry points are still gated on sign-in:

- Opening the camera for a receipt redirects to Login (`app/navigations/MainNavigation.kt:226-231`).
- An image shared into the app redirects to Login (`app/navigations/MainNavigation.kt:120-126`).

## What happens to local data

### On sign-in

Records created while signed out are kept and uploaded. `syncDataAfterSignIn` imports remote records whose identifiers are absent locally, then uploads the whole local snapshot (`app/data/sync/FirebaseSyncCoordinator.kt:29-46`). On a first sign-in there is no data key yet, so this runs after encryption setup or restore completes, from the launch sequence.

### On sign-out

**Sign-out deletes every local account and transaction.** `AuthenticationRepositoryImpl.logout` signs out of Firebase and then calls `clearLocalData()` (`app/data/repository/AuthenticationRepositoryImpl.kt:41-44`, `app/data/sync/FirebaseSyncCoordinator.kt:75-78`).

This is the opposite of what the removed `offline-first-access` spec required, which was that signing out end the cloud session and leave local finance data available. Whether the current behaviour is intended is an open question below. The merchant-category associations learned from scans are not cleared.

## Protecting authentication material

ID tokens, access tokens, refresh tokens, and client secrets must not be logged. Failure diagnostics describe the failure without including credential material.

## Open

- **Sign-out does not reset encryption state.** `logout` never calls `forgetSetup()` or `lock()` on the encryption session and does not erase the local key material, the sealed recovery phrase, the PIN wrap, or the biometric key. `forgetSetup()` is called only from `SplashViewModel` and `RecoveryPhraseRestoreViewModel`, although its documentation says sign-out lands there. Traced consequences when a different account signs in on the same device:
  - In the same process the session is still `Unlocked` with the previous account's key, so the launch sequence skips setup. Uploads then fail at `KeyMaterialGate.requireKeyMaterial`, because the new account has no key material.
  - After a restart, the previous account's local key material is adopted and unlocked through the device wrap.
- **Is wiping local data on sign-out intended?** The earlier spec said no. A user who signs out loses the on-device copy; a signed-in user can recover it from the cloud with the recovery phrase, but any record that failed to upload is gone.
- **Receipt capture requires sign-in, with no remaining reason.** The gate originally stopped receipt images from being sent to a cloud model for a signed-out user. Scanning is now on-device (see [receipt-scanning.md](receipt-scanning.md)), and the repository-level check was removed, but the two navigation redirects above still send signed-out users to Login.
