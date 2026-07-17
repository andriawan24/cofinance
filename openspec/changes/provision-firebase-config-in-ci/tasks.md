## 1. Repository Secret Provisioning

- [x] 1.1 Create distinct base64 GitHub repository secrets from the ignored Android and iOS Firebase application configuration files, and verify only their names are visible.

## 2. Workflow Configuration

- [x] 2.1 Update CI to validate and materialize the Android Firebase configuration before lint and unit tests.
- [x] 2.2 Update Android delivery to validate and materialize the Android Firebase configuration before release assembly while retaining the separate App Distribution service-account flow.
- [x] 2.3 Update iOS delivery to validate and materialize the iOS Firebase configuration before signing checks, framework compilation, and archive.

## 3. Verification and Delivery

- [x] 3.1 Verify decoded secrets match the local source files without printing their contents, and verify workflow files never upload or log Firebase application configuration.
- [x] 3.2 Run Android lint and unit tests with ignored configuration files, validate workflow YAML, and run strict OpenSpec validation.
- [x] 3.3 Commit only the CI/OpenSpec change, push it to `main`, and confirm the resulting GitHub Actions runs pass or report any independent residual failures.
