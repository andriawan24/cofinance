package id.andriawan.cofinance.data.ocr.parser

import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import id.andriawan.cofinance.data.ocr.OcrLine
import id.andriawan.cofinance.data.ocr.OcrResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class ReceiptField { AMOUNT, DATE, FEE, CATEGORY }

data class ParsedReceipt(
    val response: ReceiptScanResponse,
    val confidence: Map<ReceiptField, Float>
) {
    fun confidenceOf(field: ReceiptField): Float = confidence[field] ?: 0f

    val lowConfidenceFields: Set<ReceiptField>
        get() = ReceiptField.entries
            .filter { confidenceOf(it) < ReceiptParser.LOW_CONFIDENCE_THRESHOLD }
            .toSet()
}

/**
 * Derives the fields the transaction draft consumes — total amount, transaction date,
 * fee, and category — from recognized receipt text, using Indonesian receipt vocabulary
 * and formatting.
 *
 * Extraction is deterministic and never leaves the device. Every field is reported with
 * a confidence so the review screen can direct the user's attention, and a date that no
 * candidate supports confidently is reported blank rather than guessed.
 */
@OptIn(ExperimentalTime::class)
class ReceiptParser(private val clock: Clock = Clock.System) {

    /**
     * [learnedCategories] maps a [MerchantKey] to a category the user previously chose for
     * that merchant on this device. A learned category wins over the static dictionary, so
     * a correction always beats the built-in guess. Resolving the map is the caller's job,
     * which keeps this parser pure and synchronous.
     */
    fun parse(
        result: OcrResult,
        learnedCategories: Map<String, String> = emptyMap()
    ): ParsedReceipt {
        val lines = result.lines.sortedWith(
            compareBy<OcrLine> { it.boundingBox.centerY }.thenBy { it.boundingBox.left }
        )
        val texts = lines.map(OcrLine::text)
        val normalized = texts.map(::normalizeForMatch)

        val amount = extractAmount(lines, normalized)
        val date = ReceiptDateParser.parse(texts, clock.now())
        val fee = extractFee(texts, normalized)
        val category = extractCategory(lines, normalized, learnedCategories)
        val confidentDate = date.confidence >= MIN_DATE_CONFIDENCE

        return ParsedReceipt(
            response = ReceiptScanResponse(
                totalPrice = amount.value,
                transactionDate = if (confidentDate) date.isoDate else null,
                fee = fee.value,
                category = category.value
            ),
            confidence = mapOf(
                ReceiptField.AMOUNT to amount.confidence,
                ReceiptField.DATE to if (confidentDate) date.confidence else 0f,
                ReceiptField.FEE to fee.confidence,
                ReceiptField.CATEGORY to category.confidence
            )
        )
    }

    private fun extractAmount(lines: List<OcrLine>, normalized: List<String>): Extracted<Long> {
        val candidates = lines.flatMapIndexed { index, line ->
            CurrencyNormalizer.tokensIn(line.text).map { value ->
                AmountCandidate(value, index, line.boundingBox.centerY)
            }
        }
        if (candidates.isEmpty()) return Extracted(0L, 0f)

        val largest = candidates.maxOf { it.value }
        val scored = candidates
            .map { it to score(it, normalized, largest) }
            .sortedByDescending { it.second }

        val (best, bestScore) = scored.first()
        val runnerUpScore = scored.getOrNull(1)?.second

        val confidence = when {
            runnerUpScore == null -> SINGLE_CANDIDATE_CONFIDENCE
            bestScore <= 0f -> AMBIGUOUS_AMOUNT_CONFIDENCE
            else -> {
                val margin = (bestScore - runnerUpScore) / bestScore
                (AMBIGUOUS_AMOUNT_CONFIDENCE + (1f - AMBIGUOUS_AMOUNT_CONFIDENCE) * margin)
                    .coerceIn(0f, 1f)
            }
        }

        return Extracted(best.value, confidence)
    }

    private fun score(
        candidate: AmountCandidate,
        normalized: List<String>,
        largest: Long
    ): Float {
        val ownLine = normalized[candidate.lineIndex]
        val lineAbove = normalized.getOrNull(candidate.lineIndex - 1)

        if (ownLine.containsAnyKeyword(ReceiptLexicon.NEGATIVE_KEYWORDS) ||
            ownLine.containsAnyKeyword(ReceiptLexicon.FEE_KEYWORDS)
        ) {
            return NEGATIVE_WEIGHT
        }

        val keyword = when {
            ownLine.containsAnyKeyword(ReceiptLexicon.TOTAL_KEYWORDS) -> SAME_LINE_KEYWORD_WEIGHT
            lineAbove != null && lineAbove.qualifiesAsTotalLabel() -> LINE_ABOVE_KEYWORD_WEIGHT
            else -> 0f
        }

        val position = ((candidate.centerY - 0.5f) * 2f).coerceIn(0f, 1f) * POSITION_WEIGHT
        val magnitude = (candidate.value.toFloat() / largest) * MAGNITUDE_WEIGHT

        return keyword + position + magnitude
    }

    private fun String.qualifiesAsTotalLabel(): Boolean =
        containsAnyKeyword(ReceiptLexicon.TOTAL_KEYWORDS) &&
            !containsAnyKeyword(ReceiptLexicon.NEGATIVE_KEYWORDS) &&
            !containsAnyKeyword(ReceiptLexicon.FEE_KEYWORDS)

    private fun extractFee(texts: List<String>, normalized: List<String>): Extracted<Long> {
        normalized.forEachIndexed { index, line ->
            if (!line.containsAnyKeyword(ReceiptLexicon.FEE_KEYWORDS)) return@forEachIndexed

            val value = CurrencyNormalizer.tokensIn(texts[index]).firstOrNull()
                ?: texts.getOrNull(index + 1)?.let { CurrencyNormalizer.tokensIn(it).firstOrNull() }

            if (value != null) return Extracted(value, FEE_MATCH_CONFIDENCE)
        }
        return Extracted(0L, FEE_ABSENT_CONFIDENCE)
    }

    private fun extractCategory(
        lines: List<OcrLine>,
        normalized: List<String>,
        learnedCategories: Map<String, String>
    ): Extracted<String?> {
        if (learnedCategories.isNotEmpty()) {
            MerchantKey.from(lines)?.let { learnedCategories[it] }?.let {
                return Extracted(it, LEARNED_CONFIDENCE)
            }
        }
        matchDictionary(normalized, ReceiptLexicon.MERCHANTS)?.let {
            return Extracted(it.name, MERCHANT_CONFIDENCE)
        }
        matchDictionary(normalized, ReceiptLexicon.ACQUIRERS)?.let {
            return Extracted(it.name, ACQUIRER_CONFIDENCE)
        }
        return Extracted(null, 0f)
    }

    private fun <T> matchDictionary(normalized: List<String>, dictionary: Map<String, T>): T? {
        val keywords = dictionary.keys.sortedByDescending(String::length)
        normalized.forEach { line ->
            keywords.forEach { keyword ->
                if (line.containsKeyword(keyword)) return dictionary.getValue(keyword)
            }
        }
        return null
    }

    private data class AmountCandidate(val value: Long, val lineIndex: Int, val centerY: Float)

    private data class Extracted<T>(val value: T, val confidence: Float)

    companion object {
        /** Fields scoring below this are surfaced for user verification. */
        const val LOW_CONFIDENCE_THRESHOLD = 0.6f

        /** A date below this is reported blank, which the scan flow treats as a failure. */
        const val MIN_DATE_CONFIDENCE = 0.5f

        private const val SAME_LINE_KEYWORD_WEIGHT = 3f
        private const val LINE_ABOVE_KEYWORD_WEIGHT = 2f
        private const val POSITION_WEIGHT = 2f
        private const val MAGNITUDE_WEIGHT = 1.5f
        private const val NEGATIVE_WEIGHT = -4f

        private const val SINGLE_CANDIDATE_CONFIDENCE = 0.9f
        private const val AMBIGUOUS_AMOUNT_CONFIDENCE = 0.55f
        private const val FEE_MATCH_CONFIDENCE = 0.9f
        private const val FEE_ABSENT_CONFIDENCE = 0.7f
        private const val LEARNED_CONFIDENCE = 0.95f
        private const val MERCHANT_CONFIDENCE = 0.85f
        private const val ACQUIRER_CONFIDENCE = 0.65f
    }
}
