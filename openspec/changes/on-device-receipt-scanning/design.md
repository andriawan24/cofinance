## Context

Today's scan path is: `PreviewScreen` → `PreviewViewModel.scanReceipt` → `ScanReceiptUseCase` → `TransactionRepository.scanReceipt` → `GeminiDataSource` → Gemini `gemini-3-flash-preview` over the network. The response is deserialized into `ReceiptScanResponse`, mapped to the `ReceiptScan` domain model, and a `TransactionType.DRAFT` transaction is created and opened in the AddTransaction screen for editing.

Two properties of the current code shape this design.

First, the draft review step already exists. `PreviewViewModel` writes a DRAFT and navigates to `AddTransactionViewModel.loadExistingTransaction`, which prefills amount, date, category, fee, notes, and accounts as fully editable fields. Extraction output is therefore a pre-fill, not a commitment, and a parser that is right most of the time is acceptable where a parser that silently commits wrong data would not be.

Second, only four of the eight extracted fields are consumed. `PreviewViewModel` builds `AddTransactionParam` from `totalPrice`, `transactionDate`, `category`, and `fee`. `bankName`, `transactionType`, `sender`, and `receiver` are parsed by Gemini and discarded. The fields that are hardest to extract with deterministic rules — free-form counterparty names — are exactly the fields with no consumer, so the accuracy risk of leaving the cloud model is much smaller than the field count suggests.

Constraints: Android and iOS are the only targets. Android `minSdk` is 24. The app is offline-first, so the scan path must not assume connectivity. `GEMINI_API_KEY` currently flows from `local.properties` through BuildKonfig into the binary, and CI provisions it as a secret; both paths are removed here.

## Goals / Non-Goals

**Goals:**

- No receipt image, OCR text, or derived field ever leaves the device during scanning.
- No API key or remote credential is required for scanning, and none remains in the shipped binary.
- Extraction logic for amount, date, fee, and category lives in `commonMain` and is unit-testable on the JVM without a device, emulator, or camera.
- Platform OCR is confined to a thin `expect`/`actual` seam.
- The parser reports per-field confidence so the review screen can direct the user's attention.
- Scanning works while signed out and while offline.
- Accuracy is measured against a checked-in fixture corpus rather than asserted.

**Non-Goals:**

- No on-device or cloud LLM. If a future change adds Apple Foundation Models or Gemini Nano, it does so as an optional post-processor over OCR text, not as a base requirement.
- No newly wired consumers for `bankName`, `transactionType`, `sender`, or `receiver`. They remain in the `ReceiptScan` model, populated on a best-effort basis or left blank.
- No change to the draft-then-review transaction flow, the `ReceiptScanner` interface, `ScanReceiptUseCase`, or the `ReceiptScan` domain model shape beyond additive confidence data.
- No encryption of synchronized data. Separate change.
- No non-Latin script support and no locale coverage beyond Indonesian receipts.

## Decisions

### Decision 1: Platform OCR behind an `expect`/`actual` seam, parsing in common code

The OCR abstraction returns a structured recognition result, not a flat string:

```
OcrResult(blocks: List<OcrBlock>)
OcrBlock(text: String, lines: List<OcrLine>)
OcrLine(text: String, boundingBox: OcrRect, confidence: Float?)
OcrRect(left, top, right, bottom)   // normalized 0..1 against image dimensions
```

Bounding boxes are essential. The dominant heuristic for a receipt total is positional — the largest currency value in the lower portion of the receipt, on or adjacent to a line containing a total keyword. A flat text dump discards the geometry that makes this reliable. Normalizing coordinates to 0..1 at the platform boundary keeps the common parser independent of image resolution and orientation, and makes fixtures resolution-independent.

- Android actual: ML Kit Text Recognition v2 (`com.google.mlkit:text-recognition`). On-device, free, no network. Bundled variant is chosen over the Play-services-delivered variant so a first scan cannot fail on a device that has not yet downloaded the model, at the cost of roughly 4 MB of APK size. Offline-first is a product property here, not just a size trade.
- iOS actual: `VNRecognizeTextRequest` with `recognitionLevel = .accurate` and `usesLanguageCorrection = false`. Language correction is disabled deliberately: it improves prose and harms receipt tokens such as account numbers and amount digits. Vision reports origin at bottom-left in a normalized space; the actual converts to the top-left-origin convention above so the common parser sees one coordinate system.

Alternatives considered. A pure-Kotlin OCR implementation was rejected: no credible multiplatform OCR exists and receipt-quality thermal print demands a mature engine. Tesseract via cinterop was rejected as materially worse than both platform engines on thermal receipts while adding build complexity on two platforms. Doing extraction inside each platform actual was rejected because it would duplicate the parser twice and make it untestable in `commonTest`.

### Decision 2: Deterministic parser, four target fields, Indonesian-tuned

`ReceiptParser` in `commonMain` takes `OcrResult` and returns `ReceiptScan` plus per-field confidence. Extraction strategies:

**Amount.** Candidate currency tokens are collected by normalizing Indonesian formatting: an optional `Rp`/`IDR` prefix, `.` as the thousands separator, `,` as the decimal separator, and trailing `,00`. Candidates are scored by (a) presence of a total keyword on the same line or the line immediately above — `TOTAL`, `JUMLAH`, `TOTAL BAYAR`, `TOTAL BELANJA`, `NOMINAL`, `GRAND TOTAL`, (b) vertical position, favoring the lower half, (c) magnitude, since the total is usually the largest value on the receipt, and (d) a negative signal for lines matching subtotal, tax, change, or cash-tendered keywords — `SUBTOTAL`, `PPN`, `KEMBALI`, `TUNAI`, `BAYAR TUNAI`. Highest score wins; confidence is derived from the margin between the top two candidates.

**Date.** A format matrix is tried in priority order against every line: `dd/MM/yyyy`, `dd-MM-yyyy`, `yyyy-MM-dd`, `dd MMM yyyy`, `dd MMMM yyyy`, each with an optional time component `HH:mm` or `HH:mm:ss`. Month-name matching uses an Indonesian month lexicon covering both full and abbreviated forms (`Januari`/`Jan`, `Agustus`/`Agu`/`Ags`, `Oktober`/`Okt`, `Desember`/`Des`) alongside English forms, since Indonesian receipts mix both. Output is ISO 8601 with a timezone, matching what the current Gemini schema produces and what `AddTransactionParam` expects. When the receipt carries no timezone, Asia/Jakarta (UTC+7) is assumed, which is correct for the stated Indonesia-only user base and is recorded as a parser assumption. Ambiguous `dd/MM` versus `MM/dd` is resolved to `dd/MM` — the Indonesian convention — and any value that would place the transaction in the future is rejected and lowers confidence.

**Fee.** Lines matching a fee lexicon — `BIAYA ADMIN`, `BIAYA TRANSAKSI`, `BIAYA LAYANAN`, `ADMIN`, `FEE` — contribute their currency token. Absent a match, fee is zero, which is the common case and matches current behavior where `PreviewViewModel` only sets fee when greater than zero.

**Category.** A keyword dictionary maps merchant and acquirer tokens to the existing `TransactionCategory` enum values. Because the user base is Indonesia-only, the dictionary is tractable: roughly thirty banks and twenty e-wallet and QRIS acquirers plus common merchant chains. On no match the category is left blank and `PreviewViewModel`'s existing `ifBlank { null }` handling applies, leaving the user to pick on the review screen. A locally persisted merchant-to-category map records user corrections so repeat merchants improve over time; that map is device-local and is not synchronized.

Alternatives considered. A bundled receipt-understanding model such as a Donut-style transformer was rejected on bundle size, in the hundreds of megabytes. Shipping a small on-device classifier for category only was rejected as premature before the keyword dictionary's real miss rate is measured against the fixture corpus.

### Decision 3: Confidence is additive on the domain model

`ReceiptScan` gains a confidence map keyed by field, defaulted such that existing construction sites remain valid. `PreviewViewModel` carries low-confidence field identifiers into the draft navigation, and the AddTransaction screen renders those fields in a needs-verification state. This keeps the parser honest about uncertainty instead of forcing a false binary between a confident answer and a total scan failure.

The existing hard-failure condition is preserved and narrowed: `PreviewViewModel` already aborts with `error_receipt_scan_failed` when `transactionDate` is blank. The parser returns a blank date rather than a guessed one when no candidate clears a minimum confidence threshold, so that failure path continues to mean the same thing.

### Decision 4: Session gate removal

`offline-first-access` currently requires an authenticated session for scanning, with the stated rationale that the repository must reject a scan "before sending image data to Gemini." That rationale is void once nothing is sent. Keeping the gate would make an offline-capable, zero-cost, fully local feature depend on a cloud account, which contradicts the offline-first requirement in the same spec. The gate is therefore removed from `TransactionRepository` and the corresponding requirement is replaced.

`SessionPolicy` remains injected and used for synchronization decisions; only the scan precondition is dropped.

### Decision 5: Verification evidence per platform

Because the parser is in `commonMain` and OCR is at the boundary, most verification is device-free:

- `commonTest` parser tests run against checked-in `OcrResult` fixtures serialized as JSON, derived from real Indonesian receipts. Fixtures store normalized geometry and expected amount, date, fee, and category. An aggregate accuracy assertion guards against regression: amount and date must meet a stated per-field threshold across the corpus, and the thresholds are recorded in the test so a drop is a visible failure rather than a silent one.
- Android: an instrumented or host-side check that the ML Kit actual produces a non-empty `OcrResult` for a bundled sample image, plus a release-configuration build confirming no `GEMINI_API_KEY` field remains in generated BuildKonfig.
- iOS: an `iosSimulatorArm64` check that the Vision actual produces a non-empty `OcrResult` for the same sample image and that its coordinate conversion matches the top-left-origin convention, verified by asserting a known token's expected quadrant.
- Both: a manual scan-to-draft pass on a real receipt in airplane mode, confirming the flow completes with no network.

## Risks / Trade-offs

- **Amount extraction is wrong on an unusual receipt layout and the user does not notice before saving.** → The draft review screen already requires the user to pass through an editable form before the transaction is saved. Low-confidence amounts are additionally flagged. Amount is the field with the highest fixture-corpus threshold.
- **Category accuracy drops materially, degrading the perceived quality of scanning.** → Category is the most forgiving field: it is a single tap to correct on a screen the user already visits, blank is an accepted value today, and the locally learned merchant map recovers accuracy for repeat merchants. If measured accuracy is unacceptable, a category-only on-device classifier can be added later without touching the OCR seam.
- **The fixture corpus is too small or unrepresentative, so measured accuracy is optimistic.** → Corpus composition is a task deliverable with a stated minimum count and a requirement to span the major receipt sources in the Indonesian market: bank transfer confirmations, QRIS payment slips, e-wallet receipts, and retail thermal receipts. Fixtures are text-only OCR output, so they can be checked in without storing anyone's receipt images.
- **Bundling the ML Kit model adds roughly 4 MB to the Android artifact.** → Accepted deliberately in exchange for a scan path that never depends on a model download. Revisit only if artifact size becomes a stated constraint.
- **Vision and ML Kit disagree enough that one platform's accuracy is materially worse.** → The fixture corpus is engine-agnostic because fixtures are `OcrResult`, but a per-platform smoke comparison on the same sample images is part of verification, so a large divergence surfaces before release rather than in the field.
- **Removing the session gate exposes scanning to signed-out users, which may not be intended product-wise.** → This is a deliberate, called-out behavior change in the proposal rather than a silent side effect. If it is unwanted, the gate can be retained as a product decision, but then it needs a rationale other than the now-removed data egress.
- **Timezone assumption is wrong for a user travelling or a receipt from another timezone.** → Asia/Jakarta is assumed only when the receipt carries no timezone, matching the stated Indonesia-only user base. The date field is editable on the review screen, and the assumption is documented in the parser rather than buried.

## Migration Plan

No data migration. No stored data changes shape.

Removal sequence matters for secret hygiene: the BuildKonfig field, `local.properties` expectation, and CI secret are removed in the same change that removes the last code reference, so no build configuration is left referencing a field that no longer exists and no key is left provisioned to a build that no longer needs it. The Gemini API key itself should be revoked in the Google Cloud console after release, since previously shipped binaries still contain it; that is an operator action outside this repository and is noted as a follow-up rather than a task.

Rollback is a revert of the change. Because the `ReceiptScanner` interface is unchanged, no caller is affected by moving between implementations.

## Open Questions

- ~~Do DRAFT transactions reach Firestore through `FinanceSyncCoordinator.mirrorAllIfSignedIn`?~~ Resolved: yes. `LocalFinanceDatabase.getAllTransactions` applies no type filter, so DRAFT and CYCLE_RESET rows are uploaded alongside real transactions. This does not affect this change; it places drafts inside the scope of the separate encryption change, where it is now specified.
- What minimum fixture-corpus size and per-field accuracy thresholds are acceptable to the project? The design requires that thresholds exist and be asserted; the specific numbers are set when the corpus is assembled and the first measurements are available.
- Should `bankName` and `transactionType` be carried into the draft to auto-select the sender account? The parser can populate them at low marginal cost, but wiring a consumer is a separate product change and is explicitly out of scope here.
