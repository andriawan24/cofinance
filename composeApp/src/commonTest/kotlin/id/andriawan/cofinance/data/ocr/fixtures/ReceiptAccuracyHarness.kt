package id.andriawan.cofinance.data.ocr.fixtures

import id.andriawan.cofinance.domain.model.response.ReceiptScan

/**
 * Runs a parse function across a set of fixtures and reports per-field accuracy.
 *
 * The harness knows nothing about parsing. It compares what a [ParseReceipt]
 * produced against what a fixture declared, counts matches, and reports. That
 * separation is what lets the harness itself be tested with stub parsers whose
 * accuracy is known in advance.
 */
object ReceiptAccuracyHarness {

    fun measure(fixtures: List<ReceiptFixture>, parse: ParseReceipt): AccuracyReport {
        val results = fixtures.map { fixture ->
            val actual = runCatching { parse(fixture.ocr) }
            FixtureOutcome(
                fixture = fixture,
                actual = actual.getOrNull(),
                failure = actual.exceptionOrNull(),
                matches = actual.getOrNull()?.let { compare(fixture.expected, it) } ?: FieldMatches.NONE
            )
        }
        return AccuracyReport(results)
    }

    private fun compare(expected: ExpectedParse, actual: ReceiptScan) = FieldMatches(
        amount = expected.amount == actual.totalPrice,
        date = normalizeDate(expected.date) == normalizeDate(actual.transactionDate),
        fee = expected.fee == actual.fee,
        category = expected.category.trim().uppercase() == actual.category.trim().uppercase()
    )

    /**
     * Dates are compared as text after light normalization rather than by
     * parsing to an instant, so that a parser emitting a differently-shaped but
     * equivalent string is reported as a mismatch and gets looked at. Receipt
     * dates feed a draft the user reviews; an unexpected shape is worth seeing.
     */
    private fun normalizeDate(value: String): String = value.trim().uppercase()
}

/** Per-field match outcome for a single fixture. */
data class FieldMatches(
    val amount: Boolean,
    val date: Boolean,
    val fee: Boolean,
    val category: Boolean
) {
    companion object {
        /** Used when the parser threw: nothing matched. */
        val NONE = FieldMatches(amount = false, date = false, fee = false, category = false)
    }
}

/** What happened for one fixture. */
data class FixtureOutcome(
    val fixture: ReceiptFixture,
    val actual: ReceiptScan?,
    val failure: Throwable?,
    val matches: FieldMatches
)

/** Aggregate accuracy across a fixture set. */
data class AccuracyReport(val outcomes: List<FixtureOutcome>) {

    val size: Int = outcomes.size

    val amountAccuracy: Double = fraction { it.matches.amount }
    val dateAccuracy: Double = fraction { it.matches.date }
    val feeAccuracy: Double = fraction { it.matches.fee }
    val categoryAccuracy: Double = fraction { it.matches.category }

    /** Fixtures whose parse threw. */
    val failures: List<FixtureOutcome> = outcomes.filter { it.failure != null }

    /**
     * Accuracy over an empty set is undefined, not perfect. Returning 0.0 here
     * means an empty corpus can never satisfy a positive threshold by accident.
     */
    private fun fraction(predicate: (FixtureOutcome) -> Boolean): Double =
        if (outcomes.isEmpty()) 0.0 else outcomes.count(predicate).toDouble() / outcomes.size

    fun describe(): String = buildString {
        appendLine("Fixtures measured: $size")
        appendLine("  amount   ${percent(amountAccuracy)}")
        appendLine("  date     ${percent(dateAccuracy)}")
        appendLine("  fee      ${percent(feeAccuracy)}")
        appendLine("  category ${percent(categoryAccuracy)}")
        if (failures.isNotEmpty()) {
            appendLine("Parser threw on ${failures.size} fixture(s):")
            failures.forEach { appendLine("  ${it.fixture.id}: ${it.failure}") }
        }
        mismatches().takeIf(List<String>::isNotEmpty)?.let { lines ->
            appendLine("Mismatches:")
            lines.forEach(::appendLine)
        }
    }

    private fun mismatches(): List<String> = outcomes.flatMap { outcome ->
        buildList {
            val expected = outcome.fixture.expected
            val actual = outcome.actual
            if (!outcome.matches.amount) {
                add("  ${outcome.fixture.id} amount: expected ${expected.amount}, got ${actual?.totalPrice}")
            }
            if (!outcome.matches.date) {
                add("  ${outcome.fixture.id} date: expected '${expected.date}', got '${actual?.transactionDate}'")
            }
        }
    }

    private fun percent(value: Double): String {
        val scaled = (value * 1000).toInt()
        return "${scaled / 10}.${scaled % 10}%"
    }
}
