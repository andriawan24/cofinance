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

Run warning-enabled configuration and compilation for Android, Desktop, both iOS architectures, JS, and WasmJS. Then run shared tests, Android debug assembly, and focused smoke tests for Google sign-in, remote image loading, Supabase requests, PowerSync startup, and receipt scanning. Treat new warnings as audit inputs rather than suppressing them by default.
