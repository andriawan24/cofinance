## 1. OCR abstraction seam

- [x] 1.1 Define the common OCR result model (`OcrResult`, `OcrBlock`, `OcrLine`, `OcrRect`) in `commonMain` with normalized 0..1 top-left-origin geometry, and an `expect` recognizer interface taking image bytes. Verify: `commonMain` compiles and the model is serializable for fixtures.
- [x] 1.2 Add the ML Kit Text Recognition v2 bundled dependency to the version catalog and `androidMain`. Verify: `:composeApp:assembleDebug` succeeds and the dependency resolves to the bundled variant, not the Play-services-delivered one.
- [ ] 1.3 Implement the Android `actual` recognizer over ML Kit, converting ML Kit geometry to normalized top-left-origin coordinates. Verify: an Android-side test recognizes a bundled sample receipt image and returns at least one line with coordinates within 0..1. — **Implementation done; verification blocked on a sample receipt photograph.** `MlKitOcrEngine` normalizes ML Kit's pixel boxes against the source image and clamps to 0..1; ML Kit's origin is already top-left so no axis is flipped, and the image is now decoded EXIF-upright so a portrait capture is not recognized sideways. The instrumented harness is in place and runs: `MlKitGeometryTest` passed 3/3 on a motorola edge 60 pro (Android 16) via `:core:connectedAndroidDeviceTest` — re-confirmed on 2026-08-24 after the OCR seam moved into `:core`, in a run of 41 whose only 3 failures are `MlKitSampleReceiptTest` naming the absent asset, pinning the 0..1 range, the top-left ordering, and the EXIF-rotated case. That test draws a plain canvas, not a receipt, so it does **not** discharge this task. `MlKitSampleReceiptTest` is the stated verification and fails naming the asset the user must supply at `core/src/androidDeviceTest/assets/sample_receipt.jpg` (git-ignored, see the README beside it). A rendered image was deliberately not substituted: it would show only that ML Kit reads clean rasterized type, not real thermal print.
- [ ] 1.4 Implement the iOS `actual` recognizer over `VNRecognizeTextRequest` with `recognitionLevel = .accurate` and `usesLanguageCorrection = false`, converting Vision's bottom-left-origin normalized geometry to the top-left-origin convention. Verify: an `iosSimulatorArm64` test recognizes the same sample image, returns at least one line within 0..1, and asserts that a token known to sit at the top of the image has a smaller `top` value than a token known to sit at the bottom. — **Implementation done; the stated verification cannot run in this repository.** `VisionOcrEngine` requests the accurate level with language correction off and flips the **Y axis** — Vision's origin is bottom-left with Y growing upward, so a box's maximum Y (its visual top) becomes `top = 1 - maxY` and its minimum Y becomes `bottom = 1 - minY`; X is unchanged. That conversion is now the top-level `visionBoxToTopLeftRect`, covered by `VisionCoordinateConversionTest` in `iosTest`, which asserts the top-token/bottom-token ordering the verify clause calls for. **Now verified.** The link blocker was removed by Decision 17 of the `end-to-end-encrypted-sync` change, which moved the OCR seam into the Firebase-free `:core` module; `:core:iosSimulatorArm64Test` runs. `VisionCoordinateConversionTest` passes 6/6, including the top-token-above-bottom-token ordering this task names. Running it for the first time also caught a wrong expectation in that very test: it asserted a centre of `0.4` for a box spanning Y 0.3..0.5, where Vision's upward Y axis makes the correct top-left-origin centre `0.6`. `visionBoxToTopLeftRect` itself was right; the assertion was corrected. The verification clause asks for the same sample image as 1.3, which does not exist yet, but the geometry contract it exists to protect is now machine-checked on a real Vision run. The task stays unchecked for the one clause still unmet: recognizing the shared sample receipt, which waits on the same photograph as 1.3.
- [x] 1.5 Register the recognizer in the Koin graph alongside the existing data source wiring. Verify: the graph resolves on both platforms without the Gemini model binding.

## 2. Fixture corpus

- [ ] 2.1 Assemble recognition fixtures from real Indonesian receipts, stored as serialized `OcrResult` JSON in `commonTest` resources with expected amount, date, fee, and category. Cover bank transfer confirmations, QRIS payment slips, e-wallet receipts, and retail thermal receipts. Verify: fixtures load in `commonTest`, and inspection confirms no receipt image data is checked in.
- [ ] 2.2 Record the minimum corpus size and the per-field accuracy thresholds for amount and date as named constants in the test suite, with the measured baseline noted alongside. Verify: thresholds are asserted, not merely documented, and the suite fails if a threshold constant is unset.

## 3. Receipt parser

- [x] 3.1 Implement Indonesian currency token normalization: `Rp`/`IDR` prefix, `.` thousands separator, `,` decimal separator, trailing `,00`. Verify: unit tests cover each form plus malformed input.
- [x] 3.2 Implement amount candidate scoring using total-keyword proximity, vertical position, magnitude, and negative signals for subtotal, tax, change, and cash-tendered lines. Verify: a fixture containing subtotal, tax, total, and cash-tendered lines resolves to the total value.
- [x] 3.3 Implement the date format matrix with the Indonesian month lexicon (full and abbreviated, alongside English forms), day-first resolution for ambiguous numeric dates, rejection of future-dated values, and ISO 8601 output carrying the Asia/Jakarta offset when the receipt supplies none. Verify: unit tests cover each format, the ambiguous case, the future-date rejection, and the timezone default.
- [x] 3.4 Implement fee extraction from the administration and transaction fee lexicon, defaulting to zero when absent. Verify: unit tests cover a fee-bearing and a fee-free fixture.
- [x] 3.5 Build the merchant, bank, and acquirer category dictionary for the Indonesian market and implement category inference, returning blank on no match. Verify: unit tests cover a matched merchant, an unmatched merchant, and confirm blank flows through `PreviewViewModel`'s existing `ifBlank { null }` handling.
- [x] 3.6 Implement per-field confidence, including a lower amount confidence when the top two candidates score comparably, and a blank date when no candidate meets the minimum threshold. Verify: unit tests cover the competing-candidate case and the no-confident-date case.
- [ ] 3.7 Assemble `ReceiptParser` and run it across the full fixture corpus, asserting the accuracy thresholds from task 2.2. Verify: the aggregate accuracy test passes and reports measured per-field accuracy.

## 4. Wire the on-device scanner into the existing flow

- [x] 4.1 Extend the `ReceiptScan` domain model with additive per-field confidence, defaulted so existing construction sites remain valid. Verify: existing call sites compile unchanged and existing tests pass.
- [x] 4.2 Implement the on-device `ReceiptScanner` composing the recognizer and the parser, replacing `GeminiDataSource` in the Koin graph. Verify: `ScanReceiptUseCase` and `TransactionRepository` compile without modification to their signatures.
- [x] 4.3 Remove the authenticated-session precondition from the scan path in `TransactionRepository`, leaving `SessionPolicy` in place for synchronization decisions. Verify: a `commonTest` with a signed-out session policy performs a scan and produces a draft, replacing the existing rejection test.
- [x] 4.4 Carry low-confidence field identifiers from `PreviewViewModel` into the draft navigation, and render those fields in a needs-verification state on the AddTransaction screen. Verify: a low-confidence amount is presented as needing verification, a fully confident scan presents none, and all four fields remain editable.
- [x] 4.5 Confirm the blank-date failure path still reports `error_receipt_scan_failed` and creates no draft. Verify: a test drives extraction returning a blank date and asserts no draft is written.

## 5. Merchant category learning

- [x] 5.1 Add device-local persistence for merchant-to-category associations, outside the entities handled by `FinanceSyncCoordinator`. Verify: a test confirms the store is not read by the sync path.
- [x] 5.2 Record an association when a user changes the category on a scanned draft, and apply stored associations during category inference ahead of the static dictionary. Verify: a test scans a merchant, corrects the category, rescans the same merchant, and asserts the corrected category is extracted.
- [x] 5.3 Confirm associations are excluded from cloud synchronization. Verify: a sync test asserts no association data is uploaded.

## 6. Remove the Gemini integration

- [x] 6.1 Delete `GeminiDataSource.kt` and `GeminiHelper.kt`, and remove their Koin bindings from `NetworkModule`. Verify: no source reference to `GenerativeModel`, `GeminiHelper`, or `generativeai` remains.
- [x] 6.2 Remove the `generativeai-google` entry from the version catalog and from `commonMain` dependencies. Verify: a dependency report confirms the module is absent from both platform configurations.
- [x] 6.3 Remove the `GEMINI_API_KEY` BuildKonfig field, its `local.properties` read, and its CI secret provisioning. Verify: a release-configuration build of each platform generates no Gemini field, and the CI workflow no longer references the secret.
- [x] 6.4 Update `README.md` and any setup documentation that instructs contributors to supply a Gemini key. Verify: no documentation references the removed key.

## 7. Verification and specification closure

- [ ] 7.1 Run the full `commonTest` suite plus the Android and iOS platform checks from tasks 1.3 and 1.4. Verify: all suites pass on both targets.
- [ ] 7.2 Perform a manual scan-to-draft pass on a real receipt with the device in airplane mode, signed out. Verify: the scan completes, a draft opens for review, and no network error appears.
- [ ] 7.3 Confirm no outbound request derives from the receipt image or its recognized text during a scan. Verify: a network trace or interceptor over a scan shows no such request.
- [x] 7.4 Run `openspec validate "on-device-receipt-scanning" --strict`. Verify: validation passes.
- [ ] 7.5 Synchronize the delta specs for `on-device-receipt-scanning`, `offline-first-access`, and `android-kmp-build` into `openspec/specs/`, then validate the main specs. Verify: main specs validate and the removed receipt-AI requirement no longer appears in `offline-first-access`.

## Status of unchecked tasks

Recorded 2026-08-06 after the implementation pass. 22 of 31 tasks complete.
Verification run: `testAndroidHostTest` — 58 tests, 54 pass, 4 fail (all four are
the deliberate empty-corpus guards described below). `compileAndroidMain` and
`compileTestKotlinIosSimulatorArm64` both succeed. `openspec validate --strict`
passes.

**Blocked on real receipt data — 2.1, 2.2, 3.7**

The corpus requires OCR captures of genuine Indonesian receipts, which were not
available. The fixture format, loader, accuracy harness, threshold constants, and
capture instructions are all in place under
`composeApp/src/commonTest/kotlin/id/andriawan/cofinance/data/ocr/fixtures/`.
Populating it is mechanical once real receipts exist — see the README there.

Fabricated fixtures were deliberately not written. Accuracy measured against
hand-authored input tests only whether the parser agrees with whoever wrote the
fixture, which would make the thresholds in 2.2 meaningless while appearing green.
`ReceiptCorpusAccuracyTest` therefore fails while the corpus is empty, under
minimum size, or missing a source type. Those four failures are the reminder, not
a regression.

**Blocked on a sample receipt photograph — 1.3**

Updated 2026-08-17. The instrumented harness now exists and runs on a real device;
what is missing is the receipt itself. Drop a photograph of a genuine Indonesian
receipt at `core/src/androidDeviceTest/assets/sample_receipt.jpg` and run
`./gradlew :composeApp:connectedAndroidDeviceTest`. The directory is git-ignored
under the same privacy rule as the fixture corpus: no receipt image data is
committed.

**Blocked on the iOS test binary not linking — 1.4**

Updated 2026-08-17. The Vision recognizer and its coordinate test both compile;
`iosSimulatorArm64Test` fails at link with `ld: framework 'FirebaseCore' not found`.
Same pre-existing cause as 7.1 below.

**Blocked on a physical device — 7.2, 7.3**

7.2 and 7.3 require an airplane-mode scan and a network trace on real hardware.

**Partially blocked — 7.1**

`commonTest` passes on the Android host target. The iOS test binary cannot link:
`ld: framework 'FirebaseCore' not found`. Firebase's native frameworks are supplied
by the Xcode project rather than the Gradle test target, so `iosSimulatorArm64Test`
has never been runnable in this repository. Confirmed pre-existing and out of scope
for this change.

**Deferred by sequence — 7.5**

Delta specs are synchronized at completion, not before the blocked tasks close.

## Out-of-scope fixes made during implementation

- `ReceiptScan.from()` never mapped `fee`, so it was always 0 and
  `PreviewViewModel`'s `if (data.fee > 0)` could never fire. Fixed under task 4.1
  with a regression test; the on-device parser extracts fee correctly and that
  mapping bug would have swallowed it.
- Adding the `merchant_categories` table bumped the Room schema to version 2, which
  armed the pre-existing `fallbackToDestructiveMigration(dropAllTables = true)` in
  `DatabaseBuilder.kt` for the first time. That would have dropped every local
  account and transaction on update — unrecoverable for offline-only users, who
  have no cloud copy. Added `autoMigrations = [AutoMigration(from = 1, to = 2)]`.
  The fallback itself remains armed for future bumps and is tracked separately.
- `DatabaseBuilder.ios.kt` failed to compile with a missing
  `@OptIn(ExperimentalForeignApi::class)`. Pre-existing and unrelated to this
  change, but it blocked all iOS verification, so it was fixed.
