## Why

The Android delivery workflow uploads a release to Firebase but then fails with a 404 while assigning the nonexistent `internal-testers` group, so GitHub Actions never reports a successful distribution. The workflows also still provision removed Supabase and PowerSync settings and need to reflect the current Android/iOS-only Firebase stack and upgraded build toolchain.

## What Changes

- Align CI and delivery build inputs with the current Firebase-based application configuration and supported Android/iOS targets.
- Upgrade official GitHub Actions to Node 24-based majors so hosted runs do not rely on the deprecated Node 20 action runtime.
- Replace the fragile third-party Android distribution step with a pinned Firebase CLI flow that authenticates through the existing service account, ensures the configured tester group exists, and distributes the release.
- Make missing or invalid Android distribution credentials fail the delivery job instead of silently skipping the required outcome.
- Verify distribution through the GitHub Actions result and Firebase CLI success output without logging credentials or decoded Firebase configuration.
- Keep iOS signing/distribution behavior out of scope because the repository does not currently have the required iOS signing and App Distribution secrets.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `ci-firebase-configuration`: Require the Android delivery workflow to complete Firebase App Distribution to a valid tester group and fail safely when distribution cannot run.
- `dependency-management`: Require automation build inputs and verification tasks to remain aligned with the supported targets and current build toolchain.

## Impact

The change affects `.github/workflows/ci.yml`, `.github/workflows/cd-android.yml`, `.github/workflows/cd-ios.yml`, the CI-facing dependency specification, and Firebase App Distribution automation. It uses the existing protected Firebase application ID, Android application configuration, and service-account secrets; no new application runtime dependency or committed secret is introduced.
