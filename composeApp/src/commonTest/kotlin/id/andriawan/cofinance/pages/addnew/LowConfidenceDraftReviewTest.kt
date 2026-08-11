package id.andriawan.cofinance.pages.addnew

import id.andriawan.cofinance.data.local.merchant.MerchantCategoryLocalDataSource
import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import id.andriawan.cofinance.data.ocr.OcrBlock
import id.andriawan.cofinance.data.ocr.OcrLine
import id.andriawan.cofinance.data.ocr.OcrRect
import id.andriawan.cofinance.data.ocr.OcrResult
import id.andriawan.cofinance.data.ocr.parser.MerchantCategoryLearning
import id.andriawan.cofinance.data.ocr.parser.ParsedReceipt
import id.andriawan.cofinance.data.ocr.parser.ReceiptField
import id.andriawan.cofinance.data.ocr.parser.decodeReceiptFields
import id.andriawan.cofinance.data.ocr.parser.encodeToArgument
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.utils.enums.TransactionCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the draft review end of scanning: which fields arrive flagged for verification and
 * when a category change teaches the device-local merchant map.
 *
 * These exercise the state and the pure decisions rather than `AddNewViewModel`, which pulls
 * in Compose resources and a main dispatcher — the same approach as `OfflineFirstAccessTest`.
 */
class LowConfidenceDraftReviewTest {

    @Test
    fun lowConfidenceAmountReachesTheDraftFlagged() {
        val state = draftStateFrom(
            parsed(
                ReceiptField.AMOUNT to 0.2f,
                ReceiptField.DATE to 0.9f,
                ReceiptField.FEE to 0.9f,
                ReceiptField.CATEGORY to 0.9f
            )
        )

        assertTrue(state.needsVerification(ReceiptField.AMOUNT))
        assertEquals(setOf(ReceiptField.AMOUNT), state.lowConfidenceFields)
    }

    @Test
    fun fullyConfidentScanFlagsNothing() {
        val state = draftStateFrom(
            parsed(
                ReceiptField.AMOUNT to 0.95f,
                ReceiptField.DATE to 0.95f,
                ReceiptField.FEE to 0.95f,
                ReceiptField.CATEGORY to 0.95f
            )
        )

        assertTrue(state.lowConfidenceFields.isEmpty())
        ReceiptField.entries.forEach { assertFalse(state.needsVerification(it)) }
    }

    @Test
    fun flaggingLeavesEveryExtractedFieldEditable() {
        val prefilled = AddNewUiState(
            amount = "125000",
            fee = "2500",
            includeFee = true,
            expenseCategory = TransactionCategory.FOOD,
            isValid = true
        )
        val flagged = prefilled.copy(lowConfidenceFields = ReceiptField.entries.toSet())

        // Verification is additive: values and the save gate are untouched by the flags.
        assertEquals(prefilled, flagged.copy(lowConfidenceFields = emptySet()))
        assertTrue(flagged.isValid)
        ReceiptField.entries.forEach { assertTrue(flagged.needsVerification(it)) }
    }

    @Test
    fun changedCategoryOnAScannedDraftIsRecordedOnce() = runTest {
        val store = RecordingMerchantCategoryStore()
        val learning = MerchantCategoryLearning(store)
        learning.parse(receipt("TOKO SERBA JAYA", "TOTAL 55.000"))

        val correction = categoryCorrectionOf(
            scannedCategory = TransactionCategory.OTHERS.name,
            chosenCategory = TransactionCategory.HOUSING.name
        )
        correction?.let { learning.recordCategoryCorrection(it) }

        assertEquals(TransactionCategory.HOUSING.name, correction)
        assertEquals(1, store.recordCount)
        assertEquals(listOf(TransactionCategory.HOUSING.name), store.getAssociations().values.toList())
    }

    @Test
    fun untouchedCategoryRecordsNothing() = runTest {
        val store = RecordingMerchantCategoryStore()
        val learning = MerchantCategoryLearning(store)
        learning.parse(receipt("TOKO SERBA JAYA", "TOTAL 55.000"))

        val correction = categoryCorrectionOf(
            scannedCategory = TransactionCategory.FOOD.name,
            chosenCategory = TransactionCategory.FOOD.name
        )
        correction?.let { learning.recordCategoryCorrection(it) }

        assertNull(correction)
        assertEquals(0, store.recordCount)
    }

    @Test
    fun categoryChangeOnANonScannedDraftRecordsNothing() {
        assertNull(
            categoryCorrectionOf(
                scannedCategory = null,
                chosenCategory = TransactionCategory.HOUSING.name
            )
        )
    }

    /** Mirrors the scan-to-draft hop: parser confidence, through navigation, into the form state. */
    private fun draftStateFrom(parsed: ParsedReceipt): AddNewUiState {
        val argument = ReceiptScan.from(parsed).lowConfidenceFields.encodeToArgument()
        return AddNewUiState(lowConfidenceFields = decodeReceiptFields(argument))
    }

    private fun parsed(vararg confidence: Pair<ReceiptField, Float>) = ParsedReceipt(
        response = ReceiptScanResponse(
            totalPrice = 125_000,
            transactionDate = "2026-03-05T10:15:00+07:00",
            fee = 2_500,
            category = TransactionCategory.FOOD.name
        ),
        confidence = confidence.toMap()
    )
}

private class RecordingMerchantCategoryStore : MerchantCategoryLocalDataSource {
    private val associations = mutableMapOf<String, String>()
    var recordCount = 0
        private set

    override suspend fun getAssociations(): Map<String, String> = associations.toMap()

    override suspend fun getAssociation(merchantKey: String): String? = associations[merchantKey]

    override suspend fun recordAssociation(merchantKey: String, category: String) {
        recordCount++
        associations[merchantKey] = category
    }

    override suspend fun clearAssociations() {
        associations.clear()
        recordCount = 0
    }
}

private fun receipt(header: String, vararg body: String): OcrResult {
    val lines = listOf(line(header, 0.05f)) +
        body.mapIndexed { index, text -> line(text, 0.6f + index * 0.05f) }
    return OcrResult(listOf(OcrBlock(text = lines.joinToString("\n") { it.text }, lines = lines)))
}

private fun line(text: String, centerY: Float): OcrLine = OcrLine(
    text = text,
    boundingBox = OcrRect(left = 0.05f, top = centerY - 0.01f, right = 0.35f, bottom = centerY + 0.01f)
)
