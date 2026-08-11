# Receipt fixture corpus

Fixtures for measuring `ReceiptParser` accuracy, required by the
`on-device-receipt-scanning` capability spec.

**The corpus is currently empty.** `ReceiptCorpusAccuracyTest` fails while it stays
that way. That failure is deliberate — see [Why an empty corpus fails](#why-an-empty-corpus-fails).

## What a fixture is

One `ReceiptFixture`: the OCR output for a single real receipt, plus the four
field values a correct parse must produce from it.

```
id           stable identifier, also the filename
sourceType   BANK_TRANSFER | QRIS | E_WALLET | RETAIL_THERMAL
synthetic    false for corpus fixtures
notes        capture notes: device, OS version, engine, anything unusual
ocr          serialized OcrResult exactly as the recognizer produced it
expected     amount, date, fee, category
```

`expected.amount` and `expected.fee` are whole rupiah with separators and any
currency prefix removed. `expected.date` is ISO 8601 carrying a timezone offset.
`expected.category` is a `TransactionCategory` enum name, or blank when the parser
should infer nothing.

## Privacy rule

**A fixture holds recognized text and geometry only. Never commit receipt image
data.**

Receipts are personal financial records. Capture the OCR output, verify it carries
no image bytes, and redact anything in the recognized text that identifies a real
person or account before committing — account numbers, cardholder names, phone
numbers. Redact in a way that preserves the token's shape and position, since the
parser's heuristics depend on layout.

## Capturing a fixture

1. Photograph a real receipt on a real device.
2. Run the platform recognizer over it — `MlKitOcrEngine` on Android,
   `VisionOcrEngine` on iOS — and serialize the resulting `OcrResult` to JSON.
   `OcrResult` is `@Serializable`, so `Json.encodeToString(result)` is sufficient.
3. Read the receipt yourself and record what a correct parse produces. This is the
   ground truth; do not copy it from the parser's current output, which would make
   the test assert that the parser agrees with itself.
4. Redact per the privacy rule above.
5. Create `<id>.kt` in this directory holding the JSON in a raw string constant.
6. Register the constant in `ReceiptFixtureRegistry.realFixtures`.

Fixtures live in Kotlin raw strings rather than resource files because `commonTest`
has no portable resource-loading API across Android, iOS, and the JVM host. The
JSON is still the format of record — it is what a capture emits and what a reviewer
reads.

## Corpus requirements

From the capability spec:

- Every `ReceiptSourceType` must be represented by at least one non-synthetic
  fixture. All four: bank transfer confirmations, QRIS slips, e-wallet receipts,
  retail thermal receipts.
- The corpus must meet the minimum size in `ReceiptAccuracyThresholds`.
- Per-field accuracy for amount and date must meet the recorded thresholds, and a
  regression below them fails the build.

Capture across several banks and merchants rather than many receipts from one
source. Ten receipts from one bank's app measure that bank's slip layout, not the
parser.

## Setting the thresholds

Thresholds in `ReceiptAccuracyThresholds` are placeholders until a real corpus
exists. Once it does:

1. Run the accuracy harness and record measured per-field accuracy.
2. Set each threshold at or slightly below the measured baseline, and note the
   measured value alongside it.

Do not set thresholds from aspiration. A threshold above what the parser achieves
fails permanently; one far below it never catches a regression.

## Synthetic fixtures

`SyntheticHarnessFixtures.kt` holds hand-written fixtures that exist **only** to
test this harness — that the loader parses JSON, the accuracy math is right, and
threshold breaches fail. They carry `synthetic = true` and
`ReceiptFixtureCorpus.accuracyCorpus` excludes them.

Never add a synthetic fixture to the accuracy corpus. Hand-written input measures
whether the parser agrees with whoever wrote the fixture, which is not accuracy.

## Why an empty corpus fails

An empty corpus divides zero correct parses by zero attempts. Reported naively that
is either a crash or a vacuous 100%, and a green test that proves nothing is worse
than a red one — it reads as "accuracy verified" to everyone downstream.

So `ReceiptCorpusAccuracyTest` fails explicitly while the corpus is empty, under
minimum size, missing a source type, or while `ParserUnderTest.parse` is unwired.
Each failure names what is missing. Delete none of these guards to get a green
build; populate the corpus instead.
