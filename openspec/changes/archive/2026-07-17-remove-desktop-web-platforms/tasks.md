## 1. Remove Desktop/JS/WasmJS Kotlin targets

- [x] 1.1 Remove `jvm("desktop")`, `js { ... }`, and `wasmJs { ... }` target declarations from `composeApp/build.gradle.kts`
- [x] 1.2 Remove `compose.desktop { application { ... } }` block from `composeApp/build.gradle.kts`
- [x] 1.3 Remove `desktopMain`/`jsMain`/`wasmJsMain` source-set dependency blocks; re-wire `nonWebMain` to depend only on `androidMain`/`iosMain` (or inline into those sets if simpler)
- [x] 1.4 Remove now-unused imports (`org.jetbrains.compose.desktop.application.dsl.TargetFormat`, `ExperimentalWasmDsl`) from `composeApp/build.gradle.kts`

## 2. Prune dependencies

- [x] 2.1 Grep `gradle/libs.versions.toml` for catalog keys only referenced by the removed Desktop/JS/WasmJS blocks (e.g. `ktor-client-cio`, desktop-only Compose entries) and confirm no other module references them
- [x] 2.2 Remove confirmed-unused catalog entries from `gradle/libs.versions.toml`
- [x] 2.3 Re-run repo-wide grep for each removed catalog key to confirm zero remaining references

## 3. Sync specs

- [x] 3.1 Apply the `android-kmp-build` delta spec (MODIFIED "Preserve multiplatform targets") to `openspec/specs/android-kmp-build/spec.md`
- [x] 3.2 Apply the `dependency-management` delta spec to `openspec/specs/dependency-management/spec.md`
- [x] 3.3 Apply the `google-authentication` delta spec (REMOVED "Explicit unsupported-platform behavior") to `openspec/specs/google-authentication/spec.md`
- [x] 3.4 Apply the `offline-data-sync` delta spec (MODIFIED "Native targets...", REMOVED "Web targets remain online-only") to `openspec/specs/offline-data-sync/spec.md`

## 5. Remove dead platform-specific source code

- [x] 5.1 Delete `composeApp/src/{desktopMain,desktopTest,jsMain,jsTest,wasmJsMain,wasmJsTest,webMain}/` (dead `expect`/`actual` implementations, storybook runtime, webpack HTML/CSS)
- [x] 5.2 Delete `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/data/local/OnlineOnlyDatabase.kt` (only backed the removed web targets)
- [x] 5.3 Remove the `getAccounts`/`addAccount`/`getTransactions`/`createTransaction` region from `SupabaseDataSource.kt` (only reachable from the deleted `OnlineOnlyDatabase`) and prune now-unused imports
- [x] 5.4 Remove unused `AccountRequest`/`AddTransactionRequest`/`GetTransactionsRequest` model classes and their unused `toRequest()` mapper extensions on `AccountParam`/`AddTransactionParam`/`GetTransactionsParam`
- [x] 5.5 Update the stale Desktop/JS/WasmJS mention in `CofinanceDatabase.kt`'s doc comment

## 6. Update CI, README, and OpenSpec context

- [x] 6.1 Replace `:composeApp:desktopTest` with `:composeApp:allTests` in `.github/workflows/ci.yml`
- [x] 6.2 Update `README.md` platform list, project structure tree, and Web build sections to Android/iOS only
- [x] 6.3 Update `openspec/config.yaml` context to list Android and iOS only

## 7. Verify

- [x] 7.1 Run `./gradlew :composeApp:compileAndroidMain` (Android compile task) and confirm success
- [x] 7.2 Run an iOS compile/link task (`./gradlew :composeApp:compileKotlinIosSimulatorArm64`) and confirm success
- [x] 7.3 Run `./gradlew build` (or targeted equivalent) and confirm no references to removed targets remain and the build has no unresolved-reference errors
- [x] 7.4 Confirm no leftover files reference removed targets (`composeApp/webpack.config.d/`, `composeApp/storybook/`, `buildSrc/.../ComposeStorybookGeneratorTask.kt`, `kotlin-js-store/`, `composeApp/src/{desktopMain,jsMain,wasmJsMain,webMain}/` stay absent)
- [x] 7.5 Run `openspec validate remove-desktop-web-platforms --strict` and confirm it passes
