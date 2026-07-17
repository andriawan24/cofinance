## Context

`composeApp/build.gradle.kts` declares five Kotlin targets: `android`, `iosArm64`/`iosSimulatorArm64`, `jvm("desktop")`, `js`, and `wasmJs`. A prior in-flight (uncommitted) change already stripped the storybook code-generation task wiring from this file and deleted `buildSrc/src/main/kotlin/ComposeStorybookGeneratorTask.kt`, `composeApp/storybook/`, `composeApp/webpack.config.d/`, and the `kotlin-js-store/` lockfiles, but the `jvm("desktop")`, `js { ... }`, and `wasmJs { ... }` target declarations, their source sets, and `compose.desktop { ... }` config block are still present. Only Android and iOS are shipped; Desktop/JS/WasmJS exist for storybook/demo purposes that are being removed.

`applyDefaultHierarchyTemplate()` auto-wires `jvmMain` (used by `desktopMain` alias), `jsMain`, `wasmJsMain`, and the shared `webMain`/`nonJvmMain` intermediate sets. A custom `nonWebMain` source set is manually created and wired to `androidMain`, `iosMain`, and `desktopMain` for PowerSync dependencies.

## Goals / Non-Goals

**Goals:**
- Remove the `jvm("desktop")`, `js { ... }`, and `wasmJs { ... }` target blocks and their source sets from `composeApp/build.gradle.kts`.
- Remove the `compose.desktop { application { ... } }` config block (desktop packaging only).
- Prune `gradle/libs.versions.toml` entries that are exclusively consumed by the removed targets (desktop-only Compose artifacts, `ktor-client-cio`, storybook-related entries already partially removed).
- Simplify the `nonWebMain` intermediate source set to only bridge `androidMain`/`iosMain` (or remove it if collapsing into `commonMain` is safe, since PowerSync no longer needs to exclude a web target).
- Update `specs/android-kmp-build/spec.md` "Preserve multiplatform targets" requirement to list Android and iOS only.

**Non-Goals:**
- No change to Android or iOS target configuration, signing, or dependency versions.
- No change to shared business logic, only build topology and dead platform code.
- Not re-auditing or upgrading dependency versions beyond removing unused ones.

## Decisions

- **Keep `nonWebMain` as a two-target bridge rather than collapsing into `commonMain`.** Android and iOS still need PowerSync dependencies that common code doesn't need generically applied; keeping a named intermediate set documents intent and avoids surprising future readers. Alternative considered: inline PowerSync deps directly into `androidMain.dependencies`/`iosMain.dependencies` — rejected because it duplicates two dependency blocks instead of one and loses the "why" comment location.
- **Delete `compose.desktop { }` block entirely rather than leaving it unused.** Gradle would otherwise still evaluate a dangling desktop application config with no `jvm("desktop")` target backing it, which is invalid. Alternative: keep target but strip application block — rejected, contradicts the goal of full desktop removal.
- **Prune `libs.versions.toml` conservatively.** Only remove entries with zero remaining references after the target removal (verified via grep across `*.gradle.kts` files), to avoid breaking Android/iOS by mistake.

## Risks / Trade-offs

- [Risk] A version-catalog entry removed here is silently still referenced by androidApp or root build scripts → Mitigation: grep every `libs.*` catalog key across the whole repo (not just composeApp) before deleting it from `libs.versions.toml`.
- [Risk] Removing `nonWebMain` incorrectly could break the Android/iOS PowerSync dependency resolution → Mitigation: keep the intermediate set, only remove target-specific plumbing (desktop leg), and verify with `./gradlew :composeApp:compileDebugKotlinAndroid` and `./gradlew :composeApp:compileKotlinIosArm64` (or equivalent) after the change.
- [Risk] Kotlin's `applyDefaultHierarchyTemplate()` may emit warnings/errors if it expects at least one non-JVM/non-native target family present in certain versions → Mitigation: build after removal and confirm no template-related Gradle errors; adjust hierarchy call only if evidence requires it.

## Migration Plan

1. Remove `jvm("desktop")`, `js { ... }`, `wasmJs { ... }` blocks and `compose.desktop { }` from `composeApp/build.gradle.kts`.
2. Remove `desktopMain`/`jsMain`/`wasmJsMain` source-set references and dependency blocks; adjust `nonWebMain` wiring to only depend on `androidMain`/`iosMain`.
3. Grep `gradle/libs.versions.toml` catalog keys used only by removed targets/tooling and delete them; re-run a repo-wide grep to confirm no remaining references.
4. Confirm no leftover files reference removed targets (already-deleted storybook/webpack/yarn-lock files from the in-flight change stay deleted).
5. Update `openspec/specs/android-kmp-build/spec.md` requirement text (via delta spec in this change) to reflect Android + iOS only.
6. Build/verify: `./gradlew :composeApp:assembleDebug` (or Android compile task) and an iOS compile/link task; run `./gradlew build` if time allows to catch cross-module breakage.

Rollback: revert the commit(s) for this change; no data migration or runtime state involved, pure build-config change.

## Open Questions

None — scope is bounded to build configuration and its directly dependent spec text.
