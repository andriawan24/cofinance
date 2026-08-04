## MODIFIED Requirements

### Requirement: Separate Android application and shared KMP library
The system SHALL keep the Android application entry point in `androidApp` and shared multiplatform code in `composeApp`, and the Gradle build SHALL contain no additional module that contributes no build logic or product code.

#### Scenario: Build the Android application
- **WHEN** the Android debug application is assembled
- **THEN** `androidApp` SHALL package the launcher, manifest, signing configuration, and shared `composeApp` dependency

#### Scenario: Compile shared Android code
- **WHEN** the Android target of `composeApp` is compiled
- **THEN** it SHALL use `com.android.kotlin.multiplatform.library` and expose shared UI and platform actual implementations to `androidApp`

#### Scenario: No empty build-logic module is configured
- **WHEN** Gradle configures the build
- **THEN** the configured project set SHALL be exactly the root project, `composeApp`, and `androidApp`
- **AND** no `buildSrc` directory SHALL be present for Gradle to discover as an implicit included build

#### Scenario: Build logic is reintroduced
- **WHEN** shared build logic such as a convention plugin is needed again
- **THEN** it SHALL be hosted in a module that declares that logic in source, rather than in a module that exists without contributing any
