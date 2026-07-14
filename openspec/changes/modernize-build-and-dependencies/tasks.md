## 1. Repair target hierarchy

- [ ] 1.1 Replace the current ad-hoc native/web `dependsOn` graph with a supported hierarchy configuration.
- [ ] 1.2 Verify that `iosMain` and `nonWebMain` participate in both iOS compilations.
- [ ] 1.3 Enable or explicitly scope Android host tests for the existing `commonTest` sources.

## 2. Modernize Gradle and AGP configuration

- [ ] 2.1 Remove AGP flags scheduled for removal in AGP 10 and verify current defaults.
- [ ] 2.2 Replace Gradle container delegate syntax scheduled for removal in Gradle 10.
- [ ] 2.3 Replace legacy project dependency notation and set a stable `buildSrc` root name.
- [ ] 2.4 Replace internal AGP local-property access with provider-backed local and CI secret inputs.

## 3. Correct dependency topology

- [ ] 3.1 Keep Ktor core in common code and scope OkHttp, Darwin, and CIO to Android, iOS, and Desktop respectively.
- [ ] 3.2 Add Coil's Ktor 3 network integration and verify remote images.
- [ ] 3.3 Consolidate aligned Compose catalog versions without forcing independently versioned modules onto the same number.
- [ ] 3.4 Narrow PowerSync and logging exposure from `api` to `implementation` where public signatures permit.
- [ ] 3.5 Record a separate decision for retaining or replacing the Gemini wrapper.

## 4. Verify the refreshed stack

- [ ] 4.1 Run warning-enabled Android and Desktop builds.
- [ ] 4.2 Compile iOS device and simulator targets.
- [ ] 4.3 Compile JS and WasmJS targets and run shared tests.
- [ ] 4.4 Smoke-test authentication, remote images, Supabase networking, PowerSync lifecycle, and receipt scanning on affected platforms.
- [ ] 4.5 Validate the OpenSpec change and update findings with any remaining accepted warnings.

