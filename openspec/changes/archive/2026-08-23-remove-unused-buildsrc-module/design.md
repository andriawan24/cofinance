## Context

`buildSrc/` currently holds exactly two files:

- `buildSrc/build.gradle.kts` — applies the `kotlin-dsl` plugin and declares `gradlePluginPortal()` + `mavenCentral()`.
- `buildSrc/settings.gradle.kts` — sets `rootProject.name = "cofinance-build-logic"`.

There is no `buildSrc/src/` directory. The single class that once lived there, `ComposeStorybookGeneratorTask.kt`, was deleted by the archived `2026-07-17-remove-desktop-web-platforms` change along with the storybook and JS/WasmJS tooling it supported. The root name was set by the archived `2026-07-14-modernize-build-and-dependencies` change to silence a type-safe-accessor cache warning — a fix that becomes moot once the directory is gone.

Gradle gives `buildSrc` special treatment: it is an implicit included build discovered by directory name, never declared via `include(...)`. The root `settings.gradle.kts` therefore contains no reference to it, and grep across `*.kts`, `*.toml`, `*.yml`, `*.properties`, and workflow files finds no consumer anywhere in the repository — the only hits are historical prose in archived OpenSpec documents. Removal is consequently a pure deletion with no call sites to update.

Constraints: the project builds Android and iOS device/simulator targets from `composeApp`, with `androidApp` as the Android application module. `settings.gradle.kts` enables `TYPESAFE_PROJECT_ACCESSORS`, which is what made the missing `buildSrc` root name warn in the first place.

## Goals / Non-Goals

**Goals:**

- Delete `buildSrc/` and its generated `build/` and `.gradle/` directories so Gradle stops configuring and compiling an empty included build on every invocation.
- Record the module-set boundary in `android-kmp-build` so an empty build-logic module cannot silently return.
- Verify that Android and both iOS architectures still configure and compile after removal.

**Non-Goals:**

- Introducing a `build-logic` composite/included build as a replacement. There is no shared build logic to host; adding an empty replacement would recreate the exact overhead being removed.
- Any dependency, version-catalog, or plugin upgrade.
- Changes to `settings.gradle.kts`, `composeApp/build.gradle.kts`, `androidApp/build.gradle.kts`, or CI workflows.
- Touching the unrelated uncommitted working-tree changes that exist alongside this work.

## Decisions

**Decision 1: Delete `buildSrc/` outright rather than converting it to an included `build-logic` build.**

The conventional migration for a `buildSrc` module is `includeBuild("build-logic")`, which improves incremental behavior for projects that actually own convention plugins. This project owns none — the directory has zero source files. Converting would preserve the configuration and compilation cost this change exists to eliminate, plus add an explicit `includeBuild` line to `settings.gradle.kts`. Outright deletion is the only option that achieves the goal. If shared build logic is ever needed again, an included `build-logic` build is the pattern to reach for at that time, not a resurrected `buildSrc`.

**Decision 2: Delete the generated `buildSrc/build/` and `buildSrc/.gradle/` directories along with the sources.**

These are Gradle outputs, not source. Leaving them behind would keep a stale `buildSrc` directory on disk, which Gradle would still detect as an implicit included build on the next invocation — defeating the change. Both are safely regenerable in principle and contain nothing project-authored.

**Decision 3: Express the outcome as a modification to the existing "Separate Android application and shared KMP library" requirement in `android-kmp-build`, not as a new capability.**

That requirement already defines the module boundary (`androidApp` for the app entry point, `composeApp` for shared code). Stating the complete module set — and the absence of a build-logic module — belongs in the same requirement rather than in a parallel one that would drift. This mirrors how the same spec already asserts the absence of removed Kotlin targets via a "No Desktop, JS, or WasmJS targets remain" scenario.

**Decision 4: No delta spec for `dependency-management`.**

`buildSrc/build.gradle.kts` declared repositories and the `kotlin-dsl` plugin, so removal touches dependency resolution in a literal sense. But no requirement in `dependency-management` describes `buildSrc` or a build-logic module, and none becomes false: versions stay centralized in the catalog, families stay aligned, and the "Prefer supported public build APIs" requirement is unaffected because `buildSrc` used no deprecated API. Writing a delta with no requirement change would add noise at archive time.

**Platform consequences:** none are target-specific. `buildSrc` contributed no code to any Kotlin target, so Android, `iosArm64`, and `iosSimulatorArm64` compilation inputs are byte-identical before and after. The observable difference is confined to the Gradle build itself — one fewer included build in the configuration phase.

## Risks / Trade-offs

- **A hidden consumer of `buildSrc` exists that grep missed** → Mitigated by the mechanism: `buildSrc` contains no source, so it can export no plugin, task type, or class for anything to consume. A repository-wide grep over build scripts, the catalog, properties, and workflows found only archived prose. Configuration failure would surface immediately at `./gradlew help`, before any compilation.
- **Removing `rootProject.name = "cofinance-build-logic"` reintroduces the type-safe project accessor warning it was added to fix** → Not applicable: the warning was emitted *because* an unnamed `buildSrc` existed. With no `buildSrc` at all there is no unnamed included build to warn about. Verified by running the build with `--warning-mode=all` and confirming no new warning appears.
- **Stale Gradle caches keep a phantom `buildSrc` classpath entry** → Mitigated by verifying with a configuration-cache-eligible run after deletion; if a stale entry surfaces, the cache invalidates itself on the changed build layout.
- **Trade-off: no home for future convention plugins.** Accepted — an empty module is not a useful placeholder, and the replacement pattern (`includeBuild("build-logic")`) is well understood and cheap to add when there is actual logic to put in it.

## Migration Plan

1. Delete `buildSrc/` (sources plus `build/` and `.gradle/` outputs).
2. Verify configuration: `./gradlew help --warning-mode=all` succeeds with no new warnings and no `buildSrc` in the task graph.
3. Verify Android: `./gradlew :androidApp:assembleDebug` succeeds.
4. Verify iOS: compile both `iosArm64` and `iosSimulatorArm64` shared-module targets.
5. Confirm no file in the repository references `buildSrc` outside archived OpenSpec history.

**Rollback:** the change is two small files under version control; `git revert` restores them, and Gradle re-detects `buildSrc` on the next invocation with no other coordination required.

## Verification Evidence

| Platform / scope | Command | Expected evidence |
| --- | --- | --- |
| Gradle configuration | `./gradlew help --warning-mode=all` | Build succeeds; no `buildSrc` project configured; no new deprecation or accessor warnings |
| Android | `./gradlew :androidApp:assembleDebug` | Debug APK assembles successfully |
| iOS device | `./gradlew :composeApp:compileKotlinIosArm64` | Compiles successfully |
| iOS simulator | `./gradlew :composeApp:compileKotlinIosSimulatorArm64` | Compiles successfully |
| Repository hygiene | `grep -rn "buildSrc"` over build scripts, catalog, properties, workflows | No hits outside `openspec/changes/archive/` prose and this change's own artifacts |

## Open Questions

None. The directory has no source files and no consumers; scope and outcome are fully determined.
