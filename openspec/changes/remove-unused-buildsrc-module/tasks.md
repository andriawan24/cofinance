## 1. Confirm buildSrc is unused

- [ ] 1.1 Confirm `buildSrc/` contains no source files — only `build.gradle.kts` and `settings.gradle.kts` outside `build/` and `.gradle/` (design Context)
- [ ] 1.2 Grep the repository for `buildSrc` across `*.kts`, `*.toml`, `*.properties`, `*.yml`/`*.yaml`, and confirm the only hits are archived OpenSpec prose (design Risks: hidden consumer)
- [ ] 1.3 Capture the pre-removal configured project set via `./gradlew projects` as the baseline for the module-set scenario (spec: No empty build-logic module is configured)

## 2. Remove the module

- [ ] 2.1 Delete `buildSrc/build.gradle.kts` and `buildSrc/settings.gradle.kts` (proposal: What Changes; design Decision 1)
- [ ] 2.2 Delete the generated `buildSrc/build/` and `buildSrc/.gradle/` directories so no `buildSrc` directory remains for Gradle to discover (design Decision 2)
- [ ] 2.3 Confirm `settings.gradle.kts`, `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`, and `androidApp/build.gradle.kts` need no edits, since `buildSrc` was implicit and unreferenced (proposal: Impact — Unaffected)

## 3. Verify the build

- [ ] 3.1 Run `./gradlew help --warning-mode=all`; confirm success, no `buildSrc` project configured, and no new accessor or deprecation warning (design Verification Evidence; Risks: accessor warning)
- [ ] 3.2 Run `./gradlew projects` and confirm the configured set is exactly the root project, `composeApp`, and `androidApp` (spec: No empty build-logic module is configured)
- [ ] 3.3 Run `./gradlew :androidApp:assembleDebug` and confirm the debug APK assembles (spec: Build the Android application)
- [ ] 3.4 Run `./gradlew :composeApp:compileKotlinIosArm64` and confirm it compiles (design Verification Evidence — iOS device)
- [ ] 3.5 Run `./gradlew :composeApp:compileKotlinIosSimulatorArm64` and confirm it compiles (design Verification Evidence — iOS simulator)
- [ ] 3.6 Re-run the grep from 1.2 and confirm no `buildSrc` reference remains outside archived OpenSpec history and this change's artifacts (design Migration Plan step 5)

## 4. Finalize specifications

- [ ] 4.1 Run `openspec validate "remove-unused-buildsrc-module" --strict` and resolve any reported issue (AGENTS.md completion gate)
- [ ] 4.2 Sync the `android-kmp-build` delta into `openspec/specs/android-kmp-build/spec.md` so the module-set boundary lands in the main spec (proposal: Modified Capabilities)
- [ ] 4.3 Record verification evidence from section 3 in `verification.md` and confirm every task above is checked with passing evidence (AGENTS.md completion gate)
