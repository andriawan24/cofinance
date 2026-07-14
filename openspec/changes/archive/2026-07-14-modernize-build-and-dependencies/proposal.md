## Why

The July 2026 dependency refresh compiles on Android and Desktop, but iOS compilation fails because shared iOS actual implementations are excluded by the source-set hierarchy. Capturing and resolving this together with Gradle 10 and AGP 10 deprecations keeps the project on supported public build APIs and restores the advertised platform matrix.

## What Changes

- Repair Kotlin Multiplatform source-set hierarchy wiring so shared iOS and native code participates in every intended compilation.
- Remove obsolete AGP compatibility flags and adopt current defaults deliberately.
- Replace Gradle APIs deprecated for removal in Gradle 10, including delegated container access and legacy project dependency notation.
- Make HTTP engines target-specific and add the Coil network integration required for remote images.
- Consolidate Compose coordinates and tighten `api` versus `implementation` exposure.
- Replace internal AGP property-loading APIs with provider-backed secret inputs and environment fallbacks.
- Add a repeatable dependency verification matrix covering Android, Desktop, iOS, JS, and WasmJS.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `android-kmp-build`: Require every platform source set to participate in its target compilation and require supported build APIs.
- `dependency-management`: Strengthen engine scoping, upgrade verification, dependency alignment, and next-major compatibility requirements.

## Impact

Affected areas include `composeApp/build.gradle.kts`, `androidApp/build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`, `buildSrc`, the version catalog, CI build commands, and image-loading configuration. No product-facing finance behavior is intended to change.
