## ADDED Requirements

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
