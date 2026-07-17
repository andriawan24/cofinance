## Context

The Android Google Services Gradle plugin requires `androidApp/google-services.json` during lint and assembly. The iOS application calls `FirebaseApp.configure()` and uses a filesystem-synchronized Xcode group, so `iosApp/iosApp/GoogleService-Info.plist` must exist before archive. Both files are intentionally ignored after their removal from Git history. Current GitHub Actions jobs only create `local.properties`; they do not restore either Firebase application configuration file.

The repository already has a `GOOGLE_SERVICE_JSON` secret containing a Firebase service-account credential for App Distribution. That credential has a different schema and privilege profile from either client application configuration and must remain separate.

## Goals / Non-Goals

**Goals:**

- Restore Android lint, unit-test, and release-build workflows without committing Firebase application configuration.
- Make the iOS archive path self-contained when signing credentials are available.
- Validate required secrets before expensive build steps and report only safe error messages.
- Preserve the ignored local developer configuration workflow.

**Non-Goals:**

- Change Firebase runtime initialization, Gradle plugins, application identifiers, signing, or App Distribution credentials.
- Make iOS signing available where signing secrets are absent.
- Reintroduce platform configuration files to Git history or workflow artifacts.

## Decisions

### Use separate base64 repository secrets per platform

`FIREBASE_ANDROID_CONFIG_BASE64` will contain the base64 form of `google-services.json`, and `FIREBASE_IOS_CONFIG_BASE64` will contain the base64 form of `GoogleService-Info.plist`. Separate names prevent either value from being confused with the privileged `GOOGLE_SERVICE_JSON` service-account secret. Base64 is transport encoding only; confidentiality comes from GitHub Actions secret storage and masking.

Alternative considered: synthesize configuration from many individual secrets. This duplicates the vendor file schemas in YAML and makes future Firebase Console changes harder to apply correctly.

### Decode and validate before platform build steps

Each workflow will pass the secret through a step-level environment variable and use Ruby's standard Base64 library to strictly decode it to the ignored platform path. Ruby is available on both GitHub-hosted Ubuntu and macOS runners and avoids GNU/BSD `base64` flag differences. Android JSON and iOS plist structure will be validated without printing their contents.

Alternative considered: interpolate secrets directly in shell commands. Step-level environment variables reduce expression-injection and quoting risks.

### Treat missing or invalid application configuration as a hard failure

CI and delivery builds cannot produce a correctly configured Firebase application without these files. Provisioning therefore fails early rather than silently skipping lint, assembly, or archive. The iOS provisioning step runs before signing validation so an apparent successful job cannot hide missing Firebase configuration.

## Risks / Trade-offs

- [Repository secrets are unavailable to untrusted fork pull requests] → Such jobs will fail at the explicit provisioning step instead of later in Gradle; supporting forks with synthetic Firebase projects remains out of scope.
- [Secret values can become stale after Firebase application reconfiguration] → Keep each secret named by platform and replace it from the matching downloaded vendor file.
- [A decoded file could be uploaded accidentally] → Write only to ignored source paths, keep artifact paths limited to reports/binaries, and verify workflows never print or upload configuration files.
- [The iOS workflow may still skip archives] → Existing signing-secret gating remains unchanged and is reported independently from Firebase provisioning.

## Migration Plan

1. Populate both new repository secrets from the currently ignored local vendor files.
2. Add provisioning and validation steps to CI, Android CD, and iOS CD.
3. Validate workflow syntax and execute Android lint/tests locally with the ignored configuration.
4. Push the workflow change and monitor CI and platform delivery runs.
5. Roll back by reverting the workflow commit; repository secrets may remain unused or be deleted separately.

## Open Questions

None.
