# Android KMP Build Specification

## Purpose

Define the supported module boundary and build behavior for the Android target of the Kotlin Multiplatform application.

## Requirements

### Requirement: Separate Android application and shared KMP library
The system SHALL keep the Android application entry point in `androidApp` and shared multiplatform code in `composeApp`.

#### Scenario: Build the Android application
- **WHEN** the Android debug application is assembled
- **THEN** `androidApp` SHALL package the launcher, manifest, signing configuration, and shared `composeApp` dependency

#### Scenario: Compile shared Android code
- **WHEN** the Android target of `composeApp` is compiled
- **THEN** it SHALL use `com.android.kotlin.multiplatform.library` and expose shared UI and platform actual implementations to `androidApp`

### Requirement: Preserve multiplatform targets
The shared module SHALL provide Android, iOS device, iOS simulator, Desktop JVM, JavaScript, and WasmJS targets.

#### Scenario: Resolve platform implementations
- **WHEN** any supported target is compiled
- **THEN** each common `expect` declaration SHALL resolve to an `actual` implementation for that target

### Requirement: Keep build-time secrets out of source control
The build SHALL source Supabase, Gemini, Google authentication, and PowerSync configuration from ignored local configuration or environment-backed secret inputs.

#### Scenario: Build configuration is generated
- **WHEN** BuildKonfig generates constants
- **THEN** required values SHALL be present without being committed, rendered in documentation, screenshots, or emitted to CI logs

