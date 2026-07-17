## REMOVED Requirements

### Requirement: Explicit unsupported-platform behavior
**Reason**: Desktop, JS, and WasmJS targets were removed from the project; Android and iOS both have implemented native Google sign-in providers, so no unsupported-platform code path remains.
**Migration**: None. Existing Android and iOS sign-in flows are unaffected.
