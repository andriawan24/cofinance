package id.andriawan.cofinance.data.local.merchant

import id.andriawan.cofinance.data.ocr.OcrBlock
import id.andriawan.cofinance.data.ocr.OcrLine
import id.andriawan.cofinance.data.ocr.OcrRect
import id.andriawan.cofinance.data.ocr.OcrResult
import id.andriawan.cofinance.data.ocr.parser.MerchantCategoryLearning
import id.andriawan.cofinance.data.ocr.parser.MerchantKey
import id.andriawan.cofinance.utils.enums.TransactionCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MerchantCategoryLearningTest {

    @Test
    fun correctedCategoryIsExtractedOnALaterScanOfTheSameMerchant() = runTest {
        val learning = MerchantCategoryLearning(FakeMerchantCategoryStore())

        val first = learning.parse(receipt("TOKO SERBA JAYA", "TOTAL 55.000"))
        assertNull(first.response.category, "an unknown merchant must not guess a category")

        learning.recordCategoryCorrection(TransactionCategory.HOUSING.name)

        val second = learning.parse(receipt("TOKO SERBA JAYA", "TOTAL 72.000"))
        assertEquals(TransactionCategory.HOUSING.name, second.response.category)
    }

    @Test
    fun learnedAssociationBeatsTheStaticDictionary() = runTest {
        val learning = MerchantCategoryLearning(FakeMerchantCategoryStore())

        val guessed = learning.parse(receipt("STARBUCKS GRAND INDONESIA", "TOTAL 55.000"))
        assertEquals(TransactionCategory.FOOD.name, guessed.response.category)

        learning.recordCategoryCorrection(TransactionCategory.SUBSCRIPTION.name)

        val learned = learning.parse(receipt("STARBUCKS GRAND INDONESIA", "TOTAL 55.000"))
        assertEquals(TransactionCategory.SUBSCRIPTION.name, learned.response.category)
    }

    @Test
    fun correctionKeyedToOneMerchantDoesNotLeakToAnother() = runTest {
        val learning = MerchantCategoryLearning(FakeMerchantCategoryStore())

        learning.parse(receipt("TOKO SERBA JAYA", "TOTAL 55.000"))
        learning.recordCategoryCorrection(TransactionCategory.HOUSING.name)

        val other = learning.parse(receipt("WARUNG PAK BUDI", "TOTAL 20.000"))
        assertEquals(TransactionCategory.FOOD.name, other.response.category)
    }

    @Test
    fun correctionWithoutAPrecedingScanRecordsNothing() = runTest {
        val store = FakeMerchantCategoryStore()

        MerchantCategoryLearning(store).recordCategoryCorrection(TransactionCategory.HOUSING.name)

        assertTrue(store.getAssociations().isEmpty())
    }

    @Test
    fun merchantKeyIsDerivedFromTheReceiptHeader() {
        assertEquals(
            MerchantKey.from(receipt("STARBUCKS GRAND INDONESIA LANTAI 2", "TOTAL 55.000")),
            MerchantKey.from(receipt("Starbucks - Grand Indonesia", "TOTAL 61.000"))
        )
        assertNull(MerchantKey.from(receipt("12/03/2026", "TOTAL 55.000")))
    }
}

private class FakeMerchantCategoryStore : MerchantCategoryLocalDataSource {
    private val associations = mutableMapOf<String, String>()
    var readCount = 0
        private set

    override suspend fun getAssociations(): Map<String, String> {
        readCount++
        return associations.toMap()
    }

    override suspend fun getAssociation(merchantKey: String): String? {
        readCount++
        return associations[merchantKey]
    }

    override suspend fun recordAssociation(merchantKey: String, category: String) {
        associations[merchantKey] = category
    }

    override suspend fun clearAssociations() {
        associations.clear()
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
