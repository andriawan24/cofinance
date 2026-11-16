# History

**Question:** Which changes were delivered under OpenSpec, and what did each one decide?

**Last verified:** 2026-11-16. Compiled from the OpenSpec `changes/` tree before it was removed. The full proposal, design, and task files remain in git history under `openspec/` — the last commit containing them is `ac66286`.

## Why this note exists

From July to November 2026 the project planned every implementation change as an OpenSpec change: a proposal, a design, delta specs, and a task list. On 2026-11-16 that workflow was replaced by the research notes in this directory. The specs had drifted from the code — they still named Supabase, PowerSync, and Gemini months after all three were removed — and the task lists recorded test evidence for a suite that no longer exists.

This note keeps the one thing from that tree that the code cannot tell you: what was decided, when, and why.

## Delivered changes

| Date | Change | What it did and why |
|------|--------|---------------------|
| 2026-07-14 | Modernize build and dependencies | Fixed iOS compilation, which was failing because shared iOS actuals were excluded by the source-set hierarchy, and moved the build off APIs scheduled for removal in Gradle 10 and AGP 10. |
| 2026-07-14 | Enforce OpenSpec agent workflow | Made OpenSpec mandatory for agent-driven changes. Superseded by this directory. |
| 2026-07-14 | Add OpenSpec continue skill | Added a skill for resuming an interrupted change. Superseded. |
| 2026-07-17 | Migrate Supabase and PowerSync to Firebase | Consolidated authentication, profiles, files, and finance data on Firebase, removing the PowerSync synchronization layer. |
| 2026-07-17 | Remove desktop and web platforms | Dropped the JVM desktop, JS, and WasmJS targets. Only Android and iOS were shipped, and the others added build time, dependencies, and `expect`/`actual` upkeep with no product behind them. |
| 2026-07-22 | Add offline-first anonymous mode | Made the app usable with no account, on durable on-device data, with sign-in as an opt-in for cloud sync. Introduced Room as the source of truth. |
| 2026-07-22 | Provision Firebase config in CI | Reconstructed the git-ignored Firebase configuration files from secrets, so CI and delivery could build. |
| 2026-08-04 | Update Firebase distribution workflows | Fixed Android delivery, which uploaded a release and then failed assigning it to a tester group that did not exist. Removed leftover Supabase and PowerSync settings from the workflows. |
| 2026-08-23 | Remove unused `buildSrc` module | Deleted an empty `buildSrc` that Gradle still configured on every invocation. |

## Changes in progress when OpenSpec was removed

Both were implemented and merged. What remained open was verification that needs real hardware, real receipts, or a live Firebase project. Those items now live in the "Open" section of the relevant note.

| Change | State at removal | Now documented in |
|--------|------------------|-------------------|
| On-device receipt scanning | 22 of 31 tasks done. Blocked on a corpus of real receipts and on manual device checks. | [receipt-scanning.md](receipt-scanning.md) |
| End-to-end encrypted sync | 33 of 40 tasks done. Blocked on iOS Keychain tests that cannot run in the test harness, and on manual end-to-end and restore passes. | [client-side-encryption.md](client-side-encryption.md), [app-lock.md](app-lock.md) |

## Things that changed after the specs were written

These are the places where the OpenSpec text and the code disagreed at removal. The research notes follow the code.

- **The test suite was deleted** in `56bcd9a`, and the `:core-testing` module that held shared fakes in `e90920b`, ahead of a planned integration-test stage. Task lists that cite test counts describe evidence that cannot be reproduced.
- **The recovery phrase stopped being a 12-word BIP39 mnemonic** in `57182fc`. It is now six alphanumeric groups carrying the same 128 bits.
- **The re-entry quiz for the recovery phrase was removed** in `9bbf052`, in favour of copying or saving it.
- **The plaintext migration flow was built and then removed** in `169f037`, because the backend held no user data to migrate.
- **The security and OCR seams moved into `:core`** in `9b085f9`.
- **Avatars moved from Firebase Storage to S3-compatible storage**, which added five required build values. No OpenSpec change covered this.
- **Sign-out now clears local finance data.** The offline-first spec required the opposite. See [authentication.md](authentication.md).
- **Deleting transactions and accounts was added** in `e7624ad`. No OpenSpec change covered it.
