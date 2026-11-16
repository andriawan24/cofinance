# Cofinance - AGENTS.md

## Research notes

`docs/research/` is where this project records how each area works, why it was built that way, and what is still open. Start at `docs/research/README.md`.

- **Before changing an area, read its note.** Each one ends with an "Open" section listing known defects and unverified behaviour.
- **When a question needs investigating**, run `/mattpocock-skills:research` with the question. It investigates against primary sources — this repository's code first, then official documentation — and writes one Markdown note to `docs/research/`, citing the source of every claim.
- **When a change alters behaviour a note describes, update the note in the same change.** Remove an "Open" item once it is resolved.
- **Say how a claim was established.** "Read in code" and "observed on a device" are different strengths of evidence. Do not record something as verified because it compiles.

There is no separate proposal or task-list workflow. OpenSpec was removed on 2026-11-16; `docs/research/history.md` summarizes what was decided under it.

## Project Overview

Cofinance is a Kotlin Multiplatform personal finance app for Android and iOS, with a Compose Multiplatform UI. It works with no account on durable on-device data; signing in with Google turns on end-to-end encrypted cloud synchronization.

## Build & Run

```bash
# Build the project
./gradlew build

# Install the Android app on a connected device
./gradlew :androidApp:installDebug

# Build the Android release APK
./gradlew :androidApp:assembleRelease

# Android lint, as run in CI
./gradlew :androidApp:lint

# Build the iOS framework (or open iosApp/iosApp.xcodeproj in Xcode and run)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

`:composeApp` is a library. The Android application tasks are on `:androidApp`.

The repository currently has no tests. The test tasks CI runs (`:composeApp:testAndroidHostTest`, `:androidApp:testDebugUnitTest`) execute nothing.

### Build Configuration

A `local.properties` file is required with all six keys; the build fails without them. Each can instead be supplied through the environment variable shown.

| `local.properties` key | Environment variable | Purpose |
|------------------------|----------------------|---------|
| `google_auth_client_id` | `GOOGLE_AUTH_CLIENT_ID` | Web OAuth client ID for Google sign-in |
| `storage.base_url` | `STORAGE_BASE_URL` | S3-compatible storage for avatars |
| `storage.access_key` | `STORAGE_ACCESS_KEY` | |
| `storage.access_secret` | `STORAGE_ACCESS_SECRET` | |
| `storage.bucket_name` | `STORAGE_BUCKET_NAME` | |
| `storage.region` | `STORAGE_REGION` | |

Firebase uses the standard platform configuration files: `androidApp/google-services.json` and `iosApp/iosApp/GoogleService-Info.plist`. Download each file for the matching registered application in Firebase Console; do not copy configuration values into BuildKonfig.

These values must never be committed, included in screenshots, or printed to CI logs. `local.properties` and both Firebase files are already in `.gitignore` — do not remove them. To share keys across machines or CI, use environment variables or a secret manager rather than checking a file in.

## Architecture

**MVVM + Clean Architecture** with Koin dependency injection.

```text
Presentation (pages/, components/)  →  Domain (usecases/, model/)  →  Data (repository/, local/, remote/)
```

- **Source of truth**: the Room database on the device. Repositories write locally first, then mirror to Firebase for signed-in users.
- **ViewModels**: `StateFlow<UiState>` plus sealed `UiEvent` types for unidirectional data flow
- **Navigation**: type-safe routes via Kotlin serialization (`navigations/destinations/Destinations.kt`)
- **Async results**: wrapped in `ResultState<T>` (Loading, Success, Error)
- **DI modules**: `networkModule`, `databaseModule`, `repositoryModule`, `securityModule`, `useCaseModule`, `viewModelModule` — all registered in `pages/App.kt`

See `docs/research/architecture.md` for the screen list, the full `expect`/`actual` inventory, and where existing code departs from the conventions below.

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| ViewModel | `[Feature]ViewModel` | `ActivityViewModel` |
| Screen | `[Feature]Screen` | `LoginScreen` |
| UiState | `[Feature]UiState` | `ActivityUiState` |
| UiEvent | `[Feature]UiEvent` (sealed) | `ActivityUiEvent` |
| Repository | `[Entity]Repository` / `[Entity]RepositoryImpl` | `AccountRepository` |
| UseCase | `[Action][Entity]UseCase` | `DeleteTransactionUseCase` |
| Component | Descriptive name | `BalanceCard`, `TransactionItem` |

## Module Structure

- **core/** — KMP library with no Compose and no Firebase: encryption, app lock, and the OCR engines
- **composeApp/** — KMP library with the UI, data and domain layers, DI, and the receipt parser; builds the iOS framework
- **androidApp/** — Android application wrapper
- **iosApp/** — Xcode project wrapper; Firebase and Google Sign-In come from Swift Package Manager

`core` and `composeApp` share the `id.andriawan.cofinance.data.*` package namespace.

### Source Sets

Each KMP module has `commonMain`, `androidMain`, and `iosMain`.

`core/src/commonMain/kotlin/id/andriawan/cofinance/data/`:

- `crypto/` — record encryption, key wrapping, recovery phrase, device key vault
- `keyring/` — the in-memory encryption session
- `lock/` — PIN, biometric, failed-attempt guard, auto-lock
- `model/` — the account and transaction payloads that get encrypted
- `ocr/` — the OCR engine interface and result model

`composeApp/src/commonMain/kotlin/id/andriawan/cofinance/`:

- `pages/` — screen composables and ViewModels (one package per feature)
- `components/` — reusable UI components
- `navigations/` — navigation graph and route definitions
- `domain/usecases/`, `domain/model/` — business logic and domain models
- `data/repository/` — repository interfaces and implementations
- `data/local/` — Room database, DAOs, local data sources
- `data/remote/` — Firestore document store and encrypted remote data sources
- `data/sync/` — the coordinator that mirrors Room to Firestore
- `data/session/` — the sign-in session policy
- `data/datasource/` — Firebase auth and profile, avatar storage, the receipt scanner
- `data/ocr/parser/` — the receipt parser
- `auth/` — Google sign-in
- `di/` — Koin modules
- `theme/` — Material Design 3 theming
- `localization/` — locale detection
- `utils/` — extensions, helpers, enums

Strings are in `composeApp/src/commonMain/composeResources/values/` (English) and `values-id/` (Indonesian).

## Key Libraries

Versions are declared once, in `gradle/libs.versions.toml`. Update that file, then this list.

- **Kotlin** 2.4.21 — Multiplatform language and compiler
- **Android Gradle Plugin** 9.5.1, **Gradle** 9.9.1 — Build tooling
- **Compose Multiplatform** 1.12.2 — UI framework
- **Material 3** 1.9.0 — Compose design system components
- **Navigation Compose** 2.10.0 — Type-safe navigation
- **Firebase Kotlin SDK** 2.8.0 — Authentication and Cloud Firestore
- **Room** 2.8.5 — Local database, with bundled SQLite 2.7.1
- **Koin** 4.2.2 — Dependency injection
- **cryptography-kotlin** 0.6.0 — AES-GCM, ECDH, HKDF, PBKDF2
- **AndroidX Biometric** 1.1.0 — Biometric prompt on Android
- **ML Kit Text Recognition** 16.0.1 — On-device OCR for receipt scanning (Android; iOS uses Vision)
- **CameraK** 1.2 — Multiplatform camera
- **Coil** 3.6.3 — Image loading
- **Ktor** 3.6.1 — HTTP engines used by Coil
- **AWS SDK for Kotlin (S3)** 1.9.37 — Avatar upload on Android
- **Kotlinx Datetime** 0.8.0 — Date/time handling
- **BuildKonfig** 0.24.0 — Multiplatform build-time configuration

## Code Style

- Kotlin official code style (`kotlin.code.style=official`)
- No ktlint or detekt. Android Lint runs in CI.
- Pull request titles must follow conventional commits (`feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `perf`, `style`, `build`, `ci`), with a lowercase subject.
- Package: `id.andriawan.cofinance`
- Android: minSdk 24, targetSdk 37, compileSdk 37, JVM 17
- iOS: deployment target 18.2
