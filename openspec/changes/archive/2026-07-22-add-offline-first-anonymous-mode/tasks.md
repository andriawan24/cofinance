## 1. Local Persistence Foundation

- [x] 1.1 Configure stable Room KMP, bundled SQLite, KSP, schema output, and Android/iOS database builders.
- [x] 1.2 Implement local account and transaction entities, reactive DAO queries, filtering/hydration mapping, and atomic balance mutations behind `CofinanceDatabase`.
- [x] 1.3 Bind the durable local database as the repository source of truth and remove mandatory Firebase user ids from local finance operations.

## 2. Authentication and Synchronization

- [x] 2.1 Introduce an injectable session policy that exposes current signed-in state for repositories and presentation logic.
- [x] 2.2 Separate authenticated Firestore finance access into a remote synchronization boundary with user-scoped snapshot reads and upserts.
- [x] 2.3 Implement deterministic sign-in synchronization (remote-only import, local-wins merge, accounts-before-transactions upload) and local-first authenticated write-through.
- [x] 2.4 Run synchronization after Google/Firebase login and make logout return to local-only mode without clearing local finance data.

## 3. Local-First Experience and AI Gate

- [x] 3.1 Change splash/main navigation so missing authentication enters the main experience and direct authenticated-only routes are guarded.
- [x] 3.2 Add signed-out profile copy and a prominent sign-in-to-sync CTA while retaining authenticated profile/edit/logout behavior.
- [x] 3.3 Gate receipt/camera AI entry points with sign-in guidance and enforce authenticated-only scanning in `TransactionRepository` before Gemini is invoked.
- [x] 3.4 Add complete English and Indonesian localization for local-only, synchronization, and AI sign-in messaging.

## 4. Coverage and End-Only Verification

- [x] 4.1 Add common unit test sources for signed-out local access, merge policy/write-through behavior, and repository-level AI denial.
- [x] 4.2 At the end of implementation, run build-only Gradle verification that compiles common, Android, iOS, and unit-test sources without running functional or standalone test tasks.
- [x] 4.3 Audit the final diff against every requirement/scenario and run strict OpenSpec validation.
- [x] 4.4 Synchronize the change's delta specs into main specs, validate the resulting main specifications, and record all tasks complete only after evidence succeeds.
