## Why

Cofinance currently builds Android, iOS, Desktop (JVM), JS, and WasmJS targets, but only Android and iOS are actively shipped and maintained. The Desktop/Web targets add build time, dependency surface (webpack, storybook, yarn lockfiles), and expect/actual maintenance burden without a shipping product behind them. Dropping them narrows the module to the platforms actually supported.

## What Changes

- **BREAKING**: Remove the Desktop (JVM) target from `composeApp` — target declaration, `jvmMain`/`jvmTest` source sets, and desktop-only dependencies.
- **BREAKING**: Remove the JS and WasmJS targets from `composeApp` — target declarations, `jsMain`/`wasmJsMain` source sets, webpack config (`composeApp/webpack.config.d/`), storybook tooling (`composeApp/storybook/`, `buildSrc/src/main/kotlin/ComposeStorybookGeneratorTask.kt`), and `kotlin-js-store/` lockfiles.
- Remove version catalog entries in `gradle/libs.versions.toml` that are only used by the Desktop/JS/WasmJS targets or the storybook tooling.
- Update shared source set hierarchy (e.g. `nonWebMain`, common `expect`/`actual` wiring) so it only branches for Android and iOS.
- Update `openspec/specs/android-kmp-build/spec.md` to describe only Android and iOS as supported targets.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `android-kmp-build`: "Preserve multiplatform targets" requirement changes from Android/iOS/Desktop/JS/WasmJS to Android and iOS only; source-set hierarchy requirements that reference Desktop/JS/WasmJS are removed or narrowed accordingly.
- `dependency-management`: "Scope platform engines and native libraries" and "Verify upgrades before adoption" drop Desktop/JS/WasmJS scenarios (CIO engine, PowerSync-on-Desktop, cross-target compile verification).
- `google-authentication`: "Explicit unsupported-platform behavior" requirement is removed — no unsupported platform remains once only Android and iOS ship.
- `offline-data-sync`: "Native targets use a local PowerSync database" drops Desktop; "Web targets remain online-only" requirement is removed along with the `OnlineOnlyDatabase` implementation it described.

## Impact

- `composeApp/build.gradle.kts` — remove `jvm()`, `js()`, `wasmJs()` target blocks and associated source sets/dependencies.
- `composeApp/webpack.config.d/`, `composeApp/storybook/` — removed entirely.
- `buildSrc/src/main/kotlin/ComposeStorybookGeneratorTask.kt` — removed entirely.
- `kotlin-js-store/` (`yarn.lock`, `wasm/yarn.lock`) — removed entirely.
- `gradle/libs.versions.toml` — prune Desktop/JS/WasmJS/storybook-only entries.
- `androidApp/build.gradle.kts` — no target removal, but review for any references to removed shared source sets.
- `composeApp/src/{desktopMain,desktopTest,jsMain,jsTest,wasmJsMain,wasmJsTest,webMain}/` — removed entirely (dead once the targets are gone).
- `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/data/local/OnlineOnlyDatabase.kt` — removed; it only backed the now-removed web targets.
- `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/data/datasource/SupabaseDataSource.kt` — removed the `getAccounts`/`addAccount`/`getTransactions`/`createTransaction` region that existed solely to back `OnlineOnlyDatabase`.
- `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/data/model/request/{AccountRequest,AddTransactionRequest,GetTransactionsRequest}.kt` — removed; only reachable through the deleted `SupabaseDataSource` region and unused `toRequest()` mappers.
- `.github/workflows/ci.yml` — `:composeApp:desktopTest` replaced with `:composeApp:allTests`.
- `README.md`, `openspec/config.yaml` — platform lists and Web build instructions updated to Android/iOS only.
- `openspec/specs/{android-kmp-build,dependency-management,google-authentication,offline-data-sync}/spec.md` — requirement text updated to match remaining targets.
- No impact to Android or iOS build outputs, signing, or runtime behavior.
