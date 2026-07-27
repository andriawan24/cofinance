## 1. Workflow Alignment

- [x] 1.1 Remove obsolete Supabase and PowerSync inputs from CI and delivery-generated `local.properties`
- [x] 1.2 Update Linux CI to run the current Android-hosted lint and unit-test tasks
- [x] 1.3 Replace Android's third-party Firebase upload action with pinned Node 22 and Firebase CLI 15.24.0 setup
- [ ] 1.4 Upgrade official workflow actions to Node 24-based majors and remove hosted-runner deprecation annotations

## 2. Reliable Firebase Distribution

- [x] 2.1 Make Android App Distribution credentials mandatory and derive the validated Firebase project ID without exposing configuration
- [x] 2.2 Add idempotent `internal-testers` group discovery/creation and distribute the release APK to that group
- [x] 2.3 Validate workflow syntax, Firebase CLI command availability, and the Android release build locally

## 3. Remote Verification

- [x] 3.1 Commit only the scoped OpenSpec and workflow changes and push them to the repository
- [x] 3.2 Monitor CI and Android delivery for the pushed SHA, diagnose failures, and apply validated corrections until both succeed
- [x] 3.3 Confirm the successful Android run executed Firebase distribution rather than skipping it and record requirement-level evidence

## 4. OpenSpec Completion

- [ ] 4.1 Re-run required local verification and compare the final diff and remote evidence against every scenario
- [ ] 4.2 Strictly validate the change, synchronize delta specifications into main specs, and validate the resulting main specifications
