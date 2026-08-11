package id.andriawan.cofinance.data.ocr.fixtures

/*
 * ============================================================================
 * SYNTHETIC HARNESS FIXTURES — NOT CORPUS DATA. DO NOT MEASURE ACCURACY ON THESE.
 * ============================================================================
 *
 * These three documents were hand-written. No OCR engine produced them and no
 * real receipt exists behind them. Their only job is to prove that the fixture
 * loader parses, that the accuracy arithmetic in `ReceiptAccuracyHarness` is
 * correct, and that an under-strength corpus fails loudly.
 *
 * Every one of them carries `"synthetic": true`, which excludes it from
 * `ReceiptFixtureCorpus.accuracyCorpus`. Do not flip that flag, do not copy
 * these as a template for real captures, and do not let them stand in for
 * corpus coverage of a source type.
 *
 * Real fixtures are captured by running platform OCR over a real receipt — see
 * README.md in this directory.
 */

internal const val SYNTHETIC_HARNESS_FIXTURE_CLEAN: String = """
{
  "id": "synthetic-harness-clean",
  "sourceType": "RETAIL_THERMAL",
  "synthetic": true,
  "notes": "SYNTHETIC. Hand-written. Exercises the happy path: every field present and unambiguous.",
  "ocr": {
    "blocks": [
      {
        "text": "SYNTHETIC HARNESS RECEIPT",
        "lines": [
          {
            "text": "SYNTHETIC HARNESS RECEIPT",
            "boundingBox": { "left": 0.1, "top": 0.05, "right": 0.9, "bottom": 0.1 }
          },
          {
            "text": "05/03/2026 10:22",
            "boundingBox": { "left": 0.1, "top": 0.12, "right": 0.5, "bottom": 0.17 }
          }
        ]
      },
      {
        "text": "TOTAL Rp150.000",
        "lines": [
          {
            "text": "SUBTOTAL Rp150.000",
            "boundingBox": { "left": 0.1, "top": 0.7, "right": 0.9, "bottom": 0.75 }
          },
          {
            "text": "TOTAL Rp150.000",
            "boundingBox": { "left": 0.1, "top": 0.8, "right": 0.9, "bottom": 0.85 }
          }
        ]
      }
    ]
  },
  "expected": {
    "amount": 150000,
    "date": "2026-03-05T10:22:00+07:00",
    "fee": 0,
    "category": "OTHERS"
  }
}
"""

internal const val SYNTHETIC_HARNESS_FIXTURE_FEE_BEARING: String = """
{
  "id": "synthetic-harness-fee-bearing",
  "sourceType": "BANK_TRANSFER",
  "synthetic": true,
  "notes": "SYNTHETIC. Hand-written. Exercises a non-zero fee field in the accuracy arithmetic.",
  "ocr": {
    "blocks": [
      {
        "text": "SYNTHETIC HARNESS TRANSFER",
        "lines": [
          {
            "text": "SYNTHETIC HARNESS TRANSFER",
            "boundingBox": { "left": 0.1, "top": 0.05, "right": 0.9, "bottom": 0.1 }
          },
          {
            "text": "12 Agustus 2025",
            "boundingBox": { "left": 0.1, "top": 0.14, "right": 0.6, "bottom": 0.19 }
          },
          {
            "text": "BIAYA ADMIN Rp2.500",
            "boundingBox": { "left": 0.1, "top": 0.6, "right": 0.9, "bottom": 0.65 }
          },
          {
            "text": "TOTAL Rp502.500",
            "boundingBox": { "left": 0.1, "top": 0.72, "right": 0.9, "bottom": 0.78 }
          }
        ]
      }
    ]
  },
  "expected": {
    "amount": 502500,
    "date": "2025-08-12T00:00:00+07:00",
    "fee": 2500,
    "category": "ADMINISTRATION"
  }
}
"""

internal const val SYNTHETIC_HARNESS_FIXTURE_NO_CATEGORY: String = """
{
  "id": "synthetic-harness-no-category",
  "sourceType": "QRIS",
  "synthetic": true,
  "notes": "SYNTHETIC. Hand-written. Exercises blank expected category and blank expected date.",
  "ocr": {
    "blocks": [
      {
        "text": "SYNTHETIC HARNESS QRIS",
        "lines": [
          {
            "text": "SYNTHETIC HARNESS QRIS",
            "boundingBox": { "left": 0.1, "top": 0.05, "right": 0.9, "bottom": 0.1 }
          },
          {
            "text": "NOMINAL Rp37.000",
            "boundingBox": { "left": 0.1, "top": 0.66, "right": 0.9, "bottom": 0.72 }
          }
        ]
      }
    ]
  },
  "expected": {
    "amount": 37000,
    "date": "",
    "fee": 0,
    "category": ""
  }
}
"""
