# Cofinance

A cross-platform personal finance management application built with **Kotlin Multiplatform** and **Compose Multiplatform**. Track your finances, manage accounts, view statistics, and monitor financial activities across Android and iOS.

## Features

- **Multi-Platform Support** - Single codebase for Android and iOS
- **Authentication** - Login screen with Google Sign-in integration
- **Multi-Language Support** - English and Indonesian localization
- **Material Design 3** - Modern UI with Light and Dark theme support
- **Type-Safe Navigation** - Animated screen transitions with Kotlin serialization
- **Transaction Management** - Add and track financial transactions
- **Account Management** - Manage multiple financial accounts
- **Statistics Dashboard** - View financial analytics and insights
- **Camera Integration** - Capture receipts and documents

## Tech Stack

| Technology | Version |
|------------|---------|
| Kotlin | 2.3.0 |
| Compose Multiplatform | 1.9.3 |
| Navigation Compose | 2.9.1 |
| Android Gradle Plugin | 8.13.2 |
| AndroidX Lifecycle | 2.9.6 |
| Firebase Kotlin SDK | 2.4.0 |

### Target Platforms

- **Android**: minSdk 24, targetSdk 36
- **iOS**: arm64 & Simulator architectures

## Project Structure

```
Cofinance_Shared/
├── composeApp/                     # Main multiplatform module
│   └── src/
│       ├── commonMain/             # Shared code for all platforms
│       │   └── kotlin/id/andriawan/cofinance/
│       │       ├── pages/          # Screen composables
│       │       ├── navigations/    # Navigation setup & routes
│       │       ├── theme/          # UI theming (colors, typography, shapes)
│       │       ├── components/     # Reusable UI components
│       │       ├── localization/   # Multi-language support
│       │       └── utils/          # Utilities & extensions
│       ├── androidMain/            # Android-specific code
│       └── iosMain/                # iOS-specific code
│
├── iosApp/                         # iOS native app wrapper
│   └── iosApp/
│       ├── iOSApp.swift            # iOS entry point
│       └── ContentView.swift       # SwiftUI wrapper for Compose
│
├── gradle/
│   └── libs.versions.toml          # Dependency version catalog
└── build.gradle.kts                # Root build configuration
```

## Prerequisites

- **JDK 11** or higher
- **Android Studio** (latest stable) or **IntelliJ IDEA**
- **Android SDK** (API 24+)
- **Xcode** (for iOS development, macOS only)
- **Xcode Swift Package Manager** (Firebase and Google Sign-In dependencies)

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/your-username/cofinance.git
cd cofinance
```

### Android

```bash
# Debug build
./gradlew :composeApp:assembleDebug

# Release build
./gradlew :composeApp:assembleRelease

# Install on connected device
./gradlew :composeApp:installDebug
```

Or open the project in Android Studio and run the `composeApp` configuration.

### iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select your target device/simulator
3. Build and run (Cmd + R)

Or build the framework via Gradle:

```bash
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

## Configuration

### Firebase

Create a Firebase project, enable Google sign-in in Firebase Authentication, create a Cloud Firestore database, and enable Firebase Storage. Register both application targets, then install their Firebase configuration files:

- Android: download `google-services.json` and place it at `androidApp/google-services.json`. The Android application module processes it with the Google Services Gradle plugin.
- iOS: download `GoogleService-Info.plist` and place it at `iosApp/iosApp/GoogleService-Info.plist`. Ensure the file belongs to the `iosApp` target; the filesystem-synchronized Xcode group includes files in this directory automatically. The Swift application entry point calls `FirebaseApp.configure()`.

Keep only the non-Firebase API inputs in the untracked `local.properties` file:

```properties
google_auth_client_id=your-web-oauth-client-id.apps.googleusercontent.com
gemini.api_key=your-gemini-api-key
```

Equivalent uppercase environment variables are supported for those two values: `GOOGLE_AUTH_CLIENT_ID` and `GEMINI_API_KEY`. Never commit API credentials.

Register `id.andriawan.cofinance` for Android and the bundle identifier configured in `iosApp/Configuration/Config.xcconfig` for iOS. The iOS Xcode project links Firebase Core, Auth, Firestore, and Storage through Swift Package Manager.

Firestore uses this user-scoped structure:

```text
users/{uid}                         # Cofinance profile metadata
users/{uid}/accounts/{accountId}    # Finance accounts
users/{uid}/transactions/{id}       # Transactions
avatars/{uid}/avatar.jpg            # Firebase Storage avatar
```

Configure Firestore and Storage security rules so authenticated users can access only paths whose `{uid}` equals `request.auth.uid`. The app talks directly to Firestore and does not provide a PowerSync-style app-managed local synchronization layer.

### Gradle Properties

Key build optimizations in `gradle.properties`:

```properties
kotlin.code.style=official
org.gradle.configuration-cache=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx4096M
```

### Version Catalog

Dependencies are managed in `gradle/libs.versions.toml` for centralized version control.

## Architecture

The project follows **MVVM** (Model-View-ViewModel) architecture pattern:

- **Presentation Layer**: Composable screens with ViewModels
- **Navigation**: Type-safe Kotlin serialization-based routes
- **State Management**: StateFlow with lifecycle awareness
- **Theme System**: Material Design 3 with Composable theming

### Navigation Routes

| Route | Description |
|-------|-------------|
| Splash | App launch screen |
| Login | User authentication |
| Main | Home dashboard |
| Activity | Transaction history |
| Stats | Financial statistics |
| Account | Account management |
| Profile | User profile |
| AddNew | Add new transaction |
| AddAccount | Create new account |

## Localization

The app supports multiple languages:

- English (default)
- Indonesian (Bahasa Indonesia)

String resources are located in:
- `composeApp/src/commonMain/composeResources/values/` (English)
- `composeApp/src/commonMain/composeResources/values-id/` (Indonesian)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Resources

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/)
- [Material Design 3](https://m3.material.io/)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
