# Offline-First Access Specification

## Purpose

Define immediate local-only app access, opt-in sign-in prompts, local-preserving sign-out, and authenticated-only receipt AI.

## Requirements

### Requirement: App starts in local-only mode without authentication
Cofinance SHALL allow a user with no Firebase session to enter the main finance experience and use durable on-device finance data without network access.

#### Scenario: First launch has no session
- **WHEN** the application starts without an authenticated Firebase user
- **THEN** it SHALL navigate to the main experience rather than the login page
- **AND** account and transaction operations SHALL use on-device storage

#### Scenario: Local-only app restarts
- **WHEN** a signed-out user restarts the application after saving finance data
- **THEN** the previously saved local accounts and transactions SHALL remain available

### Requirement: Signed-out users are invited to enable sync
Cofinance SHALL show a clear sign-in call to action in the signed-out profile experience and SHALL describe sign-in as enabling cloud synchronization.

#### Scenario: Signed-out user opens profile
- **WHEN** a user without a Firebase session opens Profile
- **THEN** the app SHALL show a sign-in-to-sync call to action
- **AND** it SHALL NOT show authenticated-only profile editing or logout actions

#### Scenario: User selects sign-in CTA
- **WHEN** a signed-out user selects the sign-in-to-sync call to action
- **THEN** the app SHALL open the existing Google login flow

### Requirement: Signing out preserves local use
Signing out SHALL end the cloud session without deleting local finance data or making login mandatory.

#### Scenario: Authenticated user signs out
- **WHEN** an authenticated user confirms sign-out
- **THEN** the Firebase session SHALL end
- **AND** the app SHALL remain in or return to the main local-only experience
- **AND** locally stored accounts and transactions SHALL remain available

### Requirement: Receipt AI requires an authenticated user
Cofinance SHALL allow Gemini receipt scanning only while a real Firebase user session is active.

#### Scenario: Signed-out user selects receipt automation
- **WHEN** a signed-out user selects the receipt image or camera automation entry point
- **THEN** the app SHALL present sign-in guidance or navigate to sign-in instead of opening the AI flow

#### Scenario: Signed-out scan reaches the repository
- **WHEN** a receipt scan request reaches the transaction repository without an authenticated session
- **THEN** the repository SHALL reject it before sending image data to Gemini

#### Scenario: Signed-in user selects receipt automation
- **WHEN** an authenticated user selects the receipt image or camera automation entry point
- **THEN** the existing receipt capture and Gemini scan flow SHALL remain available

### Requirement: Session policy is independently testable
Authentication-dependent routing, synchronization, and AI decisions SHALL depend on an injectable session policy rather than requiring UI code to access Firebase directly.

#### Scenario: Unit tests provide a signed-out session
- **WHEN** a unit test provides a signed-out session policy
- **THEN** local access and AI denial behavior SHALL be testable without Firebase initialization
