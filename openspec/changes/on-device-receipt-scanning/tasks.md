## 1. OCR abstraction seam

- [ ] 1.1 Define the common OCR result model (`OcrResult`, `OcrBlock`, `OcrLine`, `OcrRect`) in `commonMain` with normalized 0..1 top-left-origin geometry, and an `expect` recognizer interface taking image bytes. Verify: `commonMain` compiles and the model is serializable for fixtures.
- [ ] 1.2 Add the ML Kit Text Recognition v2 bundled dependency to the version catalog and `androidMain`. Verify: `:composeApp:assembleDebug` succeeds and the dependency resolves to the bundled variant, not the Play-services-delivered one.
- [ ] 1.3 Implement the Android `actual` recognizer over ML Kit, converting ML Kit geometry to normalized top-left-origin coordinates. Verify: an Android-side test recognizes a bundled sample receipt image and returns at least one line with coordinates within 0..1.
- [ ] 1.4 Implement the iOS `actual` recognizer over `VNRecognizeTextRequest` with `recognitionLevel = .accurate` and `usesLanguageCorrection = false`, converting Vision's bottom-left-origin normalized geometry to the top-left-origin convention. Verify: an `iosSimulatorArm64` test recognizes the same sample image, returns at least one line within 0..1, and asserts that a token known to sit at the top of the image has a smaller `top` value than a token known to sit at the bottom.
- [ ] 1.5 Register the recognizer in the Koin graph alongside the existing data source wiring. Verify: the graph resolves on both platforms without the Gemini model binding.

## 2. Fixture corpus

- [ ] 2.1 Assemble recognition fixtures from real Indonesian receipts, stored as serialized `OcrResult` JSON in `commonTest` resources with expected amount, date, fee, and category. Cover bank transfer confirmations, QRIS payment slips, e-wallet receipts, and retail thermal receipts. Verify: fixtures load in `commonTest`, and inspection confirms no receipt image data is checked in.
- [ ] 2.2 Record the minimum corpus size and the per-field accuracy thresholds for amount and date as named constants in the test suite, with the measured baseline noted alongside. Verify: thresholds are asserted, not merely documented, and the suite fails if a threshold constant is unset.

## 3. Receipt parser

- [ ] 3.1 Implement Indonesian currency token normalization: `Rp`/`IDR` prefix, `.` thousands separator, `,` decimal separator, trailing `,00`. Verify: unit tests cover each form plus malformed input.
- [ ] 3.2 Implement amount candidate scoring using total-keyword proximity, vertical position, magnitude, and negative signals for subtotal, tax, change, and cash-tendered lines. Verify: a fixture containing subtotal, tax, total, and cash-tendered lines resolves to the total value.
- [ ] 3.3 Implement the date format matrix with the Indonesian month lexicon (full and abbreviated, alongside English forms), day-first resolution for ambiguous numeric dates, rejection of future-dated values, and ISO 8601 output carrying the Asia/Jakarta offset when the receipt supplies none. Verify: unit tests cover each format, the ambiguous case, the future-date rejection, and the timezone default.
- [ ] 3.4 Implement fee extraction from the administration and transaction fee lexicon, defaulting to zero when absent. Verify: unit tests cover a fee-bearing and a fee-free fixture.
- [ ] 3.5 Build the merchant, bank, and acquirer category dictionary for the Indonesian market and implement category inference, returning blank on no match. Verify: unit tests cover a matched merchant, an unmatched merchant, and confirm blank flows through `PreviewViewModel`'s existing `ifBlank { null }` handling.
- [ ] 3.6 Implement per-field confidence, including a lower amount confidence when the top two candidates score comparably, and a blank date when no candidate meets the minimum threshold. Verify: unit tests cover the competing-candidate case and the no-confident-date case.
- [ ] 3.7 Assemble `ReceiptParser` and run it across the full fixture corpus, asserting the accuracy thresholds from task 2.2. Verify: the aggregate accuracy test passes and reports measured per-field accuracy.

## 4. Wire the on-device scanner into the existing flow

- [ ] 4.1 Extend the `ReceiptScan` domain model with additive per-field confidence, defaulted so existing construction sites remain valid. Verify: existing call sites compile unchanged and existing tests pass.
- [ ] 4.2 Implement the on-device `ReceiptScanner` composing the recognizer and the parser, replacing `GeminiDataSource` in the Koin graph. Verify: `ScanReceiptUseCase` and `TransactionRepository` compile without modification to their signatures.
- [ ] 4.3 Remove the authenticated-session precondition from the scan path in `TransactionRepository`, leaving `SessionPolicy` in place for synchronization decisions. Verify: a `commonTest` with a signed-out session policy performs a scan and produces a draft, replacing the existing rejection test.
- [ ] 4.4 Carry low-confidence field identifiers from `PreviewViewModel` into the draft navigation, and render those fields in a needs-verification state on the AddTransaction screen. Verify: a low-confidence amount is presented as needing verification, a fully confident scan presents none, and all four fields remain editable.
- [ ] 4.5 Confirm the blank-date failure path still reports `error_receipt_scan_failed` and creates no draft. Verify: a test drives extraction returning a blank date and asserts no draft is written.

## 5. Merchant category learning

- [ ] 5.1 Add device-local persistence for merchant-to-category associations, outside the entities handled by `FinanceSyncCoordinator`. Verify: a test confirms the store is not read by the sync path.
- [ ] 5.2 Record an association when a user changes the category on a scanned draft, and apply stored associations during category inference ahead of the static dictionary. Verify: a test scans a merchant, corrects the category, rescans the same merchant, and asserts the corrected category is extracted.
- [ ] 5.3 Confirm associations are excluded from cloud synchronization. Verify: a sync test asserts no association data is uploaded.

## 6. Remove the Gemini integration

- [ ] 6.1 Delete `GeminiDataSource.kt` and `GeminiHelper.kt`, and remove their Koin bindings from `NetworkModule`. Verify: no source reference to `GenerativeModel`, `GeminiHelper`, or `generativeai` remains.
- [ ] 6.2 Remove the `generativeai-google` entry from the version catalog and from `commonMain` dependencies. Verify: a dependency report confirms the module is absent from both platform configurations.
- [ ] 6.3 Remove the `GEMINI_API_KEY` BuildKonfig field, its `local.properties` read, and its CI secret provisioning. Verify: a release-configuration build of each platform generates no Gemini field, and the CI workflow no longer references the secret.
- [ ] 6.4 Update `README.md` and any setup documentation that instructs contributors to supply a Gemini key. Verify: no documentation references the removed key.

## 7. Verification and specification closure

- [ ] 7.1 Run the full `commonTest` suite plus the Android and iOS platform checks from tasks 1.3 and 1.4. Verify: all suites pass on both targets.
- [ ] 7.2 Perform a manual scan-to-draft pass on a real receipt with the device in airplane mode, signed out. Verify: the scan completes, a draft opens for review, and no network error appears.
- [ ] 7.3 Confirm no outbound request derives from the receipt image or its recognized text during a scan. Verify: a network trace or interceptor over a scan shows no such request.
- [ ] 7.4 Run `openspec validate "on-device-receipt-scanning" --strict`. Verify: validation passes.
- [ ] 7.5 Synchronize the delta specs for `on-device-receipt-scanning`, `offline-first-access`, and `android-kmp-build` into `openspec/specs/`, then validate the main specs. Verify: main specs validate and the removed receipt-AI requirement no longer appears in `offline-first-access`.
