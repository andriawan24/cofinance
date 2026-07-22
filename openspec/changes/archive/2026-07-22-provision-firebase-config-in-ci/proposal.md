## Why

Firebase platform configuration is intentionally excluded from version control, but the GitHub Actions workflows still expect those files to exist after checkout. Android lint and release builds now fail immediately, and the iOS archive path would fail once signing is enabled.

## What Changes

- Store the Android and iOS Firebase application configuration as separate base64-encoded GitHub Actions secrets.
- Materialize `google-services.json` before Android lint, tests, and release builds.
- Materialize `GoogleService-Info.plist` before the iOS framework and archive build path.
- Fail early with a clear, credential-free message when a required application configuration secret is missing or invalid.
- Keep generated configuration files ignored and prevent their contents from appearing in workflow logs or artifacts.

## Capabilities

### New Capabilities
- `ci-firebase-configuration`: Securely provision platform Firebase application configuration for CI and delivery workflows.

### Modified Capabilities

None.

## Impact

- Affected workflows: `.github/workflows/ci.yml`, `.github/workflows/cd-android.yml`, and `.github/workflows/cd-ios.yml`.
- Affected external configuration: two new repository secrets containing the platform application configuration files.
- No runtime APIs, application source, dependencies, or persisted data contracts change.
