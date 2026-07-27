## ADDED Requirements

### Requirement: Automation remains aligned with the supported stack
Continuous integration and delivery workflows SHALL use build inputs, runtime toolchains, and verification tasks that match the current project configuration and supported targets.

#### Scenario: Automation generates local build configuration
- **WHEN** CI or a platform delivery workflow creates `local.properties`
- **THEN** it SHALL provide only the current required build configuration and platform SDK location without provisioning removed Supabase or PowerSync settings

#### Scenario: Linux CI verifies the KMP project
- **WHEN** the CI workflow runs on Linux
- **THEN** it SHALL execute Android lint and Android-hosted unit tests without claiming to execute iOS tests that require macOS

#### Scenario: Android delivery invokes build and distribution tools
- **WHEN** the Android release is built and distributed
- **THEN** the workflow SHALL use a Java runtime compatible with the configured JVM target and a pinned Firebase CLI version running on a supported Node.js runtime
