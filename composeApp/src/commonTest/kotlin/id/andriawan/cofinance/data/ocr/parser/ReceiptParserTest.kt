package id.andriawan.cofinance.data.ocr.parser

import id.andriawan.cofinance.data.ocr.OcrBlock
import id.andriawan.cofinance.data.ocr.OcrLine
import id.andriawan.cofinance.data.ocr.OcrRect
import id.andriawan.cofinance.data.ocr.OcrResult
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.utils.enums.TransactionCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ReceiptParserTest {

    private val parser = ReceiptParser(FixedClock(NOW))

    @Test
    fun selectsTotalOverSubtotalTaxAndCashTendered() {
        val parsed = parser.parse(
            receipt(
                "INDOMARET SUNTER" at 0.05f,
                "SUBTOTAL 90.000" at 0.55f,
                "PPN 10.000" at 0.60f,
                "TOTAL 100.000" at 0.65f,
                "TUNAI 150.000" at 0.70f,
                "KEMBALI 50.000" at 0.75f,
                "05/03/2026 10:00" at 0.85f
            )
        )

        assertEquals(100_000L, parsed.response.totalPrice)
    }

    @Test
    fun selectsValueBelowTotalLabelInTwoColumnLayout() {
        val parsed = parser.parse(
            OcrResult(
                listOf(
                    OcrBlock(
                        lines = listOf(
                            line("SUBTOTAL", centerY = 0.60f, left = 0.10f),
                            line("Rp140.000", centerY = 0.60f, left = 0.70f),
                            line("TOTAL", centerY = 0.70f, left = 0.10f),
                            line("Rp150.000", centerY = 0.70f, left = 0.70f)
                        )
                    )
                )
            )
        )

        assertEquals(150_000L, parsed.response.totalPrice)
    }

    @Test
    fun normalizesIndonesianCurrencyFormattingOnTheTotal() {
        val parsed = parser.parse(
            receipt(
                "TOTAL BAYAR Rp1.250.000,00" at 0.70f,
                "12 Maret 2026" at 0.90f
            )
        )

        assertEquals(1_250_000L, parsed.response.totalPrice)
    }

    @Test
    fun readsEachSupportedDateFormat() {
        val expected = "2026-03-05T00:00:00+07:00"

        assertEquals(expected, dateOf("05/03/2026"))
        assertEquals(expected, dateOf("05-03-2026"))
        assertEquals(expected, dateOf("2026-03-05"))
        assertEquals(expected, dateOf("05 Mar 2026"))
        assertEquals(expected, dateOf("5 Maret 2026"))
    }

    @Test
    fun readsIndonesianAbbreviatedAndFullMonthNames() {
        assertEquals("2025-12-12T14:30:00+07:00", dateOf("12 Desember 2025 14:30"))
        assertEquals("2025-08-17T00:00:00+07:00", dateOf("17 Agu 2025"))
        assertEquals("2025-10-01T00:00:00+07:00", dateOf("01 Okt 2025"))
    }

    @Test
    fun readsOptionalTimeComponents() {
        assertEquals("2026-03-05T09:15:00+07:00", dateOf("05/03/2026 09:15"))
        assertEquals("2026-03-05T09:15:30+07:00", dateOf("05/03/2026 09:15:30"))
    }

    @Test
    fun resolvesAmbiguousNumericDatesDayFirst() {
        assertEquals("2026-03-05T00:00:00+07:00", dateOf("05/03/2026"))
    }

    @Test
    fun defaultsToJakartaOffsetWhenReceiptCarriesNoTimezone() {
        assertTrue(dateOf("05/03/2026")?.endsWith("+07:00") == true)
    }

    @Test
    fun rejectsFutureDates() {
        val parsed = parser.parse(receipt("TOTAL 50.000" at 0.7f, "05/03/2027" at 0.9f))

        assertNull(parsed.response.transactionDate)
        assertEquals(0f, parsed.confidenceOf(ReceiptField.DATE))
    }

    @Test
    fun reportsBlankDateWhenNoCandidateIsConfident() {
        val parsed = parser.parse(receipt("WARUNG MAKAN" at 0.1f, "TOTAL 50.000" at 0.7f))

        assertNull(parsed.response.transactionDate)
        assertEquals(0f, parsed.confidenceOf(ReceiptField.DATE))
        assertTrue(ReceiptScan.from(parsed.response).transactionDate.isBlank())
    }

    @Test
    fun extractsFeeFromAdministrationLine() {
        val parsed = parser.parse(
            receipt(
                "TRANSFER BCA" at 0.10f,
                "NOMINAL 500.000" at 0.50f,
                "BIAYA ADMIN 2.500" at 0.60f,
                "TOTAL 502.500" at 0.70f,
                "05/03/2026" at 0.90f
            )
        )

        assertEquals(2_500L, parsed.response.fee)
        assertEquals(502_500L, parsed.response.totalPrice)
    }

    @Test
    fun reportsZeroFeeWhenNoFeeLineIsPresent() {
        val parsed = parser.parse(receipt("TOTAL 100.000" at 0.7f, "05/03/2026" at 0.9f))

        assertEquals(0L, parsed.response.fee)
    }

    @Test
    fun inferencesCategoryFromKnownMerchant() {
        val parsed = parser.parse(receipt("STARBUCKS GRAND INDONESIA" at 0.05f, "TOTAL 55.000" at 0.7f))

        assertEquals(TransactionCategory.FOOD.name, parsed.response.category)
    }

    @Test
    fun prefersMerchantOverPaymentAcquirer() {
        val parsed = parser.parse(
            receipt(
                "STARBUCKS GRAND INDONESIA" at 0.05f,
                "PEMBAYARAN QRIS GOPAY" at 0.50f,
                "TOTAL 55.000" at 0.70f
            )
        )

        assertEquals(TransactionCategory.FOOD.name, parsed.response.category)
    }

    @Test
    fun inferencesCategoryFromAcquirerWhenNoMerchantMatches() {
        val parsed = parser.parse(receipt("QRIS PEMBAYARAN" at 0.05f, "TOTAL 55.000" at 0.7f))

        assertEquals(TransactionCategory.ADMINISTRATION.name, parsed.response.category)
    }

    @Test
    fun returnsBlankCategoryForUnknownMerchant() {
        val parsed = parser.parse(receipt("TOKO SERBA JAYA" at 0.05f, "TOTAL 55.000" at 0.7f))

        assertNull(parsed.response.category)
        assertNull(ReceiptScan.from(parsed.response).category.ifBlank { null })
    }

    @Test
    fun lowersAmountConfidenceWhenCandidatesScoreComparably() {
        val clear = parser.parse(
            receipt(
                "SUBTOTAL 90.000" at 0.55f,
                "TOTAL 100.000" at 0.65f,
                "05/03/2026" at 0.90f
            )
        )
        val competing = parser.parse(
            receipt(
                "TOTAL 100.000" at 0.60f,
                "TOTAL BAYAR 100.500" at 0.62f,
                "05/03/2026" at 0.90f
            )
        )

        assertTrue(
            competing.confidenceOf(ReceiptField.AMOUNT) < clear.confidenceOf(ReceiptField.AMOUNT),
            "competing candidates must not report the confidence of a clear winner"
        )
        assertTrue(ReceiptField.AMOUNT in competing.lowConfidenceFields)
    }

    @Test
    fun reportsZeroAmountConfidenceWhenNoCurrencyIsRecognized() {
        val parsed = parser.parse(receipt("STRUK PEMBELIAN" at 0.1f))

        assertEquals(0L, parsed.response.totalPrice)
        assertEquals(0f, parsed.confidenceOf(ReceiptField.AMOUNT))
    }

    private fun dateOf(line: String): String? =
        parser.parse(receipt("TOTAL 50.000" at 0.7f, line at 0.9f)).response.transactionDate

    private companion object {
        val NOW: Instant = Instant.parse("2026-06-01T00:00:00Z")
    }
}

@OptIn(ExperimentalTime::class)
private class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

private infix fun String.at(centerY: Float): OcrLine = line(this, centerY)

private fun line(text: String, centerY: Float, left: Float = 0.05f): OcrLine = OcrLine(
    text = text,
    boundingBox = OcrRect(left = left, top = centerY - 0.01f, right = left + 0.3f, bottom = centerY + 0.01f)
)

private fun receipt(vararg lines: OcrLine): OcrResult =
    OcrResult(listOf(OcrBlock(text = lines.joinToString("\n") { it.text }, lines = lines.toList())))
