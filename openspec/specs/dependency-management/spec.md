# Dependency Management Specification

## Purpose

Define how Cofinance declares, aligns, verifies, and upgrades build tooling and third-party libraries.

## Current Implementation

The project uses `gradle/libs.versions.toml` as its version catalog, published BOMs for Firebase Android and Koin families, a Gradle wrapper, platform-scoped Ktor engines, and Swift Package Manager for Firebase on iOS. The July 2026 working tree targets Gradle 9.6.1, AGP 9.3.0, Kotlin 2.4.0, Compose Multiplatform 1.11.1, Ktor 3.5.1, Coil 3.5.0, Firebase Kotlin SDK 2.4.0, Koin 4.2.2, CameraK 1.1, and kotlinx-datetime 0.8.0.

## Requirements

### Requirement: Centralize dependency versions
Direct dependency and plugin versions SHALL be declared in the version catalog unless a tool requires declaration in a settings or wrapper file.

#### Scenario: A dependency is upgraded
- **WHEN** a library or plugin version changes
- **THEN** the authoritative version SHALL be updated once and all related coordinates SHALL resolve consistently

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

### Requirement: Support remote image loading explicitly
The dependency graph SHALL include Coil Compose integration and a compatible Coil network artifact for targets that render remote images.

#### Scenario: Render a remote profile image
- **WHEN** Coil receives an HTTP or HTTPS image URL
- **THEN** it SHALL fetch the image through the target's selected network engine

### Requirement: Verify upgrades before adoption
Dependency replacements and upgrades SHALL be validated against the project's supported targets and publisher migration guidance before being treated as complete.

#### Scenario: Build tooling is upgraded
- **WHEN** Gradle, AGP, Kotlin, or Compose changes
- **THEN** Android and both iOS architectures SHALL compile and new deprecation or hierarchy diagnostics SHALL be reviewed

#### Scenario: Runtime library is replaced or upgraded
- **WHEN** a runtime dependency changes
- **THEN** affected source sets SHALL compile and critical flows SHALL have requirement-level verification evidence for every platform that consumes it

### Requirement: Prefer supported public build APIs
Build scripts SHALL avoid internal plugin APIs and settings scheduled for removal in the next major Gradle or AGP release.

#### Scenario: Build runs with all warnings enabled
- **WHEN** Gradle executes with `--warning-mode=all`
- **THEN** the build SHALL not rely on APIs or flags documented for removal in Gradle 10 or AGP 10

### Requirement: Automation remains aligned with the supported stack
Continuous integration and delivery workflows SHALL use build inputs, runtime toolchains, and verification tasks that match the current project configuration and supported targets.

#### Scenario: Automation generates local build configuration
- **WHEN** CI or a platform delivery workflow creates `local.properties`
- **THEN** it SHALL provide only the current required build configuration and platform SDK location without provisioning removed Supabase or PowerSync settings

#### Scenario: Linux CI verifies the KMP project
- **WHEN** the CI workflow runs on Linux
- **THEN** it SHALL execute Android lint and Android-hosted unit tests without claiming to execute iOS tests that require macOS

#### Scenario: Android delivery invokes build and distribution tools
- **WHEN** the Android release is built and distributed
- **THEN** the workflow SHALL use a Java runtime compatible with the configured JVM target and a pinned Firebase CLI version running on a supported Node.js runtime

#### Scenario: GitHub-hosted jobs invoke official actions
- **WHEN** a CI or delivery job uses official checkout, Java setup, Node setup, cache, or artifact actions
- **THEN** it SHALL use a major that runs on the supported Node 24 action runtime without a Node 20 deprecation override
