package id.andriawan.cofinance.data.ocr.fixtures

/**
 * Corpus size and per-field accuracy thresholds asserted by
 * `ReceiptCorpusAccuracyTest` (OpenSpec change `on-device-receipt-scanning`,
 * task 2.2).
 *
 * ## Why the thresholds start unset
 *
 * A threshold is only meaningful next to a measurement. No real Indonesian
 * receipts were available when this harness was written, so the corpus is empty
 * and no baseline has been measured. Rather than invent numbers that would pass
 * vacuously against an empty corpus, the thresholds are [UNSET] and the test
 * fails until a human records real ones.
 *
 * ## Setting them for real
 *
 * 1. Capture at least [MINIMUM_CORPUS_SIZE] real fixtures spanning all four
 *    source types (see `README.md` in this directory).
 * 2. Run the corpus accuracy test. It reports the measured per-field accuracy
 *    even when it fails.
 * 3. Record the measurement in [MEASURED_BASELINE] below, then set
 *    [AMOUNT_ACCURACY_THRESHOLD] and [DATE_ACCURACY_THRESHOLD] at or slightly
 *    below the measured value so that a regression, not normal variance, is
 *    what trips the assertion.
 */
object ReceiptAccuracyThresholds {

    /** Sentinel for a threshold nobody has measured yet. Never a valid accuracy. */
    const val UNSET: Double = -1.0

    /**
     * Smallest corpus that can produce a credible accuracy number.
     *
     * Twenty fixtures makes each fixture worth five percentage points, which is
     * fine enough to distinguish a real regression from a single odd receipt,
     * and is reachable by one person collecting their own receipts. Spread
     * across the four required source types, that is at least five each.
     */
    const val MINIMUM_CORPUS_SIZE: Int = 20

    /** Minimum fixtures required per source type, so no source is token-represented. */
    const val MINIMUM_PER_SOURCE_TYPE: Int = 3

    /**
     * Fraction of the corpus whose extracted amount must exactly match the
     * expected amount.
     *
     * UNSET — no baseline measured. See the class docs.
     */
    const val AMOUNT_ACCURACY_THRESHOLD: Double = UNSET

    /**
     * Fraction of the corpus whose extracted date must match the expected date.
     *
     * UNSET — no baseline measured. See the class docs.
     */
    const val DATE_ACCURACY_THRESHOLD: Double = UNSET

    /**
     * The measurement each threshold above was derived from.
     *
     * Fill this in at the same time the thresholds are set. It is what makes a
     * later "why is the threshold 0.88?" answerable.
     */
    val MEASURED_BASELINE: MeasuredBaseline? = null

    /** Thresholds still carrying the [UNSET] sentinel, by field name. */
    fun unsetThresholds(): List<String> = buildList {
        if (AMOUNT_ACCURACY_THRESHOLD == UNSET) add("AMOUNT_ACCURACY_THRESHOLD")
        if (DATE_ACCURACY_THRESHOLD == UNSET) add("DATE_ACCURACY_THRESHOLD")
    }
}

/**
 * A recorded accuracy measurement, kept alongside the thresholds it justifies.
 */
data class MeasuredBaseline(
    /** ISO date the measurement was taken. */
    val measuredOn: String,
    /** Number of real fixtures in the corpus at measurement time. */
    val corpusSize: Int,
    val amountAccuracy: Double,
    val dateAccuracy: Double,
    val feeAccuracy: Double,
    val categoryAccuracy: Double,
    /** Anything that would change how the number should be read. */
    val notes: String = ""
)
