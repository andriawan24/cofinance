## MODIFIED Requirements

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

