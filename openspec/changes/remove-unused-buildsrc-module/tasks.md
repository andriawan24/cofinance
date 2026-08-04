## 1. Confirm buildSrc is unused

- [x] 1.1 Confirm `buildSrc/` contains no source files — only `build.gradle.kts` and `settings.gradle.kts` outside `build/` and `.gradle/` (design Context)
- [x] 1.2 Grep the repository for `buildSrc` across `*.kts`, `*.toml`, `*.properties`, `*.yml`/`*.yaml`, and confirm the only hits are archived OpenSpec prose (design Risks: hidden consumer)
- [x] 1.3 Capture the pre-removal configured project set via `./gradlew projects` as the baseline for the module-set scenario (spec: No empty build-logic module is configured)

## 2. Remove the module

- [x] 2.1 Delete `buildSrc/build.gradle.kts` and `buildSrc/settings.gradle.kts` (proposal: What Changes; design Decision 1)
- [x] 2.2 Delete the generated `buildSrc/build/` and `buildSrc/.gradle/` directories so no `buildSrc` directory remains for Gradle to discover (design Decision 2)
- [x] 2.3 Confirm `settings.gradle.kts`, `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`, and `androidApp/build.gradle.kts` need no edits, since `buildSrc` was implicit and unreferenced (proposal: Impact — Unaffected)

## 3. Verify the build

- [x] 3.1 Run `./gradlew help --warning-mode=all`; confirm success, no `buildSrc` project configured, and no new accessor or deprecation warning (design Verification Evidence; Risks: accessor warning)
- [x] 3.2 Run `./gradlew projects` and confirm the configured set is exactly the root project, `composeApp`, and `androidApp` (spec: No empty build-logic module is configured)
- [x] 3.3 Run `./gradlew :androidApp:assembleDebug` and confirm the debug APK assembles (spec: Build the Android application)
- [ ] 3.4 Run `./gradlew :composeApp:compileKotlinIosArm64` and confirm it compiles (design Verification Evidence — iOS device) — **BLOCKED** by a pre-existing compile error at `composeApp/src/iosMain/kotlin/id/andriawan/cofinance/data/local/DatabaseBuilder.ios.kt:11:58` that is present at HEAD and unrelated to this change; see 3.7
- [ ] 3.5 Run `./gradlew :composeApp:compileKotlinIosSimulatorArm64` and confirm it compiles (design Verification Evidence — iOS simulator) — **BLOCKED** by the same pre-existing error; see 3.7
- [x] 3.6 Re-run the grep from 1.2 and confirm no `buildSrc` reference remains outside archived OpenSpec history and this change's artifacts (design Migration Plan step 5)
- [x] 3.7 Substitute control test for 3.4/3.5: restore `buildSrc` from HEAD, recompile `iosSimulatorArm64`, and confirm the identical error occurs with and without `buildSrc`, establishing that this change is neutral for the iOS targets (design: platform consequences — `buildSrc` contributed no code to any Kotlin target)

## 4. Finalize specifications

- [x] 4.1 Run `openspec validate "remove-unused-buildsrc-module" --strict` and resolve any reported issue (AGENTS.md completion gate)
- [x] 4.2 Sync the `android-kmp-build` delta into `openspec/specs/android-kmp-build/spec.md` so the module-set boundary lands in the main spec (proposal: Modified Capabilities)
- [x] 4.3 Record verification evidence from section 3 in `verification.md`, including the blocked status and control-test result for 3.4/3.5 (AGENTS.md completion gate)
