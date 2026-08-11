package id.andriawan.cofinance.data.ocr.fixtures

import id.andriawan.cofinance.data.ocr.OcrResult
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the accuracy harness, not the parser.
 *
 * Everything here runs against the SYNTHETIC harness fixtures and stub parse
 * functions whose accuracy is known in advance, so a bug in the loader or in the
 * accuracy arithmetic surfaces here rather than silently distorting the real
 * corpus measurement.
 */
class ReceiptAccuracyHarnessSelfTest {

    @Test
    fun loaderParsesEverySyntheticFixture() {
        val synthetic = ReceiptFixtureCorpus.syntheticHarnessFixtures

        assertEquals(3, synthetic.size)
        assertContentEquals(
            listOf(
                "synthetic-harness-clean",
                "synthetic-harness-fee-bearing",
                "synthetic-harness-no-category"
            ),
            synthetic.map(ReceiptFixture::id)
        )
        assertTrue(synthetic.all { it.ocr.lines.isNotEmpty() }, "fixtures should carry OCR lines")
        assertTrue(
            synthetic.all { fixture -> fixture.ocr.lines.all { it.boundingBox.top in 0f..1f } },
            "fixture geometry should be normalized to 0..1"
        )
    }

    @Test
    fun syntheticFixturesAreExcludedFromTheAccuracyCorpus() {
        assertTrue(ReceiptFixtureCorpus.syntheticHarnessFixtures.isNotEmpty())
        assertTrue(
            ReceiptFixtureCorpus.accuracyCorpus.none(ReceiptFixture::synthetic),
            "synthetic fixtures must never enter the measured corpus"
        )
        assertEquals(
            ReceiptFixtureCorpus.all.size,
            ReceiptFixtureCorpus.accuracyCorpus.size + ReceiptFixtureCorpus.syntheticHarnessFixtures.size
        )
    }

    @Test
    fun perfectParserScoresOneOnEveryField() {
        val report = ReceiptAccuracyHarness.measure(syntheticFixtures, ::parsePerfectly)

        assertEquals(1.0, report.amountAccuracy, TOLERANCE)
        assertEquals(1.0, report.dateAccuracy, TOLERANCE)
        assertEquals(1.0, report.feeAccuracy, TOLERANCE)
        assertEquals(1.0, report.categoryAccuracy, TOLERANCE)
        assertTrue(report.failures.isEmpty())
    }

    @Test
    fun oneWrongAmountOutOfThreeScoresTwoThirds() {
        val report = ReceiptAccuracyHarness.measure(syntheticFixtures) { ocr ->
            val perfect = parsePerfectly(ocr)
            if (perfect.totalPrice == 150_000L) perfect.copy(totalPrice = 999L) else perfect
        }

        assertEquals(2.0 / 3.0, report.amountAccuracy, TOLERANCE)
        assertEquals(1.0, report.dateAccuracy, TOLERANCE, "an amount miss must not disturb the date field")
    }

    @Test
    fun emptyFixtureSetReportsZeroAccuracyNotPerfect() {
        val report = ReceiptAccuracyHarness.measure(emptyList(), ::parsePerfectly)

        assertEquals(0, report.size)
        assertEquals(0.0, report.amountAccuracy, TOLERANCE, "an empty corpus must never report a passing score")
        assertEquals(0.0, report.dateAccuracy, TOLERANCE)
    }

    @Test
    fun thrownParseCountsAsAMissRatherThanAbortingTheRun() {
        val report = ReceiptAccuracyHarness.measure(syntheticFixtures) { ocr ->
            val perfect = parsePerfectly(ocr)
            if (perfect.totalPrice == 37_000L) error("stub parser failure") else perfect
        }

        assertEquals(3, report.size)
        assertEquals(1, report.failures.size)
        assertEquals(2.0 / 3.0, report.amountAccuracy, TOLERANCE)
    }

    @Test
    fun blankExpectationsCompareEqualToBlankOutput() {
        val noCategoryFixture = syntheticFixtures.single { it.expected.category.isEmpty() }
        val report = ReceiptAccuracyHarness.measure(listOf(noCategoryFixture), ::parsePerfectly)

        assertEquals(1.0, report.categoryAccuracy, TOLERANCE)
        assertEquals(1.0, report.dateAccuracy, TOLERANCE, "blank expected date must match blank extracted date")
    }

    @Test
    fun aDegradedParserFallsBelowAThresholdItWouldOtherwiseMeet() {
        val threshold = 0.9
        val good = ReceiptAccuracyHarness.measure(syntheticFixtures, ::parsePerfectly)
        val degraded = ReceiptAccuracyHarness.measure(syntheticFixtures) { ReceiptScan() }

        assertTrue(good.amountAccuracy >= threshold, "harness must pass a correct parser")
        assertFalse(degraded.amountAccuracy >= threshold, "harness must fail a broken parser")
    }

    @Test
    fun missingSourceTypesAreReported() {
        val onlyQris = syntheticFixtures.filter { it.sourceType == ReceiptSourceType.QRIS }

        assertContentEquals(
            listOf(
                ReceiptSourceType.BANK_TRANSFER,
                ReceiptSourceType.E_WALLET,
                ReceiptSourceType.RETAIL_THERMAL
            ),
            ReceiptFixtureCorpus.missingSourceTypes(onlyQris)
        )
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }

    private val syntheticFixtures: List<ReceiptFixture>
        get() = ReceiptFixtureCorpus.syntheticHarnessFixtures

    /**
     * A stub parser that reads the answer off the fixture. It exists so the
     * harness can be exercised with a known-correct input; it is never used to
     * measure anything real.
     */
    private fun parsePerfectly(ocr: OcrResult): ReceiptScan {
        val expected = syntheticFixtures.first { it.ocr == ocr }.expected
        return ReceiptScan(
            totalPrice = expected.amount,
            transactionDate = expected.date,
            fee = expected.fee,
            category = expected.category
        )
    }
}
