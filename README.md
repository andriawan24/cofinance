# Cofinance

A personal finance app for Android and iOS, built with **Kotlin Multiplatform** and **Compose Multiplatform**. Track accounts and transactions on your device, and optionally sync them to the cloud with end-to-end encryption.

## Features

- **Works without an account** — accounts and transactions are stored on the device and usable offline
- **Optional encrypted sync** — sign in with Google to mirror your data to Firebase, encrypted on the device so the operator cannot read it
- **Recovery phrase** — restore your data on a new device with a phrase only you hold
- **App lock** — six-digit PIN, optional biometric unlock, and auto-lock
- **Receipt scanning** — on-device OCR turns a receipt photo into a draft transaction; the image never leaves the device
- **Accounts and transactions** — expenses, income, and transfers across multiple accounts
- **Cycles and statistics** — spending by category per cycle, with a configurable cycle start day
- **English and Indonesian** — follows the system language
- **Material Design 3** — light and dark themes

## Tech Stack

| Technology | Version |
|------------|---------|
| Kotlin | 2.4.21 |
| Compose Multiplatform | 1.12.2 |
| Android Gradle Plugin | 9.5.1 |
| Gradle | 9.9.1 |
| Navigation Compose | 2.10.0 |
| Firebase Kotlin SDK | 2.8.0 |
| Room | 2.8.5 |
| Koin | 4.2.2 |
| cryptography-kotlin | 0.6.0 |
| ML Kit Text Recognition | 16.0.1 |

The full list is in `gradle/libs.versions.toml`.

### Target Platforms

- **Android**: minSdk 24, targetSdk 37
- **iOS**: 18.2 and later, arm64 device and Apple-silicon simulator

## Project Structure

```
cofinance/
├── core/                 # KMP library: encryption, app lock, OCR engines (no Compose, no Firebase)
├── composeApp/           # KMP library: UI, data, domain, DI; builds the iOS framework
│   └── src/
│       ├── commonMain/   # Shared code
│       ├── androidMain/  # Android implementations
│       └── iosMain/      # iOS implementations
├── androidApp/           # Android application
├── iosApp/               # Xcode project
├── docs/research/        # How each area works, why, and what is open
└── gradle/
    └── libs.versions.toml
```

## Prerequisites

- **JDK 17**
- **Android Studio** (latest stable)
- **Xcode** (for iOS, macOS only); Firebase and Google Sign-In are resolved through Swift Package Manager

## Getting Started

```bash
git clone https://github.com/andriawan24/cofinance.git
cd cofinance
```

Complete [Configuration](#configuration) before building. The build fails without it.

### Android

```bash
# Install the debug build on a connected device
./gradlew :androidApp:installDebug

# Build the release APK
./gradlew :androidApp:assembleRelease
```

Or open the project in Android Studio and run the `androidApp` configuration.

### iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select a device or simulator
3. Build and run

## Configuration

### Firebase

Create a Firebase project, enable Google sign-in in Firebase Authentication, and create a Cloud Firestore database. Register both applications as `id.andriawan.cofinance`, then install their configuration files:

- Android: `google-services.json` at `androidApp/google-services.json`
- iOS: `GoogleService-Info.plist` at `iosApp/iosApp/GoogleService-Info.plist`

Both files are git-ignored.

Firestore data is scoped to the signed-in user:

```text
users/{uid}                         # Profile metadata
users/{uid}/key_material/current    # Wrapped copies of the encryption key
users/{uid}/accounts/{accountId}    # Encrypted accounts
users/{uid}/transactions/{id}       # Encrypted transactions
```

Configure Firestore security rules so an authenticated user can reach only paths whose `{uid}` equals `request.auth.uid`.

### Build values

Create an untracked `local.properties` with all six values:

```properties
google_auth_client_id=your-web-oauth-client-id.apps.googleusercontent.com
storage.base_url=https://your-storage-host
storage.access_key=your-access-key
storage.access_secret=your-access-secret
storage.bucket_name=your-bucket
storage.region=your-region
```

`google_auth_client_id` is the web OAuth client ID for Google sign-in. The `storage.*` values point to the S3-compatible store that holds profile avatars.

Each value can instead be supplied through an environment variable: `GOOGLE_AUTH_CLIENT_ID`, `STORAGE_BASE_URL`, `STORAGE_ACCESS_KEY`, `STORAGE_ACCESS_SECRET`, `STORAGE_BUCKET_NAME`, `STORAGE_REGION`. Never commit credentials.

## Architecture

MVVM over a clean-architecture split, with Koin for dependency injection.

- **Presentation**: composable screens with ViewModels exposing `StateFlow`
- **Domain**: use cases and domain models
- **Data**: Room is the source of truth; Firebase is an optional encrypted mirror

Deeper documentation lives in [`docs/research/`](docs/research/README.md):

| Note | Covers |
|------|--------|
| [Build and modules](docs/research/build-and-modules.md) | Modules, targets, dependencies, build requirements |
| [CI and distribution](docs/research/ci-and-distribution.md) | GitHub workflows and tester distribution |
| [Architecture](docs/research/architecture.md) | Package layout, screens, conventions |
| [Authentication](docs/research/authentication.md) | Google sign-in and signed-out use |
| [Local data and sync](docs/research/local-data-and-sync.md) | Room, Firestore, and synchronization |
| [Client-side encryption](docs/research/client-side-encryption.md) | Key hierarchy and recovery phrase |
| [App lock](docs/research/app-lock.md) | PIN, biometric, auto-lock |
| [Receipt scanning](docs/research/receipt-scanning.md) | OCR and receipt parsing |

## Localization

- English (default): `composeApp/src/commonMain/composeResources/values/`
- Indonesian: `composeApp/src/commonMain/composeResources/values-id/`

## Contributing

1. Create a feature branch
2. Commit your changes
3. Open a pull request with a conventional-commit title, for example `feat(transactions): add recurring transfers`

If your change alters behaviour described in `docs/research/`, update the note in the same pull request.

## Resources

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/)
- [Material Design 3](https://m3.material.io/)
