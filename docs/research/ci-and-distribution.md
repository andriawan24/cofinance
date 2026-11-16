# CI and distribution

**Question:** What do the GitHub workflows do, and how do builds reach testers?

**Last verified:** 2026-11-16 against `ac66286`. Established by reading `.github/workflows/`; no workflow run was inspected.

## Workflows

| Workflow | Trigger | Runner | Result |
|----------|---------|--------|--------|
| `ci.yml` | Push and pull request on `main` and `develop` | `ubuntu-latest` | Android lint and the unit-test tasks |
| `cd-android.yml` | Push to `main`, or manual dispatch with release notes | `ubuntu-latest` | Release APK to Firebase App Distribution |
| `cd-ios.yml` | Push to `main`, or manual dispatch with release notes | `macos-latest` | Ad-hoc IPA to Firebase App Distribution |
| `pr-title-lint.yml` | Pull request opened, edited, or synchronized | — | Enforces a conventional-commit PR title |

Every push to `main` therefore triggers both delivery workflows.

### `ci.yml`

1. Sets up JDK 17 and the Gradle cache.
2. Writes `local.properties` with the six build-configuration keys and `sdk.dir` (`ci.yml:37-47`).
3. Decodes `FIREBASE_ANDROID_CONFIG_BASE64` into `androidApp/google-services.json` and checks that it contains a client for `id.andriawan.cofinance` (`ci.yml:49-78`).
4. Runs `./gradlew :androidApp:lint` (`ci.yml:81`).
5. Runs `./gradlew :composeApp:testAndroidHostTest :androidApp:testDebugUnitTest` (`ci.yml:84`).
6. Uploads the lint and test reports.

CI does not build iOS and does not run any `:core` task.

### `cd-android.yml`

1. Sets up JDK 17 and Node 22, writes `local.properties`, and provisions `google-services.json` as CI does.
2. Runs `./gradlew :androidApp:assembleRelease` and uploads the APK as an artifact.
3. Validates `FIREBASE_APP_ID_ANDROID` and `GOOGLE_SERVICE_JSON`, which must decode to a service-account JSON (`cd-android.yml:114-157`).
4. Installs `firebase-tools@15.24.0`.
5. Creates the `internal-testers` App Distribution group if it does not exist (`cd-android.yml:162-184`).
6. Distributes the APK to `internal-testers` with the release notes and commit SHA (`cd-android.yml:186-203`).

A missing or malformed distribution input fails the job. It is not converted into a skipped step.

### `cd-ios.yml`

1. Sets up JDK 17, writes `local.properties`, and decodes `FIREBASE_IOS_CONFIG_BASE64` into `iosApp/iosApp/GoogleService-Info.plist`, checking it with `plutil -lint` and against the bundle identifier (`cd-ios.yml:52-88`).
2. Validates the signing secrets. **If any is missing, it sets `can_build=false`, warns, and skips the remaining steps rather than failing** (`cd-ios.yml:90-155`).
3. Imports the signing certificate into a temporary keychain and installs the provisioning profile.
4. Runs `./gradlew :composeApp:linkReleaseFrameworkIosArm64`, then `xcodebuild archive` with `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES`, then `xcodebuild -exportArchive` using `iosApp/ExportOptions.plist`.
5. Uploads the IPA and distributes it to `internal-testers` through `wzieba/Firebase-Distribution-Github-Action@v1`.

Neither delivery workflow uploads to the Play Store, TestFlight, or the App Store.

### `pr-title-lint.yml`

Uses `amannn/action-semantic-pull-request@v5`. Allowed types are `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `perf`, `style`, `build`, and `ci`. A scope is optional, and the subject must start with a lowercase letter (`pr-title-lint.yml:22-38`).

## Secrets

Names only. Values are never written to the repository.

| Secret | Used by | Purpose |
|--------|---------|---------|
| `GOOGLE_AUTH_CLIENT_ID` | all three build workflows | Build configuration |
| `STORAGE_BASE_URL`, `STORAGE_ACCESS_KEY`, `STORAGE_ACCESS_SECRET`, `STORAGE_BUCKET_NAME`, `STORAGE_REGION` | all three build workflows | Build configuration |
| `FIREBASE_ANDROID_CONFIG_BASE64` | CI, Android delivery | Base64 of `google-services.json` |
| `FIREBASE_IOS_CONFIG_BASE64` | iOS delivery | Base64 of `GoogleService-Info.plist` |
| `FIREBASE_APP_ID_ANDROID`, `FIREBASE_APP_ID_IOS` | delivery | App Distribution target |
| `GOOGLE_SERVICE_JSON` | delivery | Base64 of the App Distribution service account |
| `IOS_CERTIFICATE_BASE64`, `IOS_CERTIFICATE_PASSWORD`, `IOS_KEYCHAIN_PASSWORD`, `IOS_PROVISION_PROFILE_BASE64` | iOS delivery | Code signing |

## Rules carried over from OpenSpec

These were requirements in the removed `ci-firebase-configuration` spec, and the workflows implement them.

- Reconstruct each platform's Firebase application configuration from a dedicated secret before any step that needs it, and fail before invoking the build when the secret is missing, malformed, or for a different application.
- Keep the application configuration separate from the service-account credential. App Distribution authenticates with the service account, never with either application configuration.
- Never commit, print, cache, or upload a decoded configuration file.
- Use a pinned Firebase CLI version on a supported Node runtime, and official action majors that run on Node 24.

## Open

- **CI's test step runs no tests.** The suite was deleted (see [build-and-modules.md](build-and-modules.md)), so the step passes vacuously. A green CI run currently means "lint passed and the project compiled".
- **Delivery ships debug-signed Android builds.** Neither workflow sets `KEYSTORE_PATH`, so `assembleRelease` falls back to the debug signing config.
- **iOS delivery can skip silently.** A missing signing secret produces a successful run that built nothing, which is the outcome the Android workflow was specifically changed to prevent.
- Delivery runs on every push to `main` with no test gate in front of it.
