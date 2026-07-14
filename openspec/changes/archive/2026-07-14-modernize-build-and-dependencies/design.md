## Context

The working tree upgrades the core stack to the mid-2026 generation. `:composeApp:compileKotlinDesktop` and `:androidApp:assembleDebug` succeed. `:composeApp:compileKotlinIosSimulatorArm64` fails because the compiler cannot find ten iOS `actual` declarations; configuration reports that the default Kotlin hierarchy template was not applied and that `iosMain` is unused. The same build reports Gradle 10 container/dependency deprecations and five AGP flags scheduled for removal in AGP 10.

The current dependency topology is:

```text
commonMain
├── Compose, lifecycle, navigation, datetime
├── Supabase BOM + Auth/PostgREST/Storage
├── Koin BOM + Core/Compose/ViewModel
├── Ktor core + CIO
├── Coil Compose
└── Gemini wrapper

nonWebMain (Android + iOS + Desktop)
└── PowerSync

Android / iOS / Desktop
├── OkHttp / Darwin / CIO inherited from commonMain
└── CameraK
```

## Goals / Non-Goals

**Goals:** make every supported target compile the intended sources, remove next-major build deprecations, scope engines correctly, keep dependency families aligned, and establish repeatable verification.

**Non-goals:** product feature changes, switching away from Supabase or PowerSync, redesigning authentication, or adopting prerelease dependency versions only to maximize version numbers.

## Decisions

### 1. Fix hierarchy before further version changes

The `iosMain` warning is correctness-critical. Define the native/web grouping through a hierarchy template or explicitly connect `iosArm64Main` and `iosSimulatorArm64Main` to `iosMain`, then verify the graph. Do not merely suppress the default hierarchy diagnostic.

### 2. Keep the refreshed versions that already form a compatible release train

Gradle 9.6.1, AGP 9.2.1, Kotlin 2.4.0, Compose 1.11.1, Ktor 3.5.1, Coil 3.5.0, and kotlinx-datetime 0.8.0 are mutually contemporary. Supabase 3.6.0, PowerSync 1.13.0, CameraK 1.1, BuildKonfig 0.22.0, and Koin 4.2.2 resolve in the successful build. Upgrade work should focus on integration correctness rather than additional speculative bumps.

### 3. Select one Ktor engine per platform

Keep `ktor-client-core` in `commonMain`; move CIO to Desktop and retain OkHttp on Android and Darwin on iOS. This avoids multiple engine implementations being inherited by native targets.

### 4. Add explicit Coil network support

`coil-compose` supplies Compose integration but does not itself establish network fetching. Add the Coil Ktor 3 network artifact and use the already selected per-platform Ktor engine for remote profile and preview images.

### 5. Reduce version and API surface drift

Use a single Compose release version for aligned Compose modules where publisher versioning permits it. Keep independently versioned Material3, lifecycle, and navigation coordinates explicit. Change PowerSync and logging from `api` to `implementation` unless a public shared-module signature demonstrably exposes those types.

### 6. Make configuration lazy and public-API based

Remove the import of AGP's internal `gradleLocalProperties`. Resolve required secrets through Gradle providers, supporting ignored `local.properties` for local builds and environment variables for CI without logging values.

### 7. Retain the Gemini wrapper pending a dedicated replacement change

Keep `dev.shreyaspatil.generativeai:generativeai-google` at `0.9.0-1.1.0` in this mechanical modernization. Receipt scanning currently depends on its request and response model, while no drop-in supported KMP replacement has been validated across Android, iOS, Desktop, JS, and WasmJS. Replacing it would exceed this change's non-goal of avoiding product behavior changes.

Create a separate OpenSpec change before replacement when at least one of these triggers occurs: the wrapper no longer resolves with the supported Kotlin/Compose toolchain, a security or service deprecation requires migration, or a candidate Google/Firebase GenAI client is verified for every target that supports receipt scanning. That change must cover API mapping, error behavior, platform availability, and receipt-scanning regression tests.

### 8. Disable optional Okio Node modules in browser bundles

Add a Kotlin-supported `webpack.config.d` fragment that resolves Okio's optional `os` and `path` imports to `false` for browser bundles. Okio guards these imports behind its Node environment path, so the browser does not require Node filesystem modules; Webpack 5 nevertheless resolves the static imports while bundling. Keep the fallback limited to browser packaging instead of adding Node polyfills and unnecessary runtime dependencies.

### 9. Accept browser compilation and bundle verification without Chrome execution

For this change, verify JS and WasmJS through target compilation, test compilation, and development Webpack bundling. Chrome browser execution is explicitly omitted because no compatible Chrome runtime is installed, the official Chrome-for-Testing artifact host is unreachable from this environment, and the installed Arc browser cannot be captured by Karma in headless or normal Chromium modes. This omission does not count as evidence for interactive web behavior; the affected runtime smoke checks remain open under task 4.4.

## Audit Findings and Priority

| Priority | Finding | Evidence | Intended update |
|---|---|---|---|
| P0 | iOS compilation fails because `iosMain` is unused | Ten missing `actual` declarations in `compileKotlinIosSimulatorArm64` | Repair source-set hierarchy and compile both iOS targets |
| P1 | Custom hierarchy disables the default template | Kotlin hierarchy diagnostic lists all explicit `dependsOn` edges | Adopt a hierarchy template or fully explicit supported graph |
| P1 | Five Android flags expire in AGP 10 | Warning-enabled Android build | Remove flags and test current defaults |
| P1 | Gradle container delegates expire in Gradle 10 | `by getting` / `by creating` warnings | Use `getByName` / `create` or typed configuration APIs |
| P1 | Legacy project dependency notation expires in Gradle 10 | Android configuration warning | Use the supported typed project dependency form |
| P1 | CIO is inherited by Android and iOS | `commonMain` plus platform engines | Move CIO to Desktop |
| P1 | Remote Coil images lack an explicit network artifact | Only `coil-compose` is declared | Add Coil Ktor 3 networking |
| P2 | Internal AGP API loads secrets eagerly | `com.android.build.gradle.internal...gradleLocalProperties` | Use Gradle providers and environment fallback |
| P2 | Compose versions are repeated | Six catalog version keys point at 1.11.1 | Consolidate aligned coordinates |
| P2 | `api` exposes PowerSync and logging | `nonWebMain` and `commonMain` dependency declarations | Narrow to `implementation` where possible |
| P2 | Android host tests are not enabled | AGP KMP warning for existing `commonTest` | Add host-test wiring or document target exclusions |
| P3 | `buildSrc` has no explicit root name | Type-safe accessor cache warning | Set a stable `buildSrc` root project name |
| Investigate | Gemini wrapper is a legacy integration point | Single `generativeai-google` wrapper coordinate | Evaluate supported Google/Firebase GenAI KMP options before replacement |

## Risks / Trade-offs

- Hierarchy changes can alter dependency visibility across native compilations; compile every target immediately after the change.
- Removing AGP flags can change generated resources or R-class behavior; verify Android resources, locale config, shrinking, and release packaging.
- Engine relocation may expose hidden reliance on CIO behavior; exercise Supabase, PowerSync, and receipt scanning on each native platform.
- A Gemini client replacement may affect request/response models and is intentionally separated from the mechanical build cleanup.

## Verification Strategy

Run warning-enabled configuration and compilation for Android, Desktop, both iOS architectures, JS, and WasmJS. Run shared tests on the available host, Desktop, and iOS simulator targets; compile JS and WasmJS tests and verify both development browser bundles without requiring Chrome execution for this change. Then run Android debug assembly and focused smoke tests for Google sign-in, remote image loading, Supabase requests, PowerSync startup, and receipt scanning. Treat new warnings as audit inputs rather than suppressing them by default.

## Implementation Verification Findings

- `./gradlew help --warning-mode=all` succeeds. The remaining Gradle 10 project-notation diagnostic is attributed by Gradle's problems report to AGP 9.2.1 plugin IDs (`com.android.internal.application` and `com.android.internal.kotlin.multiplatform.library`), not a project-authored build API. The Android app now declares the shared module through `DependencyHandler.project(String)`.
- Warning-enabled Android debug assembly and Desktop compilation succeed. Their remaining Kotlin warning is the existing Beta status of `expect`/`actual` classes.
- iOS device and simulator compilation succeed and both task graphs consume `iosMain` and `nonWebMain`. Existing iOS warnings remain for Beta `expect`/`actual` classes, `BetaInteropApi`, and a redundant Elvis expression in `Helper.ios.kt`.
- JS and WasmJS compilation succeed. Dependency insight confirms OkHttp without CIO on Android, Darwin without CIO on iOS, and CIO without OkHttp or Darwin on Desktop.
- Android host, Desktop, and iOS simulator tests pass. The Coil addition's JS and Wasm Yarn locks were refreshed, resolving the initially stale `@js-joda/core` installation. JS and Wasm development Webpack bundles now succeed; the JS report confirms Okio's optional Node `os` and `path` modules are ignored by the project fallback.
- The aggregate `allTests` task cannot complete because browser tests require Chrome. An isolated Chrome for Testing install was attempted under ignored build output: the current macOS ARM headless-shell build was unavailable, and the standard Chrome binary download timed out without receiving data from Google storage. A later bounded retry against the official Chrome-for-Testing storage URL also timed out while establishing the TLS connection, before any response or artifact was received. The installed Arc browser also failed Karma capture in both headless and normal Chromium modes. Chrome execution is therefore an explicitly accepted omission for this change, supported by successful JS/Wasm test compilation and development bundle verification rather than represented as a browser-runtime pass.
- A bounded Desktop launch succeeds, loads the PowerSync native library, and starts all 38 Koin definitions. This is startup evidence only. Google sign-in, remote image fetching, authenticated Supabase requests, PowerSync synchronization, and receipt scanning still require interactive platform runtimes and configured service state; they remain manual validation gates rather than being inferred from startup or compilation.
- All four required local configuration inputs are present without exposing their values. No Android device is connected. iOS simulators are installed but the required XcodeBuildMCP integration is not connected, so the repository's simulator-testing workflow cannot perform install, launch, screenshots, logs, or authenticated UI interaction in this session.
