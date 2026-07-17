## MODIFIED Requirements

### Requirement: Preserve multiplatform targets
The shared module SHALL provide Android, iOS device, and iOS simulator targets only, and every intermediate source set SHALL participate in each intended target compilation.

#### Scenario: Resolve platform implementations
- **WHEN** any supported target is compiled
- **THEN** each common `expect` declaration SHALL resolve to an `actual` implementation for that target

#### Scenario: Compile shared iOS implementations
- **WHEN** either iOS device or iOS simulator code is compiled
- **THEN** sources and dependencies declared in `iosMain` and the shared PowerSync-bearing intermediate source set SHALL participate in that compilation

#### Scenario: No Desktop, JS, or WasmJS targets remain
- **WHEN** `composeApp/build.gradle.kts` is inspected
- **THEN** it SHALL NOT declare a `jvm("desktop")`, `js`, or `wasmJs` Kotlin target, and SHALL NOT configure a `compose.desktop` application block
