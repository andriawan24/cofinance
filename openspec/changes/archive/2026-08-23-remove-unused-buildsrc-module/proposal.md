## Why

`buildSrc/` no longer contains any build logic. Its only source file, `ComposeStorybookGeneratorTask.kt`, was deleted by the archived `remove-desktop-web-platforms` change, leaving just `build.gradle.kts` (applying `kotlin-dsl` and declaring repositories) and `settings.gradle.kts` (setting the root name `cofinance-build-logic`). Gradle treats `buildSrc` as an implicit included build, so every invocation still configures it, resolves the Kotlin DSL toolchain, and compiles an empty source set — pure overhead that also invalidates the whole build's configuration cache whenever `buildSrc` inputs change.

## What Changes

- Delete the `buildSrc/` directory entirely, including `buildSrc/build.gradle.kts`, `buildSrc/settings.gradle.kts`, and its stale `build/` and `.gradle/` output directories.
- Record in the build specification that the project's module set is exactly `composeApp` and `androidApp`, with no build-logic module, so the empty module cannot silently reappear.
- No change to `settings.gradle.kts`, the version catalog, or either module's build script: nothing declares a dependency on `buildSrc`, and the root `settings.gradle.kts` never referenced it (`buildSrc` is implicit, not `include`d).

Not a breaking change: `buildSrc` published no plugins, tasks, or types that any build script consumes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `android-kmp-build`: the "Separate Android application and shared KMP library" requirement gains an explicit module-set boundary — the build SHALL contain only the two shipped modules and SHALL NOT carry a `buildSrc` (or other build-logic) module that contributes no build logic.

## Impact

- **Removed**: `buildSrc/build.gradle.kts`, `buildSrc/settings.gradle.kts`, `buildSrc/build/`, `buildSrc/.gradle/`.
- **Build behavior**: one fewer implicit included build to configure and compile on every Gradle invocation; the `kotlin-dsl` plugin and `gradlePluginPortal()`/`mavenCentral()` repositories declared in `buildSrc/build.gradle.kts` stop being resolved.
- **Unaffected**: application behavior on Android and iOS, `settings.gradle.kts`, `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`, `androidApp/build.gradle.kts`, and all CI workflows — none reference `buildSrc`.
- **Non-goals**: no dependency upgrades, no changes to the version catalog, no migration to a `build-logic` included build (there is no shared build logic to host), and no cleanup of the unrelated uncommitted working-tree changes present alongside this work.
