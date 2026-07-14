# Dependency Management Specification

## Purpose

Define how Cofinance declares, aligns, verifies, and upgrades build tooling and third-party libraries.

## Current Implementation

The project uses `gradle/libs.versions.toml` as its version catalog, published BOMs for Supabase and Koin families, a Gradle wrapper, and platform-scoped Ktor engines. The July 2026 working tree targets Gradle 9.6.1, AGP 9.2.1, Kotlin 2.4.0, Compose Multiplatform 1.11.1, Ktor 3.5.1, Coil 3.5.0, Supabase Kotlin 3.6.0, Koin 4.2.2, PowerSync 1.13.0, CameraK 1.1, and kotlinx-datetime 0.8.0.

## Requirements

### Requirement: Centralize dependency versions
Direct dependency and plugin versions SHALL be declared in the version catalog unless a tool requires declaration in a settings or wrapper file.

#### Scenario: A dependency is upgraded
- **WHEN** a library or plugin version changes
- **THEN** the authoritative version SHALL be updated once and all related coordinates SHALL resolve consistently

### Requirement: Align dependency families
Libraries published as a coordinated family SHALL use the publisher's BOM or a single shared catalog version.

#### Scenario: Supabase or Koin modules resolve
- **WHEN** Gradle resolves multiple modules from either family
- **THEN** all modules in that family SHALL resolve to compatible versions

#### Scenario: Compose modules resolve
- **WHEN** Compose runtime, UI, foundation, resources, tooling, and the Gradle plugin resolve
- **THEN** their versions SHALL be intentionally compatible with the selected Kotlin compiler and Compose compiler plugin

### Requirement: Scope platform engines and native libraries
Platform-specific engines and native libraries SHALL be declared only in source sets that support them, and each target SHALL resolve exactly one intended Ktor engine.

#### Scenario: Ktor client resolves on Android
- **WHEN** Android constructs an HTTP client
- **THEN** OkHttp SHALL be the intended engine and CIO SHALL NOT be inherited from common code

#### Scenario: Ktor client resolves on iOS
- **WHEN** iOS constructs an HTTP client
- **THEN** Darwin SHALL be the intended engine and CIO SHALL NOT be inherited from common code

#### Scenario: Ktor client resolves on Desktop
- **WHEN** Desktop constructs an HTTP client
- **THEN** CIO SHALL be available as the intended engine

#### Scenario: PowerSync resolves
- **WHEN** native or web targets compile
- **THEN** PowerSync SHALL be present for Android, iOS, and Desktop and absent from unsupported web source sets

### Requirement: Support remote image loading explicitly
The dependency graph SHALL include Coil Compose integration and a compatible Coil network artifact for targets that render remote images.

#### Scenario: Render a remote profile image
- **WHEN** Coil receives an HTTP or HTTPS image URL
- **THEN** it SHALL fetch the image through the target's selected network engine

### Requirement: Verify upgrades before adoption
Dependency upgrades SHALL be validated against the project's supported targets and publisher migration guidance before being treated as complete.

#### Scenario: Build tooling is upgraded
- **WHEN** Gradle, AGP, Kotlin, or Compose changes
- **THEN** Android, Desktop, both iOS architectures, JS, and WasmJS SHALL compile and new deprecation or hierarchy diagnostics SHALL be reviewed

#### Scenario: Runtime library is upgraded
- **WHEN** a runtime dependency changes
- **THEN** affected critical flows SHALL be compiled and tested on every platform that consumes it

### Requirement: Prefer supported public build APIs
Build scripts SHALL avoid internal plugin APIs and settings scheduled for removal in the next major Gradle or AGP release.

#### Scenario: Build runs with all warnings enabled
- **WHEN** Gradle executes with `--warning-mode=all`
- **THEN** the build SHALL not rely on APIs or flags documented for removal in Gradle 10 or AGP 10
