# Android instrumented OCR checks

These tests exercise `MlKitOcrEngine` on a real Android runtime. They cannot run on the
JVM host target: ML Kit text recognition loads native libraries, so `testAndroidHostTest`
is not an option and Robolectric does not help.

```bash
./gradlew :core:connectedAndroidDeviceTest    # device or emulator required
```

## Which test needs what

| Test | Needs a real receipt | Runs today |
|---|---|---|
| `MlKitGeometryTest` | no | yes |
| `MlKitSampleReceiptTest` | yes | only once the asset below exists |

`MlKitGeometryTest` renders a plain canvas with one token near the top edge and one near
the bottom, then pins the geometry contract: every edge normalized into 0..1, top-left
origin, `top` smaller for content nearer the top, and the same ordering after an
EXIF-rotated capture. That is machine-checkable without anyone's receipt — but it proves
only the coordinate conversion, not that ML Kit reads thermal print.

## Supplying the sample receipt

`MlKitSampleReceiptTest` needs a photograph that is **not committed**:

```
core/src/androidDeviceTest/assets/sample_receipt.jpg
```

Requirements:

- A real Indonesian receipt — a retail thermal receipt, QRIS slip, e-wallet receipt, or
  bank transfer confirmation.
- Photographed on a phone, upright, whole receipt in frame, in focus.
- Do not substitute a rendered or synthesized image. It would show only that ML Kit reads
  cleanly rasterized type, which is not what this test claims.

The `assets/` directory is git-ignored for this reason. Receipts are personal financial
records; the same rule governs the OCR fixture corpus, which stores recognized text only
and never image bytes — see
`composeApp/src/commonTest/kotlin/id/andriawan/cofinance/data/ocr/fixtures/README.md`.

While the asset is absent the test fails with a message naming the path. That is
deliberate: a skipped OCR check reads as "verified" downstream.
