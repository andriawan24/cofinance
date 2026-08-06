## Why

Receipt scanning currently uploads the user's receipt image to Google's Gemini API, and it authenticates that call with a `GEMINI_API_KEY` compiled into the shipped Android and iOS binaries. Both facts are incompatible with Cofinance's direction of being a privacy-preserving finance app where the operator cannot read user data, and the embedded key is extractable from any released artifact and chargeable against the project's quota. Moving extraction on-device removes the data egress and the key at the same time, and it is a prerequisite for honestly claiming end-to-end privacy once cloud sync is encrypted.

## What Changes

- Replace the Gemini-backed `ReceiptScanner` implementation with an on-device pipeline: platform OCR (ML Kit Text Recognition v2 on Android, Vision `VNRecognizeTextRequest` on iOS) followed by a shared Kotlin `ReceiptParser` in `commonMain`.
- Introduce an `expect`/`actual` OCR abstraction returning recognized text blocks with bounding boxes, so parsing logic stays in common code and is unit-testable without a device.
- Add an Indonesian-market receipt parser covering the four fields the app actually consumes today: total amount, transaction date, fee, and category. Parsing uses amount keyword and layout heuristics, an Indonesian date-format matrix, a fee-line lexicon, and a merchant/bank keyword dictionary for category inference.
- Emit per-field confidence from the parser and surface low-confidence fields on the existing draft review screen so the user is prompted to verify them.
- **BREAKING** Remove the `dev.shreyaspatil.generativeai` dependency, `GeminiDataSource`, `GeminiHelper`, and the `GEMINI_API_KEY` BuildKonfig field and its `local.properties`/CI secret wiring.
- **BREAKING** Remove the authenticated-session gate on receipt scanning. The gate existed to protect the operator's Gemini quota and to stop image egress; on-device scanning has neither cost nor egress, so scanning becomes available to signed-out local-only users, consistent with the app's offline-first stance.
- Establish a checked-in corpus of representative Indonesian receipt OCR fixtures with expected parse results, so parser accuracy is measured rather than assumed.

Non-goals for this change:

- No on-device or cloud LLM is introduced. Extraction is OCR plus deterministic rules.
- Encryption of synchronized data is out of scope and is handled by a separate change.
- The `ReceiptScanner` interface, `ScanReceiptUseCase`, `ReceiptScan` domain model, and the draft-then-review transaction flow are preserved. Sender, receiver, bank name, and transaction type remain unused by the draft flow and are not newly wired up here.

## Capabilities

### New Capabilities
- `on-device-receipt-scanning`: On-device optical extraction of transaction fields from a receipt image, covering the OCR abstraction, the shared Indonesian receipt parser, field confidence reporting, and the guarantee that no receipt image or derived text leaves the device.

### Modified Capabilities
- `offline-first-access`: The "Receipt AI requires an authenticated user" requirement is replaced. Receipt scanning no longer depends on a Firebase session, because there is no longer a remote call to gate.
- `android-kmp-build`: The build secret requirement no longer names Gemini configuration as a required secret input.

## Impact

Code:

- `composeApp/src/commonMain/.../data/datasource/GeminiDataSource.kt` — removed; replaced by an on-device scanner implementing the same `ReceiptScanner` interface.
- `composeApp/src/commonMain/.../utils/GeminiHelper.kt` — removed.
- `composeApp/src/commonMain/.../data/repository/TransactionRepository.kt` — scan path loses its session precondition.
- `composeApp/src/commonMain/.../di/NetworkModule.kt` — Gemini model provisioning replaced by OCR engine and parser provisioning.
- `composeApp/src/commonMain/.../pages/preview/PreviewViewModel.kt` — consumes parser confidence alongside the existing draft creation.
- `composeApp/src/commonMain/.../pages/addnew/` — draft review screen highlights low-confidence fields.
- New `commonMain` parser package plus `androidMain`/`iosMain` OCR actuals.
- New `commonTest` parser fixtures and accuracy tests.

Dependencies:

- Removed: `dev.shreyaspatil.generativeai:generativeai-google`.
- Added: ML Kit Text Recognition (Android). iOS uses the OS-provided Vision framework with no new dependency.

Configuration and secrets:

- `GEMINI_API_KEY` removed from BuildKonfig, `local.properties` expectations, and CI secret provisioning.

Behavioral:

- Extraction accuracy for amount, date, and fee is expected to be modestly lower than the cloud model; category inference is expected to be materially lower. All four fields are already user-editable on the draft review screen before a transaction is saved, which bounds the user-visible impact.
- Scanning gains offline capability and loses per-scan network latency.
