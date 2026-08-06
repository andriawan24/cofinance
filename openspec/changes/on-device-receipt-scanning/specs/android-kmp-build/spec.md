## MODIFIED Requirements

### Requirement: Keep build-time secrets out of source control
The build SHALL source Supabase, Google authentication, and PowerSync configuration from ignored local configuration or environment-backed secret inputs. The build SHALL NOT declare or generate a Gemini configuration value, because receipt scanning no longer calls a remote AI service.

#### Scenario: Build configuration is generated
- **WHEN** BuildKonfig generates constants
- **THEN** required values SHALL be present without being committed, rendered in documentation, screenshots, or emitted to CI logs

#### Scenario: Gemini configuration is absent
- **WHEN** the build configuration and its secret inputs are inspected
- **THEN** no Gemini API key field SHALL be declared, generated, or provisioned
