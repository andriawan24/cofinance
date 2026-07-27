## Context

The current Android delivery job builds and uploads the release APK successfully, then passes the hard-coded alias `internal-testers` to `wzieba/Firebase-Distribution-Github-Action@v1`. Firebase returns HTTP 404 during group assignment because that tester group does not exist, leaving the workflow failed even though a release object was created. CI and delivery also write obsolete Supabase and PowerSync values into `local.properties`; the current BuildKonfig contract requires only Gemini and Google authentication inputs.

The current stack is Android/iOS-only KMP with Gradle 9.6.1, AGP 9.3.1, Kotlin 2.4.10, and JDK 17 bytecode. Android is the affected distributable platform: its required Firebase secrets exist, while iOS signing and Firebase App Distribution secrets do not.

## Goals / Non-Goals

**Goals:**

- Make a main-branch or manually dispatched Android delivery finish successfully only after Firebase accepts the APK distribution.
- Keep the `internal-testers` group usable without requiring a one-time manual console prerequisite.
- Align CI-generated build properties and Gradle tasks with the current application and supported targets.
- Preserve secret isolation and provide actionable failures without printing secret values.
- Produce local and remote evidence for the affected workflow and OpenSpec requirements.

**Non-Goals:**

- Add or manage individual tester identities.
- Add missing Apple certificates, provisioning profiles, or iOS App Distribution credentials.
- Change application runtime behavior, dependency versions, signing policy, or release versioning.

## Decisions

### Use the pinned official Firebase CLI for Android distribution

The workflow will run Firebase CLI `15.24.0` under Node 22, authenticate with the existing decoded service-account file through `GOOGLE_APPLICATION_CREDENTIALS`, and invoke `appdistribution:distribute`. This exposes group-management and distribution commands in one supported interface and avoids the opaque container wrapper that currently reports the failure only after upload.

The alternative was to keep the third-party action and remove its `groups` input. That would upload a release but would not grant a tester group access, so it would not meet the distribution requirement.

### Ensure the stable tester group exists before distribution

The workflow will query tester groups for the Firebase project derived from validated `google-services.json`. If alias `internal-testers` is absent, it will create the group before distributing the APK. Group creation is idempotent at the workflow level because the list check precedes it.

The alternative was a new repository secret containing a group alias. The repository has no such secret today, and merely renaming the alias would retain an undocumented manual prerequisite.

### Treat distribution prerequisites as required delivery inputs

Android delivery will fail before Gradle or Firebase work when the app ID, service account, or application configuration is missing or invalid. A successful delivery job therefore means the build and distribution path actually ran; it no longer means that a required step was silently skipped.

### Keep current toolchain constraints explicit

Workflows will retain JDK 17, which matches the project JVM target and AGP 9.3.1 requirements, and add Node 22 for the pinned Firebase CLI. Generated `local.properties` will contain only `gemini.api_key`, `google_auth_client_id`, and Android SDK location where applicable. CI will execute Android-hosted tests rather than a cross-platform aggregate that implies iOS tests can run on Linux.

## Risks / Trade-offs

- [Service account cannot manage tester groups] → The group setup step fails with a clear error; remote evidence will identify whether the credential needs the Firebase App Distribution Admin role.
- [Pinned Firebase CLI becomes stale] → The explicit version makes runs reproducible and can be updated through the dependency-management process.
- [An empty group receives the release] → The workflow guarantees Firebase group assignment, but tester membership remains an administrative concern outside source control.
- [Existing uncommitted application changes affect local builds] → Verification records the working-tree context, and the delivery commit will include only this change's OpenSpec and workflow files.

## Migration Plan

1. Validate the updated YAML and run the relevant Gradle configuration/build checks locally without exposing configuration contents.
2. Commit the OpenSpec artifacts and workflow changes on the current branch and push to `main`, which triggers CI and Android delivery.
3. Monitor the exact pushed SHA. If group creation or distribution fails, use the Actions logs to update this design/specification before applying the next focused fix.
4. Confirm both CI and Android delivery succeed and that the Firebase CLI output reports a release distributed to `internal-testers`.
5. Roll back by reverting the workflow commit; existing Firebase releases and tester groups are retained because rollback must not delete external distribution data.

## Open Questions

None. Tester membership can be populated later through Firebase administration without changing the delivery contract.
