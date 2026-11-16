# Receipt scanning

**Question:** How does a receipt photo become a draft transaction?

**Last verified:** 2026-11-16 against `ac66286`. Established by reading the source. Parser accuracy has never been measured against real receipts. Path shorthand is defined in [README.md](README.md).

## Summary

Scanning runs entirely on the device: platform OCR, then a deterministic parser in shared Kotlin. No receipt image, recognized text, or derived field leaves the device, and no API key is involved. It replaced a cloud model in August 2026.

The parser fills four fields — amount, date, fee, category — and reports a confidence for each. The result is a `DRAFT` transaction that opens in the normal add-transaction form, where every field stays editable. Extraction is therefore a pre-fill, not a commitment.

It is tuned for Indonesian receipts and assumes Indonesian number formatting and the Asia/Jakarta timezone.

## Pipeline

1. **Entry.** The Expense tab of the add-transaction screen has an upload button (`app/pages/addnew/sections/ExpenseSection.kt:65`). Income and Transfer do not. On Android, an image shared into the app from another app also starts the flow (`androidApp/src/main/AndroidManifest.xml:20-24`).
2. **Capture.** `CameraScreen` uses CameraK with the back lens, or the system photo picker (`app/pages/camera/CameraScreen.kt:17-46`).
3. **Preview.** The user confirms the image or retakes it.
4. **Compress.** The image is downscaled to 1024 px on its longest side and re-encoded as JPEG at quality 80 (`app/utils/Helper.kt:12`).
5. **Recognize.** `OcrEngine.recognize` returns text lines with bounding boxes.
6. **Parse.** `ReceiptParser` derives the four fields.
7. **Draft.** `PreviewViewModel` saves a `DRAFT` transaction and navigates to the add-transaction screen with the list of low-confidence fields (`app/pages/preview/PreviewViewModel.kt:66-113`).
8. **Review.** The form opens prefilled. Saving overwrites the draft with a real transaction.

The receipt image is deleted when the preview screen is left; it is never stored (`app/pages/preview/PreviewScreen.kt:69-71`).

## OCR

The engines live in `:core` behind `expect fun createOcrEngine()`.

```text
OcrResult(blocks) → OcrBlock(text, lines) → OcrLine(text, boundingBox, confidence?)
```

Bounding boxes are normalized to 0..1 with a top-left origin (`core/ocr/OcrResult.kt:5-47`). Geometry is essential rather than incidental: the strongest signal for a receipt total is positional. Normalizing at the platform boundary keeps the parser independent of image resolution.

| | Android | iOS |
|---|---------|-----|
| Engine | ML Kit Text Recognition v2, Latin, bundled model | Vision `VNRecognizeTextRequest`, accurate level |
| Notable | Decodes upright from EXIF orientation | Language correction is off; flips Vision's bottom-left Y axis |
| Source | `core-android/ocr/MlKitOcrEngine.kt:19-111` | `core-ios/ocr/VisionOcrEngine.kt:43-161` |

The bundled ML Kit model was chosen over the Play-services-delivered one, so a first scan cannot fail on a device that has not downloaded the model, at a cost of roughly 4 MB. Vision's language correction is disabled because it improves prose and damages receipt tokens such as amounts and account numbers.

## Parsing

All in `app/data/ocr/parser/`.

### Amount

Every currency token is a candidate, scored on three things (`ReceiptParser.kt:73-131`):

- **Keyword:** a total keyword on the same line, or a qualifying label on the line above.
- **Position:** the lower half of the receipt scores higher.
- **Magnitude:** relative to the largest value on the receipt.

Lines with subtotal, tax, change, cash-tendered, or fee keywords are penalized. Confidence comes from the margin between the best and second-best candidates.

### Date

Formats are tried in priority order: `dd/mm/yyyy`, `dd-mm-yyyy`, `yyyy-mm-dd`, and day, month name, year, each with an optional time (`ReceiptDateParser.kt:30-109`).

- Month names are matched in Indonesian and English, including abbreviations such as `Agu`, `Ags`, `Okt`, and `Des`.
- Ambiguous numeric dates are read day-first, the Indonesian convention.
- Future dates are rejected.
- Output is ISO 8601 with a fixed `+07:00` offset.
- Two different dates on one receipt cap the confidence.

### Fee

The first line with a fee keyword — `BIAYA ADMIN`, `ADMIN`, `FEE`, and similar — contributes its amount. With no match the fee is zero, which is the common case.

### Category

Checked in order (`ReceiptParser.kt:145-172`):

1. A learned association for this merchant.
2. The merchant dictionary: about 190 Indonesian merchant and brand keywords across ten categories.
3. The acquirer dictionary: about 50 banks and e-wallets, mapped to the administration category.

With no match the category is left blank for the user to choose.

### Currency format

Indonesian formatting only: an optional `Rp` or `IDR` prefix, `.` as the thousands separator, `,` as the decimal separator (`CurrencyNormalizer.kt:3-65`). Fractions are truncated. An unmarked plain number counts only between 100 and 9,999,999.

## Confidence

A field below 0.6 is low-confidence (`ReceiptParser.kt:180`). On the review form, a low-confidence field gets an outline and a "please double-check" hint. It never blocks saving (`app/components/NeedsVerification.kt:15-40`).

A date below 0.5 is reported blank rather than guessed, and **a blank date fails the whole scan**: an error is shown and no draft is created (`app/pages/preview/PreviewViewModel.kt:58-64`).

## Learning from corrections

When the user changes the category on a scanned draft and saves, the association between that merchant and the chosen category is stored, and it takes priority on the next scan of the same merchant (`MerchantCategoryLearning.kt:23-40`).

The merchant key is the first line in the top 40% of the receipt with at least three letters, normalized to its first three tokens. Associations live in the Room table `merchant_categories` and are never synchronized.

## Decisions and why

1. **Platform OCR behind a thin seam, parsing in common code.** No credible multiplatform OCR exists. Parsing inside each platform actual would duplicate the parser and put it out of reach of common tests.
2. **Deterministic rules, not a model.** A bundled receipt-understanding model was rejected on size, in the hundreds of megabytes. A category-only classifier was rejected as premature before the dictionary's real miss rate is known.
3. **Four fields.** These are the four the draft flow consumes. Free-form counterparty names are the hardest to extract with rules and had no consumer.
4. **Blank instead of a guess.** A parser that is right most of the time is acceptable because the review form already exists; a parser that silently commits wrong data would not be.
5. **Asia/Jakarta when the receipt gives no timezone.** Correct for an Indonesia-only user base, and the date remains editable.

## Open

- **Accuracy has never been measured.** A fixture corpus of real Indonesian receipts — bank transfers, QRIS slips, e-wallet receipts, retail thermal receipts — was planned, with per-field thresholds. It was never assembled, because no real receipts were available and fabricated fixtures would only test whether the parser agrees with whoever wrote them. The harness that waited for it was removed with the test suite.
- **The three manual checks were never done:** an end-to-end scan on a real receipt in airplane mode while signed out, a network trace confirming nothing leaves the device, and recognition of a real receipt photo on each platform.
- **Scanning still requires sign-in.** Navigation redirects a signed-out user to Login before the camera or a shared image (`app/navigations/MainNavigation.kt:120-126, 226-231`). The reason for the gate — image egress to a cloud model — no longer exists. See [authentication.md](authentication.md).
- **Android rotation fix may not apply.** Inferred, not tested. Compression re-encodes through `Bitmap.compress`, which writes no EXIF, while the ML Kit engine relies on EXIF to turn the image upright. A portrait capture may reach OCR sideways.
- **A blank date discards a good amount.** The scan fails outright even when the amount was read confidently.
- **The loading indicator can stick.** `scanReceipt` turns loading on and then returns early if the file could not be read (`app/pages/preview/PreviewViewModel.kt:51-53`).
- **A scanned draft always opens as an expense.** There is no transfer or income detection.
- **Unused fields.** `ReceiptScan` still carries `bankName`, `transactionType`, `sender`, and `receiver` from the cloud model's response. The parser never fills them and nothing reads them.
- **English-format amounts are not handled.** Read from the patterns, not tested: `1,234.56` and `12.50` are both rejected.
- **iOS share-to-app is half-wired.** `ContentView` parses a `cofinance://share` URL, but the scheme is not declared in `Info.plist`, and a URL arriving after first composition never reaches Compose.
- **Should bank name and transaction type auto-select the sender account?** The parser could populate them cheaply; wiring a consumer is a separate product decision.
- Learned merchant associations are never cleared, including at sign-out.
