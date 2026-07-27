## Verification Evidence

### Local

- `go run github.com/rhysd/actionlint/cmd/actionlint@v1.7.7`: passed for all workflow files.
- Firebase CLI `15.24.0` exposed `appdistribution:groups:list`, `appdistribution:groups:create`, and `appdistribution:distribute` with the expected options.
- `./gradlew :androidApp:assembleRelease --no-daemon --warning-mode=all`: passed against the upgraded working-tree stack.
- A detached worktree at the exact committed baseline passed `./gradlew :androidApp:lint :composeApp:testAndroidHostTest :androidApp:testDebugUnitTest --no-daemon`.

### Remote run for `38ca0c015fdb68ee74b05f701cf0b6df82bdeb71`

- CI: https://github.com/andriawan24/cofinance/actions/runs/30256328805 — succeeded; Android lint and both configured unit-test tasks executed successfully.
- Android delivery: https://github.com/andriawan24/cofinance/actions/runs/30256328485 — succeeded; every build and distribution step executed.
- Firebase App Distribution release: `0.0.1 (8)`, release ID `02o4of7q4nrm0`.
- Firebase CLI created `Internal Testers` with alias `internal-testers`, uploaded the release, added release notes, and reported `distributed to testers/groups successfully`.

### Requirement and scenario audit

| Capability | Scenario | Evidence | Result |
| --- | --- | --- | --- |
| `ci-firebase-configuration` | Tester group already exists | Pending a second delivery run after the first run created the group. | Pending |
| `ci-firebase-configuration` | Tester group does not exist | Android step `Ensure Firebase tester group exists` created alias `internal-testers`; distribution then succeeded. | Pass |
| `ci-firebase-configuration` | Firebase rejects distribution | The workflow uses `set -euo pipefail` and no conditional success override around group setup or distribution; command failure exits the job. | Pass (structural) |
| `ci-firebase-configuration` | Distribution input is missing or invalid | Configuration and credential validation paths exit nonzero with diagnostics and do not print decoded contents. | Pass (structural) |
| `ci-firebase-configuration` | Delivery job reports success | Android run succeeded only after the executed Firebase CLI step reported successful tester/group distribution. | Pass |
| `dependency-management` | Automation generates local build configuration | All three workflows write only Gemini, Google authentication, and platform SDK values. | Pass |
| `dependency-management` | Linux CI verifies the KMP project | CI executed Android lint, `testAndroidHostTest`, and `testDebugUnitTest` successfully. | Pass |
| `dependency-management` | Android delivery invokes build and distribution tools | Run used JDK 17, Node 22, and Firebase CLI `15.24.0`; release build and distribution succeeded. | Pass |
