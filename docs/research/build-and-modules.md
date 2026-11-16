# Build and modules

**Question:** Which modules, targets, and dependencies make up the build, and what does a build need?

**Last verified:** 2026-11-16 against `ac66286`. Established by reading the build files; nothing here was re-run.

## Modules

Gradle includes three modules: `:core`, `:composeApp`, and `:androidApp` (`settings.gradle.kts:35-37`). `iosApp/` is an Xcode project, not a Gradle module.

| Module | What it is | Plugins | Source |
|--------|-----------|---------|--------|
| `:core` | KMP library with no Compose and no Firebase. Holds the encryption, app-lock, and OCR-engine code. Packages no framework of its own. | `kmp`, `androidKmpLibrary`, `kotlinSerialization` | `core/build.gradle.kts:3-29` |
| `:composeApp` | KMP library holding UI, data, domain, DI, and the receipt parser. Builds the static `ComposeApp` iOS framework. Depends on `api(projects.core)`. | `kmp`, `cmp`, `composeCompiler`, `androidKmpLibrary`, `kotlinSerialization`, `androidxRoom`, `ksp`, `buildkonfig` | `composeApp/build.gradle.kts:5-43, 79` |
| `:androidApp` | Android application: `MainActivity`, manifest, signing, Google Services processing. | `androidApplication`, `composeCompiler`, `googleServices` | `androidApp/build.gradle.kts:1-5` |

`:composeApp` is a library, so `:composeApp:assembleDebug` and `:composeApp:installDebug` do not produce or install an APK. The application tasks live on `:androidApp`.

### Why `:core` exists

`:core` was extracted so that security and OCR code could be linked into an iOS test binary. Firebase reaches the iOS app through the Xcode project's Swift packages rather than through Gradle, so a Gradle-linked test binary of `:composeApp` failed with `ld: framework 'FirebaseCore' not found`. Moving the Firebase-free code into its own module removed the coupling instead of teaching the test link where Firebase's frameworks live. Package names were preserved, so `core` and `composeApp` share the `id.andriawan.cofinance.data.*` namespace — `data.ocr` is in `:core` while `data.ocr.parser` is in `:composeApp`.

The receipt parser stayed in `:composeApp` because its lexicon maps merchants onto `TransactionCategory`, which is bound to Compose resources.

## Targets

Both KMP modules declare `android`, `iosArm64`, and `iosSimulatorArm64`, with the default hierarchy template (`core/build.gradle.kts:10-29`, `composeApp/build.gradle.kts:17-43`). There is no `iosX64`, JVM desktop, JS, or Wasm target; desktop and web were removed in July 2026 (see [history.md](history.md)).

| Setting | Value | Source |
|---------|-------|--------|
| Application ID and namespace | `id.andriawan.cofinance` | `androidApp/build.gradle.kts:8-15` |
| `:core` namespace | `id.andriawan.cofinance.core` | `core/build.gradle.kts:11` |
| `:composeApp` namespace | `id.andriawan.cofinance.shared` | `composeApp/build.gradle.kts:18` |
| BuildKonfig package | `com.andriawan.cofinance` (note `com`, not `id`) | `composeApp/build.gradle.kts:164` |
| `compileSdk` / `targetSdk` | 37 / 37 | `androidApp/build.gradle.kts:12, 17` |
| `minSdk` | 24 | `androidApp/build.gradle.kts:16` |
| JVM target | 17 | `core/build.gradle.kts:20`, `composeApp/build.gradle.kts:27`, `androidApp/build.gradle.kts:58-61` |
| Android version | code 8, name `0.0.2` | `androidApp/build.gradle.kts:18-19` |
| iOS deployment target | 18.2 | `iosApp/iosApp.xcodeproj/project.pbxproj:243, 341` |
| iOS bundle identifier | `id.andriawan.cofinance` | `iosApp/Configuration/Config.xcconfig:4` |
| iOS version | marketing 1.0, build 1 | `iosApp/Configuration/Config.xcconfig:6-7` |

The Android release build minifies and shrinks resources (`androidApp/build.gradle.kts:39-55`). `androidApp/proguard-rules.pro` is a placeholder with no rules.

## Dependencies

All versions are declared in `gradle/libs.versions.toml`. Koin and Firebase Android modules take their versions from a BOM.

| Library | Version | Used for |
|---------|---------|----------|
| Gradle wrapper | 9.9.1 | `gradle/wrapper/gradle-wrapper.properties:3` |
| Android Gradle Plugin | 9.5.1 | Android and Android-KMP build tooling |
| Kotlin | 2.4.21 | Language, compiler, serialization and Compose compiler plugins |
| KSP | 2.3.10 | Room compiler |
| Compose Multiplatform | 1.12.2 | UI framework |
| Material 3 | 1.9.0 | Design system components |
| Navigation Compose | 2.10.0 | Type-safe navigation |
| AndroidX Lifecycle | 2.11.0 | ViewModel and lifecycle-aware collection |
| Koin BOM | 4.2.2 | Dependency injection |
| Firebase Kotlin SDK (GitLive) | 2.8.0 | Authentication and Cloud Firestore |
| Firebase Android BOM | 35.2.0 | Aligns the native Android Firebase modules |
| Google Services plugin | 4.6.0 | Processes `google-services.json` |
| Room / bundled SQLite | 2.8.5 / 2.7.1 | Local database |
| cryptography-kotlin | 0.6.0 | AES-GCM, ECDH, HKDF, PBKDF2 |
| AndroidX Biometric | 1.1.0 | Biometric prompt on Android |
| ML Kit Text Recognition | 16.0.1 | Bundled on-device OCR on Android (iOS uses Vision) |
| CameraK | 1.2 | Multiplatform camera |
| Coil | 3.6.3 | Image loading, with the Ktor network fetcher |
| Ktor | 3.6.1 | HTTP engines (OkHttp on Android, Darwin on iOS) for Coil |
| AWS SDK for Kotlin, S3 | 1.9.37 | Avatar upload to S3-compatible storage (Android only) |
| Credentials / Google ID | 1.6.0 / 1.2.1 | Google sign-in on Android |
| kotlinx-datetime | 0.8.0 | Date and time |
| kotlinx-serialization | 1.13.0 | JSON and navigation routes |
| kotlinx-coroutines | 1.11.0 | Concurrency |
| BuildKonfig | 0.24.0 | Build-time configuration constants |

On iOS, Firebase and Google Sign-In come from Swift Package Manager rather than Gradle: `firebase-ios-sdk` from 11.8.0 (products Auth, Core, Firestore, Storage) and `GoogleSignIn-iOS` from 9.2.0 (`iosApp/iosApp.xcodeproj/project.pbxproj:412-457`).

No source constructs a Ktor `HttpClient`. The Ktor dependencies are present for `coil-network-ktor3`.

### Dependency rules carried over from OpenSpec

These were requirements in the removed `dependency-management` spec. They still describe how the build is meant to be maintained.

- Declare every direct dependency and plugin version once, in the version catalog, unless a tool requires it in a settings or wrapper file.
- Use the publisher's BOM, or one shared catalog version, for libraries published as a family.
- Declare platform engines and native libraries only in the source set that supports them. Each target resolves exactly one Ktor engine; CIO is not inherited from common code.
- After upgrading Gradle, AGP, Kotlin, or Compose, compile Android and both iOS architectures and review new deprecation diagnostics.
- Avoid internal plugin APIs and flags scheduled for removal in the next major Gradle or AGP release.

## What a build needs

### Build-time configuration

BuildKonfig generates six string constants. Each is read from an environment variable first and from `local.properties` second, and the build fails if neither supplies it (`composeApp/build.gradle.kts:140-202`).

| Constant | `local.properties` key | Environment variable |
|----------|------------------------|----------------------|
| `GOOGLE_AUTH_API_KEY` | `google_auth_client_id` | `GOOGLE_AUTH_CLIENT_ID` |
| `STORAGE_BASE_URL` | `storage.base_url` | `STORAGE_BASE_URL` |
| `STORAGE_ACCESS_KEY` | `storage.access_key` | `STORAGE_ACCESS_KEY` |
| `STORAGE_ACCESS_SECRET` | `storage.access_secret` | `STORAGE_ACCESS_SECRET` |
| `STORAGE_BUCKET_NAME` | `storage.bucket_name` | `STORAGE_BUCKET_NAME` |
| `STORAGE_REGION` | `storage.region` | `STORAGE_REGION` |

`google_auth_client_id` is the web OAuth client ID used as the server client ID for Google sign-in on Android. The five `storage.*` values configure the S3-compatible store that holds avatars.

### Firebase configuration files

- Android: `androidApp/google-services.json`, processed by the Google Services plugin.
- iOS: `iosApp/iosApp/GoogleService-Info.plist`, read by `FirebaseApp.configure()` in `iosApp/iosApp/iOSApp.swift:11`.

Both are git-ignored, along with `local.properties` (`.gitignore:7-9`).

### Android signing

Signing reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, and `KEY_PASSWORD` from the environment. When `KEYSTORE_PATH` is empty, the release build falls back to the debug signing config (`androidApp/build.gradle.kts:9, 24-45`).

### Gradle properties

Configuration cache and build cache are on; cinterop commonization is enabled for the shared module (`gradle.properties:4-9`).

## Tests

There are no test sources anywhere in the repository. The whole suite, the `:core-testing` module that held shared fakes, and the instrumented-test wiring were removed in commits `56bcd9a` and `e90920b`, ahead of a planned integration-test stage.

What remains wired: `commonTest` depends on `kotlin-test` and Android host tests are enabled in both KMP modules (`core/build.gradle.kts:15-17, 50-52`, `composeApp/build.gradle.kts:22-24, 124-126`). CI still runs the test tasks, which currently execute nothing (see [ci-and-distribution.md](ci-and-distribution.md)).

## Open

- **The storage access key and secret are compiled into the shipped app** through BuildKonfig. Anyone with the APK can extract them. The on-device receipt scanning work removed an embedded Gemini key for exactly this reason; the storage credentials reintroduce the same exposure.
- **`keystore/cofinance.jks` is tracked in git**, and no `.gitignore` rule covers keystores.
- **The Android release build is debug-signed** whenever `KEYSTORE_PATH` is unset, which is the case in CI.
- The `androidLibrary` plugin alias is declared `apply false` at the root and applied by no module (`build.gradle.kts:9`).
- `.gitignore:31-34` ignores `core/src/androidDeviceTest/assets/` and points to a README beside it. That directory no longer exists.
- `iosApp/ExportOptions.plist` carries placeholder team and provisioning-profile values and keys the profile to `id.andriawan.cofinance.Cofinance`, which is not the app's bundle identifier.
- `Config.xcconfig` leaves `TEAM_ID` empty while the Xcode project hardcodes a development team.
