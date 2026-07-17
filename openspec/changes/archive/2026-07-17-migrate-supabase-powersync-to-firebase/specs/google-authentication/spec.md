## MODIFIED Requirements

### Requirement: Native Google sign-in on Android
The Android application SHALL request a Google ID token through Android Credential Manager.

#### Scenario: Android sign-in succeeds
- **WHEN** a user selects a Google account and Credential Manager returns a valid Google ID token
- **THEN** the application SHALL exchange that token with Firebase Authentication and establish a session

#### Scenario: Android sign-in is cancelled
- **WHEN** the user cancels the credential flow
- **THEN** the application SHALL return to the login state without reporting an authentication failure

### Requirement: Native Google sign-in on iOS
The iOS application SHALL use the Google Sign-In iOS SDK through the Swift-to-Kotlin bridge.

#### Scenario: iOS bridge is configured
- **WHEN** the iOS application starts
- **THEN** it SHALL configure `GoogleSignInBridgeImpl`, register it with the shared bridge holder, and handle callback URLs

#### Scenario: iOS sign-in succeeds
- **WHEN** the native Google account flow returns an ID token
- **THEN** the shared login flow SHALL exchange that token with Firebase Authentication and establish a session

### Requirement: Protect authentication material
The application SHALL NOT log ID tokens, access tokens, refresh tokens, client secrets, or private configuration.

#### Scenario: Authentication fails
- **WHEN** a native or Firebase authentication operation fails
- **THEN** diagnostics SHALL describe the failure without including credential material
