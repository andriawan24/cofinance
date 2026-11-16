# Architecture

**Question:** How is the shared code laid out, and which conventions does it actually follow?

**Last verified:** 2026-11-16 against `ac66286`. Established by reading the source. Path shorthand is defined in [README.md](README.md).

## Layering

MVVM over a clean-architecture split, wired with Koin:

```text
pages/, components/   →   domain/usecases/, domain/model/   →   data/repository/, data/local/, data/remote/
```

Room is the source of truth for finance data. Repositories write locally first and mirror to Firebase afterwards (see [local-data-and-sync.md](local-data-and-sync.md)).

## Package layout

### `:core`

Everything sits under `id.andriawan.cofinance.data`.

| Package | Contents |
|---------|----------|
| `data.crypto` | Record encryption, key wrapping, recovery phrase, device key vault, scrypt |
| `data.keyring` | `EncryptionSession`, which holds the unwrapped data key in memory |
| `data.lock` | PIN lock, biometric key box, failed-attempt guard, auto-lock, local key material storage |
| `data.model` | `AccountResponse`, `TransactionResponse` — the two payloads the record cipher seals |
| `data.ocr` | `OcrEngine`, the `OcrResult` model, and the ML Kit and Vision engines |

### `:composeApp`

Rooted at `id.andriawan.cofinance`.

| Package | Contents |
|---------|----------|
| `auth` | `GoogleAuthManager` |
| `components` | Reusable composables |
| `data.datasource` | `FirebaseDataSource`, `GarageStorageDataSource`, `ReceiptScanner` |
| `data.local` | Room database, builders, DAOs and local data sources for accounts, transactions, and merchant categories |
| `data.model` | `document/` (Firestore), `entity/` (Room), `request/`, `response/` |
| `data.ocr.parser` | Receipt parser, lexicon, currency and date parsing, merchant-category learning |
| `data.remote` | `FinanceDocumentStore`, encrypted remote data sources, `KeyMaterialGate` |
| `data.repository` | Account, transaction, and authentication repositories |
| `data.session` | `SessionPolicy` |
| `data.sync` | `FirebaseSyncCoordinator` |
| `di` | Koin modules |
| `domain.model`, `domain.usecases` | Domain models, and use cases under `accounts/`, `authentications/`, `transactions/` |
| `localization` | `AppLang` and locale detection |
| `models` | `CofinanceAppState` |
| `navigations` | `MainNavigation.kt` and `destinations/` |
| `pages` | One package per feature |
| `theme` | `CofinanceTheme`, colours, shapes, typography |
| `utils` | `ResultState`, `UiText`, dimensions, helpers, extensions |

## Screens

Routes are a sealed class of `@Serializable` destinations (`app/navigations/destinations/Destinations.kt:5-65`). The root graph starts at `Splash` (`app/navigations/MainNavigation.kt:84-310`); a nested graph inside `MainScreen` hosts the four bottom tabs (`app/pages/main/MainScreen.kt:57-137`).

| Screen | What it does |
|--------|--------------|
| Splash | Runs the launch sequence and routes to Main, encryption setup, or unlock |
| Login | Google sign-in, shown with an onboarding swiper |
| EncryptionSetup | Generates the data key and recovery phrase after sign-in |
| RecoveryPhraseRestore | Restores the data key on a device that holds none |
| Unlock | PIN or biometric unlock |
| Main | Scaffold with bottom navigation and the add button |
| Activity (tab) | Transactions for the current cycle, with totals and swipe-to-delete |
| Stats (tab) | Expense-by-category pie chart per cycle |
| Account (tab) | Accounts grouped by type, with edit and delete |
| Profile (tab) | Profile, cycle start day, security settings, sign-out; a sign-in card when signed out |
| EditProfile | Change name and avatar |
| CycleReview | Choose which account balances carry into the next cycle |
| AddNew | Add or edit a transaction; also reviews a scanned draft |
| AddAccount | Create an account |
| Camera | Capture a receipt or pick one from the gallery |
| Preview | Confirm the image and start the scan |

Security settings are a section inside Profile, not a screen. Screens return results to each other through `savedStateHandle` keys (`app/navigations/MainNavigation.kt:238, 283, 305`).

When the encryption session becomes `Locked` on any screen outside the launch sequence, navigation returns to Splash, so a timeout removes finance data from view (`app/navigations/MainNavigation.kt:69-82`).

## Dependency injection

All modules are registered in `App()` (`app/pages/App.kt:31-43`), in this order:

| Module | Provides |
|--------|----------|
| `networkModule` | Firebase Auth and Firestore, `SessionPolicy`, `EncryptionSession`, record cipher, key wrappers, `OcrEngine`, `ReceiptParser`, `ReceiptScanner`, `GoogleAuthManager` |
| `databaseModule(context)` | Room, local data sources, the Firestore document store, encrypted remote sources, `FirebaseSyncCoordinator`, `GarageStorageDataSource`, merchant-category learning |
| `repositoryModule` | The three repositories |
| `securityModule` | Local key material store, app lock, `AutoLockController`, the launch synchronizer |
| `useCaseModule` | 16 use cases |
| `viewModelModule` | 16 ViewModels |

## State and events

- State is a `MutableStateFlow<XUiState>` exposed with `asStateFlow()`.
- One-off effects go through a buffered `Channel` collected with `CollectAsEffect` (`app/utils/extensions/FlowExt.kt:10-16`).
- User actions are a sealed `XUiEvent` passed to `onEvent`.
- Async results are `ResultState<T>` — `Loading`, `Success`, `Error` — collected with `collectResult` (`app/utils/ResultState.kt:6-24`).
- User-facing text is `UiText.Raw` or `UiText.Res` (`app/utils/UiText.kt:7-16`).

## Platform boundaries

Every `expect` has an Android and an iOS `actual`.

| Area | Declarations |
|------|--------------|
| Auth | `GoogleAuthManager` |
| Media | `CameraPreviewContent`, `rememberGalleryLauncher`, `readFromFile`, `deleteFile`, `compressImage` |
| Storage | `GarageStorageDataSource`, `databaseBuilder`, `CofinanceRoomDatabaseConstructor` (generated by Room) |
| Locale and permissions | `rememberAppLocale`, `createCameraPermissionHandler`, `goToSystemSettings` |
| OCR (`:core`) | `createOcrEngine` |
| Security (`:core`) | `createDeviceKeyVault`, `createRecoveryPhraseVault`, `createRecoveryPhraseExporter`, `createFailedAttemptStore`, `createKeyMaterialStorage`, `createBiometricKeyBox`, `createAutoLockSettings` |

## Localization and theme

Strings are Compose Multiplatform resources in `composeApp/src/commonMain/composeResources/values/` (English, the default) and `values-id/` (Indonesian). The app follows the system locale; there is no in-app language picker.

`CofinanceTheme` wraps Material 3 with fixed light and dark colour schemes, the Manrope type family, and custom shapes (`app/theme/`). It follows the system dark-mode setting and does not use dynamic colour.

## Naming conventions

| Type | Pattern | Example |
|------|---------|---------|
| ViewModel | `[Feature]ViewModel` | `ActivityViewModel` |
| Screen | `[Feature]Screen` | `LoginScreen` |
| UiState | `[Feature]UiState` | `ActivityUiState` |
| UiEvent | `[Feature]UiEvent` | `ActivityUiEvent` |
| Repository | `[Entity]Repository` / `[Entity]RepositoryImpl` | `AccountRepository` |
| UseCase | `[Action][Entity]UseCase` | `DeleteTransactionUseCase` |

### Where the code departs from them

New code should follow the table above. These are existing inconsistencies, recorded so they are not mistaken for a second convention.

- `AddNewViewModel` and its state types live in `AddTransactionViewModel.kt`; the screen is `AddTransactionScreen`.
- `AddAccountViewModel` lives in `pages/addnew/`, while `AddAccountScreen` lives in `pages/addaccount/`.
- `AccountViewModel` and `ProfileViewModel` each declare a bare `UiState`.
- `ProfileEvent`, `EditProfileEvent`, and `AddAccountEvent` drop "Ui"; security settings use `SecurityIntent`.
- "UiEvent" names both user inputs and outgoing one-off effects, depending on the feature.
- Event types are sealed classes in some features and sealed interfaces in others.
- `AddAccountUseCase` and `DeleteAccountUseCase` return Kotlin `Result` rather than `ResultState`; `ResetAccountBalanceUseCase` is a plain `suspend` function.
- Several ViewModels inject repositories directly instead of going through use cases.
- The OCR engine, parser, and scanner are bound in `networkModule` although they make no network calls.

## Open

- **Dead code.** `CameraViewModel` is mostly commented-out CameraX code and is injected nowhere. `createCameraPermissionHandler` and `RationalPermissionDialog` have no callers. `AccountDocument` and `TransactionDocument` are unreferenced.
- `LocalAppLocalization` is provided at the root and read nowhere.
- Two string keys exist only in Indonesian (`label_account_subtitle`, `description_security_settings`), and neither is referenced.
- The camera error message is hard-coded English in both camera actuals.
- The flash icon does not update when toggled, and capture errors are discarded (`app/pages/camera/CameraScreen.kt:21, 33`).
