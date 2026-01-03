# Cofinance

A cross-platform personal finance management application built with **Kotlin Multiplatform** and **Compose Multiplatform**. Track your finances, manage accounts, view statistics, and monitor financial activities across Android, iOS, and Web platforms.

## Features

- **Multi-Platform Support** - Single codebase for Android, iOS, and Web (JS/Wasm)
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

### Target Platforms

- **Android**: minSdk 24, targetSdk 36
- **iOS**: arm64 & Simulator architectures
- **Web**: JavaScript and WebAssembly targets

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
│       ├── iosMain/                # iOS-specific code
│       ├── jsMain/                 # Web JS target
│       └── wasmJsMain/             # Web Wasm target
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
- **CocoaPods** (for iOS dependencies)

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

### Web (Wasm)

```bash
# Development server with hot reload
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Production build
./gradlew :composeApp:wasmJsBrowserProductionWebpack
```

### Web (JavaScript)

```bash
# Development server
./gradlew :composeApp:jsBrowserDevelopmentRun

# Production build
./gradlew :composeApp:jsBrowserProductionWebpack
```

## Configuration

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
- [Kotlin/Wasm](https://kotl.in/wasm/)
- [Material Design 3](https://m3.material.io/)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.