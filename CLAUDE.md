# Cofinance - CLAUDE.md

## Project Overview

Cofinance is a Kotlin Multiplatform (KMP) personal finance app targeting Android, iOS, Desktop (JVM), and Web (JS/WASM). It uses Compose Multiplatform for the UI layer.

## Build & Run

```bash
# Build the project
./gradlew build

# Run Android app
./gradlew :androidApp:installDebug

# Run Desktop app
./gradlew :composeApp:run

# Run tests
./gradlew :composeApp:allTests
```

### Build Configuration

A `local.properties` file is required with:
- `supabase.project_url`
- `supabase.public_api_key`
- `gemini.api_key`
- `google_auth_client_id`

These are injected at build time via BuildKonfig.

## Architecture

**MVVM + Clean Architecture** with Koin dependency injection.

```
Presentation (pages/, components/)  →  Domain (usecases/, model/)  →  Data (repository/, datasource/)
```

- **ViewModels**: Use `StateFlow<UiState>` + sealed `UiEvent` classes for unidirectional data flow
- **Navigation**: Type-safe routes via Kotlin serialization (`Destinations.kt`)
- **Async results**: Wrapped in `ResultState<T>` (Loading, Success, Error)
- **DI modules**: `networkModule`, `repositoryModule`, `useCaseModule`, `viewModelModule` — all registered in `App.kt`

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| ViewModel | `[Feature]ViewModel` | `ActivityViewModel` |
| Screen | `[Feature]Screen` | `LoginScreen` |
| UiState | `[Feature]UiState` | `ActivityUiState` |
| UiEvent | `[Feature]UiEvent` (sealed class) | `ActivityUiEvent` |
| Repository | `[Entity]Repository` / `[Entity]RepositoryImpl` | `AuthenticationRepository` |
| UseCase | `[Action][Entity]UseCase` | `FetchUserUseCase` |
| Component | Descriptive name | `BalanceCard`, `TransactionItem` |

## Module Structure

- **composeApp/** — Main KMP module with all shared and platform-specific code
- **androidApp/** — Thin Android wrapper
- **iosApp/** — iOS Xcode project wrapper

### Source Sets

All shared code lives under `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/`:

- `pages/` — Screen composables (one package per feature)
- `components/` — Reusable UI components
- `navigations/` — Route definitions
- `domain/usecases/` — Business logic
- `data/repository/` — Repository implementations
- `data/datasource/` — Supabase and Gemini data sources
- `di/` — Koin modules
- `theme/` — Material Design 3 theming
- `localization/` — English and Indonesian strings
- `utils/` — Extensions, helpers, enums

Platform-specific code uses `expect`/`actual` for: permissions, Google auth, camera, gallery picker, locale management.

## Key Libraries

- **Compose Multiplatform** 1.10.0 — UI framework
- **Supabase Kotlin** 3.3.0 — Auth, PostgREST, Realtime, Storage
- **Ktor** 3.4.0 — HTTP client
- **Koin** 4.1.1 — Dependency injection
- **Coil** 3.3.0 — Image loading
- **Google Generative AI** 0.9.0 — Gemini for receipt scanning
- **CameraK** 0.2.0 — Multiplatform camera
- **Kotlin Datetime** 0.7.1 — Date/time handling

## Code Style

- Kotlin official code style (`kotlin.code.style=official`)
- No automated linting tools configured (no ktlint/detekt)
- Package: `id.andriawan.cofinance`
- Android: minSdk 24, targetSdk 36, JVM 17
