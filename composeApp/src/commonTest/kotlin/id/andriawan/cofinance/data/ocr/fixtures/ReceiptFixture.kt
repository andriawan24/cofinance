package id.andriawan.cofinance.data.ocr.fixtures

import id.andriawan.cofinance.data.ocr.OcrResult
import kotlinx.serialization.Serializable

/**
 * One accuracy-corpus record: the OCR output for a single receipt plus the
 * fields a correct parse must produce from it.
 *
 * A fixture holds recognized **text and geometry only**. Receipt image data must
 * never be checked in — see `README.md` in this directory for the capture
 * procedure and the privacy rule.
 */
@Serializable
data class ReceiptFixture(
    /** Stable, human-readable identifier. Also the name of the file holding it. */
    val id: String,
    /** Which of the four Indonesian receipt sources this fixture came from. */
    val sourceType: ReceiptSourceType,
    /**
     * True only for fixtures that exist to exercise this test harness itself.
     * Synthetic fixtures are excluded from every accuracy assertion.
     */
    val synthetic: Boolean = false,
    /** Free-form capture notes: device, engine, anything unusual about the receipt. */
    val notes: String = "",
    /** Serialized platform OCR output, exactly as the recognizer produced it. */
    val ocr: OcrResult = OcrResult(),
    /** The parse a correct implementation must produce from [ocr]. */
    val expected: ExpectedParse = ExpectedParse()
)

/**
 * Expected parse result for a fixture. Mirrors the four fields the transaction
 * draft actually consumes.
 */
@Serializable
data class ExpectedParse(
    /** Total amount in whole rupiah, separators and currency prefix removed. */
    val amount: Long = 0,
    /** ISO 8601 date-time carrying a timezone offset, or blank if the receipt has no readable date. */
    val date: String = "",
    /** Administration or transaction fee in whole rupiah. Zero when the receipt has no fee line. */
    val fee: Long = 0,
    /** `TransactionCategory` enum name, or blank when no category should be inferred. */
    val category: String = ""
)

/**
 * The receipt sources the corpus is required to span. Every value here must be
 * represented by at least one non-synthetic fixture.
 */
@Serializable
enum class ReceiptSourceType {
    /** Bank transfer confirmation slip (BCA, Mandiri, BNI, BRI, ...). */
    BANK_TRANSFER,

    /** QRIS payment slip. */
    QRIS,

    /** E-wallet receipt (GoPay, OVO, DANA, ShopeePay, LinkAja, ...). */
    E_WALLET,

    /** Retail thermal receipt (minimarket, supermarket, restaurant). */
    RETAIL_THERMAL
}
