## ADDED Requirements

### Requirement: CI provisions Android Firebase application configuration
The CI and Android delivery workflows SHALL reconstruct the Android Firebase application configuration from a dedicated protected secret before any Gradle task that requires the Google Services plugin.

#### Scenario: Android configuration is available
- **WHEN** CI runs Android lint, unit tests, or a release build with a valid Android configuration secret
- **THEN** `androidApp/google-services.json` SHALL exist for the build and the Google Services processing task SHALL be able to consume it

#### Scenario: Android configuration is unavailable or invalid
- **WHEN** the Android configuration secret is missing, malformed base64, or not a valid matching Firebase JSON document
- **THEN** the workflow SHALL fail before invoking Gradle with a diagnostic that does not expose configuration contents

### Requirement: Delivery provisions iOS Firebase application configuration
The iOS delivery workflow SHALL reconstruct the iOS Firebase application configuration from a dedicated protected secret before framework compilation or Xcode archive steps.

#### Scenario: iOS configuration is available
- **WHEN** iOS delivery runs with a valid iOS configuration secret
- **THEN** `iosApp/iosApp/GoogleService-Info.plist` SHALL exist before the archive and SHALL be available to the filesystem-synchronized application target

#### Scenario: iOS configuration is unavailable or invalid
- **WHEN** the iOS configuration secret is missing, malformed base64, or not a valid matching Firebase plist document
- **THEN** the workflow SHALL fail before build or signing work with a diagnostic that does not expose configuration contents

### Requirement: Firebase application configuration remains protected
CI and delivery workflows SHALL keep platform Firebase application configuration separate from Firebase service-account credentials and SHALL NOT commit, print, cache, or upload decoded configuration files.

#### Scenario: Workflow artifacts and logs are produced
- **WHEN** CI or delivery completes or fails
- **THEN** Firebase application configuration contents SHALL NOT appear in logs or uploaded artifacts

#### Scenario: App Distribution credentials are used
- **WHEN** Android or iOS delivery authenticates to Firebase App Distribution
- **THEN** it SHALL continue to use the dedicated service-account credential rather than either platform application configuration secret
