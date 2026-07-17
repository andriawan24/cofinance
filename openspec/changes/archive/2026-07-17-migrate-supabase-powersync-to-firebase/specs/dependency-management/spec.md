## MODIFIED Requirements

### Requirement: Align dependency families
Libraries published as a coordinated family SHALL use the publisher's BOM or a single shared catalog version.

#### Scenario: Firebase or Koin modules resolve
- **WHEN** Gradle resolves multiple modules from either family
- **THEN** all modules in that family SHALL resolve to compatible versions

#### Scenario: Compose modules resolve
- **WHEN** Compose runtime, UI, foundation, resources, tooling, and the Gradle plugin resolve
- **THEN** their versions SHALL be intentionally compatible with the selected Kotlin compiler and Compose compiler plugin

### Requirement: Scope platform engines and native libraries
Platform-specific engines and native libraries SHALL be declared only in source sets or application targets that support them, and each target SHALL resolve exactly one intended Ktor engine.

#### Scenario: Ktor client resolves on Android
- **WHEN** Android constructs an HTTP client
- **THEN** OkHttp SHALL be the intended engine and CIO SHALL NOT be inherited from common code

#### Scenario: Ktor client resolves on iOS
- **WHEN** iOS constructs an HTTP client
- **THEN** Darwin SHALL be the intended engine and CIO SHALL NOT be inherited from common code

#### Scenario: Firebase resolves
- **WHEN** Android or iOS targets compile
- **THEN** Firebase Authentication, Firestore, and Storage SHALL resolve through the shared Kotlin API and the official target SDKs

#### Scenario: Firebase platform configuration resolves
- **WHEN** an Android or iOS application target is built
- **THEN** Android SHALL process `google-services.json` with the Google Services plugin and iOS SHALL bundle `GoogleService-Info.plist` for `FirebaseApp.configure()`

### Requirement: Verify upgrades before adoption
Dependency replacements and upgrades SHALL be validated against the project's supported targets and publisher migration guidance before being treated as complete.

#### Scenario: Build tooling is upgraded
- **WHEN** Gradle, AGP, Kotlin, or Compose changes
- **THEN** Android and both iOS architectures SHALL compile and new deprecation or hierarchy diagnostics SHALL be reviewed

#### Scenario: Runtime library is replaced or upgraded
- **WHEN** a runtime dependency changes
- **THEN** affected source sets SHALL compile and critical flows SHALL have requirement-level verification evidence for every platform that consumes it
