# Verification: remove-unused-buildsrc-module

Baseline commit: `3d02dd5` (`docs: propose remove-unused-buildsrc-module change`). Working tree carried no unrelated modifications at the time of verification.

## Pre-removal evidence

`buildSrc/` held two files and no source directory:

```
buildSrc/build.gradle.kts
buildSrc/settings.gradle.kts
```

`ls buildSrc/src` → `No such file or directory`.

A repository-wide grep for `buildSrc` across `*.kts`, `*.toml`, `*.properties`, `*.yml`, and `*.yaml` returned no hits, confirming no build script, version catalog entry, Gradle property, or CI workflow referenced it. `buildSrc` was never `include`d in `settings.gradle.kts` because Gradle discovers it implicitly by directory name.

`./gradlew projects` before removal quantified the overhead — eight tasks executed for a module with nothing in it:

```
> Task :buildSrc:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :buildSrc:compileKotlin NO-SOURCE
> Task :buildSrc:compileJava NO-SOURCE
> Task :buildSrc:compileGroovy NO-SOURCE
> Task :buildSrc:pluginDescriptors UP-TO-DATE
> Task :buildSrc:processResources NO-SOURCE
> Task :buildSrc:classes UP-TO-DATE
> Task :buildSrc:jar UP-TO-DATE
```

The project hierarchy already contained only `:androidApp` and `:composeApp`, confirming `buildSrc` contributed no Gradle project.

## Removal

`buildSrc/build.gradle.kts` and `buildSrc/settings.gradle.kts` removed via `git rm`; the generated `buildSrc/build/` and `buildSrc/.gradle/` directories removed so no `buildSrc` directory remains for Gradle to discover.

`git diff --stat HEAD` scoped to build files reported exactly:

```
buildSrc/build.gradle.kts    | 8 --------
buildSrc/settings.gradle.kts | 1 -
2 files changed, 9 deletions(-)
```

No edit was required to `settings.gradle.kts`, the root `build.gradle.kts`, `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`, or `androidApp/build.gradle.kts`.

## Post-removal results

| Task | Command | Result |
| --- | --- | --- |
| 3.1 | `./gradlew help --warning-mode=all` | **PASS** — `BUILD SUCCESSFUL in 14s`, `1 actionable task` (down from 9), no `:buildSrc:*` task, no new warning |
| 3.2 | `./gradlew projects` | **PASS** — hierarchy is exactly root `Cofinance`, `:androidApp`, `:composeApp` |
| 3.3 | `./gradlew :androidApp:assembleDebug` | **PASS** — `BUILD SUCCESSFUL in 1m 53s`, 65 tasks executed, debug APK packaged |
| 3.4 | `./gradlew :composeApp:compileKotlinIosArm64` | **PASS** at `60f321c` — `BUILD SUCCESSFUL`, compile task executed, warnings only (see below) |
| 3.5 | `./gradlew :composeApp:compileKotlinIosSimulatorArm64` | **PASS** at `60f321c` — `BUILD SUCCESSFUL`, compile task executed, warnings only (see below) |
| 3.6 | grep for `buildSrc` | **PASS** — no hit outside `openspec/changes/archive/` prose and this change's own artifacts |
| 3.7 | Control test (see below) | **PASS** — change proven neutral for iOS targets; gating defect since resolved upstream |
| 4.1 | `openspec validate "remove-unused-buildsrc-module" --strict` | **PASS** — `Change 'remove-unused-buildsrc-module' is valid` |

### Warning-mode and configuration-cache notes

The only message emitted under `--warning-mode=all` is `Type-safe project accessors is an incubating feature`, which was present identically in the pre-removal baseline. The accessor warning that the archived `modernize-build-and-dependencies` change had addressed by naming the `buildSrc` root project did **not** reappear, confirming the design's assessment that removing the directory makes that fix moot rather than regressing it.

The configuration cache invalidated exactly once, reporting `configuration cache cannot be reused because file 'buildSrc/settings.gradle.kts' has been removed`, then stored a fresh entry. This is the expected self-healing behavior anticipated in the design's risk table; no stale `buildSrc` classpath entry survived.

## iOS verification (previously blocked, now resolved)

At the original baseline (`3d02dd5` … `3ad72b5`), both iOS compile tasks failed on a single pre-existing error:

```
e: composeApp/src/iosMain/kotlin/id/andriawan/cofinance/data/local/DatabaseBuilder.ios.kt:11:58
   This declaration needs opt-in. Its usage must be marked with '@kotlinx.cinterop.ExperimentalForeignApi'
   or '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'
```

The offending call is `NSFileManager.defaultManager.URLForDirectory(...)`, a `platform.Foundation` cinterop API. The file is committed at HEAD and was not touched by this change.

### Control test (task 3.7)

To establish that this change is not the cause rather than merely assuming it, `buildSrc` was restored from HEAD via `git checkout HEAD -- buildSrc` and `:composeApp:compileKotlinIosSimulatorArm64` was re-run. The compile failed with the byte-identical error at the same file and column, with `buildSrc` present. `buildSrc` was then deleted again.

This confirms the design's platform analysis: `buildSrc` contained no source and therefore contributed no compiler plugin, opt-in flag, or code to any Kotlin target, so the iOS compilation inputs are unchanged by its removal.

### Root cause and resolution

The defect was not a Room or `androidx.sqlite` API mismatch. `NSFileManager.URLForDirectory(directory:inDomain:appropriateForURL:create:error:)` takes its `error` parameter as `CPointer<ObjCObjectVar<NSError?>>?`, a `kotlinx.cinterop` type marked `@ExperimentalForeignApi`. Kotlin/Native therefore requires the call site to opt in; `databaseBuilder` in `DatabaseBuilder.ios.kt` carried no opt-in, and the compiler reported the error at the `URLForDirectory` call token (line 11, column 58 in the pre-fix file).

The correct fix is the opt-in annotation, not a cast or a `@Suppress`. It landed upstream in commit `c6a8881` (`refactor: tidy finance data layer naming and formatting`), which added `import kotlinx.cinterop.ExperimentalForeignApi` and `@OptIn(ExperimentalForeignApi::class)` on `databaseBuilder`. `c6a8881` is a descendant of the buildSrc-removal commit `3ad72b5` and an ancestor of the current branch tip `60f321c`, so this change's branch already contains it; **no source edit was required here.**

Re-verified at `60f321c` with `local.properties` supplied (the build requires `google_auth_client_id`, otherwise configuration fails at `composeApp/build.gradle.kts:140` before any compilation):

- Reproduction: restoring the pre-`c6a8881` `DatabaseBuilder.ios.kt` and running `:composeApp:compileKotlinIosSimulatorArm64` reproduced the byte-identical `11:58` opt-in error, then the file was restored.
- `./gradlew :composeApp:compileKotlinIosArm64` → `BUILD SUCCESSFUL`, `:composeApp:compileKotlinIosArm64` executed.
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` → `BUILD SUCCESSFUL`, `:composeApp:compileKotlinIosSimulatorArm64` executed.

Both runs emit only `w:` warnings about `expect`/`actual` classes being in Beta (KT-61573), which are pre-existing and unrelated. Tasks 3.4 and 3.5 are therefore checked on their own stated evidence — a successful compile — rather than on the 3.7 substitute.

## Specification sync

The `android-kmp-build` delta was applied to `openspec/specs/android-kmp-build/spec.md`. The "Separate Android application and shared KMP library" requirement now states the complete module set and gains two scenarios: "No empty build-logic module is configured" (satisfied by the 3.2 evidence above) and "Build logic is reintroduced" (a forward-looking guard against an empty module returning).

No `dependency-management` delta was written, per design Decision 4: no requirement in that capability describes `buildSrc`, and none became false.
