## MODIFIED Requirements

### Requirement: Preserve multiplatform targets
The shared module SHALL provide Android, iOS device, iOS simulator, Desktop JVM, JavaScript, and WasmJS targets, and every intermediate source set SHALL participate in each intended target compilation.

#### Scenario: Resolve platform implementations
- **WHEN** any supported target is compiled
- **THEN** each common `expect` declaration SHALL resolve to an `actual` implementation for that target

#### Scenario: Compile shared iOS implementations
- **WHEN** either iOS device or iOS simulator code is compiled
- **THEN** sources and dependencies declared in `iosMain` and `nonWebMain` SHALL participate in that compilation

### Requirement: Use supported build APIs
The build SHALL use public Gradle, Kotlin, and Android Gradle Plugin APIs that remain supported by the next announced major release.

#### Scenario: Run warning-enabled configuration
- **WHEN** the build is configured with all warnings enabled
- **THEN** it SHALL NOT report project-authored APIs or compatibility flags scheduled for removal in Gradle 10 or AGP 10

