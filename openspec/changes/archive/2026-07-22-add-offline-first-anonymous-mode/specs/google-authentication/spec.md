## ADDED Requirements

### Requirement: Google sign-in is an optional sync upgrade
The Google login flow SHALL be initiated explicitly from the local-only experience and SHALL trigger initial finance synchronization after Firebase authentication succeeds.

#### Scenario: Local-only user signs in successfully
- **WHEN** a local-only user completes Google and Firebase authentication
- **THEN** the application SHALL synchronize local finance data with that Firebase user's cloud data
- **AND** it SHALL return the user to the main experience with an authenticated session

#### Scenario: Optional sign-in is cancelled
- **WHEN** a local-only user cancels Google sign-in
- **THEN** the application SHALL retain local-only access and local finance data

