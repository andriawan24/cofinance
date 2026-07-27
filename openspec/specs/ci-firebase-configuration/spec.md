# CI Firebase Configuration Specification

## Purpose

Define secure provisioning of platform Firebase application configuration for Cofinance continuous integration and delivery workflows.

## Requirements

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

### Requirement: Android delivery completes Firebase App Distribution
The Android delivery workflow SHALL build the release APK and distribute it through Firebase App Distribution to the `internal-testers` group for every main-branch push or successful manual dispatch.

#### Scenario: Tester group already exists
- **WHEN** Android delivery runs with valid build and Firebase credentials and the `internal-testers` group exists
- **THEN** Firebase SHALL accept the release and assign it to that group before the delivery job succeeds

#### Scenario: Tester group does not exist
- **WHEN** Android delivery runs with valid build and Firebase credentials and the `internal-testers` group is absent
- **THEN** the workflow SHALL create the group and distribute the release to it before the delivery job succeeds

#### Scenario: Firebase rejects distribution
- **WHEN** Firebase rejects group setup, release upload, or release assignment
- **THEN** the delivery job SHALL fail with a diagnostic that identifies the failed operation without exposing credentials

### Requirement: Required Android distribution inputs do not silently skip
The Android delivery workflow SHALL treat its Firebase application ID, application configuration, and service-account credential as required inputs.

#### Scenario: Distribution input is missing or invalid
- **WHEN** a required Android distribution input is missing, malformed, or does not match the application
- **THEN** the delivery workflow SHALL fail before reporting a successful delivery and SHALL NOT convert the missing outcome into a successful skipped step

#### Scenario: Delivery job reports success
- **WHEN** the Android delivery job completes successfully
- **THEN** its executed steps SHALL provide evidence that Firebase accepted the release distribution
