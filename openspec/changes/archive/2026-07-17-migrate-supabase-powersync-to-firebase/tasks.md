## 1. Firebase Project Integration

- [x] 1.1 Replace Supabase and PowerSync catalog/module dependencies and BuildKonfig fields with GitLive Firebase Auth, Firestore, and Storage dependencies plus non-secret Firebase option inputs.
- [x] 1.2 Link the required official Firebase products to the iOS application target and remove the Android PowerSync initializer.
- [x] 1.3 Add cross-platform Firebase initialization and byte-data conversion utilities for Android and iOS.
- [x] 1.4 Process `androidApp/google-services.json` with the documented Google Services Gradle plugin and configure the iOS default Firebase app from `GoogleService-Info.plist` in Swift startup.
- [x] 1.5 Remove duplicated Firebase BuildKonfig option fields and shared programmatic initialization while retaining platform byte-data conversion.

## 2. Authentication and Profiles

- [x] 2.1 Implement a Firebase data source for Google credential login, session access, logout, profile document defaults/updates, and avatar upload.
- [x] 2.2 Migrate authentication repository/domain user mapping from Supabase user metadata to the Firebase-backed user profile.
- [x] 2.3 Switch login, splash, profile logout, and dependency injection to Firebase session behavior without changing navigation or UI events.

## 3. Firestore Finance Persistence

- [x] 3.1 Implement Firestore account reads, realtime observations, creation, updates, balance adjustments, and deletion under the authenticated user.
- [x] 3.2 Implement filtered realtime and one-shot Firestore transaction reads with sender/receiver account hydration.
- [x] 3.3 Implement atomic Firestore transaction creation/update with balance application and reversal, and update repositories to use those atomic operations.
- [x] 3.4 Replace the platform PowerSync database factory with the shared Firestore implementation and remove synchronization lifecycle hooks.

## 4. Removal and Documentation

- [x] 4.1 Remove all Supabase and PowerSync implementation files, imports, configuration keys, dependencies, manifest declarations, and stale comments.
- [x] 4.2 Update repository setup documentation for Firebase configuration, Firestore/Storage schema, authentication enablement, and the absence of app-managed local sync.
- [x] 4.3 Correct setup documentation to require the standard Android and iOS Firebase configuration files and keep only Gemini and Google OAuth values in `local.properties`.

## 5. Final Verification

- [x] 5.1 Verify each changed requirement and scenario through final diff inspection and targeted static searches, with no Supabase/PowerSync runtime references remaining.
- [x] 5.2 Run strict OpenSpec validation and one final project build attempt after all implementation edits; record any external Firebase/Xcode prerequisite separately from code failures.

  Verification: strict validation passed. The final `assemble` processed Android debug/release Firebase configuration, packaged Android debug/release, compiled all shared target sources, and linked both iOS debug frameworks. Kotlin/Native then exhausted the configured 4 GB Gradle heap while optimizing `linkReleaseFrameworkIosArm64`; no source or Firebase configuration compilation error remained. A subsequent unsigned iOS Simulator debug build succeeded with the target-associated `GoogleService-Info.plist` and native `FirebaseApp.configure()` startup.
- [x] 5.3 Synchronize the validated delta specifications into the main OpenSpec specifications and validate the resulting main specs.
