package id.andriawan.cofinance.data.ocr.fixtures

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Task 2.2 of OpenSpec change `on-device-receipt-scanning`: assert per-field
 * accuracy across the stored fixture corpus so a regression fails the build.
 *
 * ## This suite currently fails on purpose
 *
 * The corpus is empty and the thresholds are unset, because no real Indonesian
 * receipts were available when the harness was built. Every gate below is
 * written so that the empty state is a loud, explained failure rather than a
 * vacuous pass — measuring "100% accuracy over zero fixtures" would defeat the
 * requirement the harness exists to enforce.
 *
 * Making it pass means capturing real fixtures and recording a real baseline.
 * See `README.md` in this directory.
 */
class ReceiptCorpusAccuracyTest {

    @Test
    fun corpusIsLargeEnoughToMeasure() {
        val corpus = ReceiptFixtureCorpus.accuracyCorpus
        if (corpus.size < ReceiptAccuracyThresholds.MINIMUM_CORPUS_SIZE) {
            fail(
                """
                Receipt fixture corpus has ${corpus.size} real fixture(s); at least
                ${ReceiptAccuracyThresholds.MINIMUM_CORPUS_SIZE} are required before accuracy can be measured.

                This is not a flaky test. The corpus must be populated with fixtures captured
                from REAL Indonesian receipts by running platform OCR over them and serializing
                the resulting OcrResult. Fabricated fixtures would make the accuracy thresholds
                meaningless.

                The ${ReceiptFixtureCorpus.syntheticHarnessFixtures.size} synthetic fixture(s) present are excluded deliberately: they exist
                only to test this harness and must never count toward the corpus.

                See composeApp/src/commonTest/kotlin/id/andriawan/cofinance/data/ocr/fixtures/README.md
                """.trimIndent()
            )
        }
    }

    @Test
    fun corpusSpansEveryRequiredReceiptSource() {
        val corpus = ReceiptFixtureCorpus.accuracyCorpus
        val missing = ReceiptFixtureCorpus.missingSourceTypes(corpus)
        val underweight = ReceiptSourceType.entries
            .associateWith { type -> corpus.count { it.sourceType == type } }
            .filterValues { it in 1 until ReceiptAccuracyThresholds.MINIMUM_PER_SOURCE_TYPE }

        if (missing.isNotEmpty() || underweight.isNotEmpty()) {
            fail(
                """
                Fixture corpus does not span the four required Indonesian receipt sources.

                No fixtures at all: ${missing.ifEmpty { "none" }}
                Below the ${ReceiptAccuracyThresholds.MINIMUM_PER_SOURCE_TYPE}-fixture minimum: ${underweight.ifEmpty { "none" }}

                The spec requires bank transfer confirmations, QRIS payment slips, e-wallet
                receipts, and retail thermal receipts to all be represented, because their
                layouts and vocabulary differ enough that accuracy on one says little about
                the others.
                """.trimIndent()
            )
        }
    }

    @Test
    fun accuracyThresholdsAreSet() {
        val unset = ReceiptAccuracyThresholds.unsetThresholds()
        if (unset.isNotEmpty() || ReceiptAccuracyThresholds.MEASURED_BASELINE == null) {
            fail(
                """
                Accuracy thresholds are unset: ${unset.ifEmpty { "none" }}
                Measured baseline recorded: ${ReceiptAccuracyThresholds.MEASURED_BASELINE != null}

                Thresholds must be derived from a measurement over a real corpus, not chosen
                in advance. Populate the corpus, run this suite to read the reported per-field
                accuracy, record it in ReceiptAccuracyThresholds.MEASURED_BASELINE, then set
                AMOUNT_ACCURACY_THRESHOLD and DATE_ACCURACY_THRESHOLD from it.
                """.trimIndent()
            )
        }
    }

    @Test
    fun parserMeetsPerFieldAccuracyThresholds() {
        val corpus = ReceiptFixtureCorpus.accuracyCorpus
        if (corpus.size < ReceiptAccuracyThresholds.MINIMUM_CORPUS_SIZE) {
            fail(
                "Cannot measure accuracy: corpus holds ${corpus.size} of the required " +
                    "${ReceiptAccuracyThresholds.MINIMUM_CORPUS_SIZE} real fixtures. " +
                    "See corpusIsLargeEnoughToMeasure for what to do."
            )
        }

        val parse = ParserUnderTest.parse ?: fail(
            "ParserUnderTest.parse is not wired to the ReceiptParser. Point it at the parser " +
                "in commonMain so the corpus is measured against the real implementation."
        )

        val report = ReceiptAccuracyHarness.measure(corpus, parse)
        println("Receipt parser accuracy over ${report.size} fixtures:\n${report.describe()}")

        assertTrue(
            report.amountAccuracy >= ReceiptAccuracyThresholds.AMOUNT_ACCURACY_THRESHOLD,
            "Amount accuracy ${report.amountAccuracy} fell below the recorded threshold " +
                "${ReceiptAccuracyThresholds.AMOUNT_ACCURACY_THRESHOLD}.\n${report.describe()}"
        )
        assertTrue(
            report.dateAccuracy >= ReceiptAccuracyThresholds.DATE_ACCURACY_THRESHOLD,
            "Date accuracy ${report.dateAccuracy} fell below the recorded threshold " +
                "${ReceiptAccuracyThresholds.DATE_ACCURACY_THRESHOLD}.\n${report.describe()}"
        )
    }

    @Test
    fun fixturesCarryNoImageData() {
        val suspects = ReceiptFixtureCorpus.all.filter { fixture ->
            fixture.ocr.lines.any { line -> looksLikeEncodedImage(line.text) } ||
                fixture.ocr.blocks.any { block -> looksLikeEncodedImage(block.text) }
        }
        assertTrue(
            suspects.isEmpty(),
            "Fixture(s) ${suspects.map(ReceiptFixture::id)} contain text that looks like encoded " +
                "image data. Fixtures must hold recognized text and geometry only."
        )
    }

    /**
     * Recognized receipt text is short, human-readable tokens. A long unbroken
     * base64-ish run is the shape an accidentally-pasted image would take.
     */
    private fun looksLikeEncodedImage(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.startsWith("data:image", ignoreCase = true)) return true
        return trimmed.length > 256 && trimmed.none(Char::isWhitespace) &&
            trimmed.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
    }
}
