package id.andriawan.cofinance.data.ocr.parser

import id.andriawan.cofinance.data.ocr.OcrLine
import id.andriawan.cofinance.data.ocr.OcrResult

/**
 * Derives the identifier a learned category is stored under.
 *
 * Indonesian receipts print the merchant, bank, or acquirer name in the header, so the
 * key is taken from the topmost qualifying header line. The derivation is deterministic
 * and depends only on recognized text and geometry:
 *
 * 1. Lines are ordered top to bottom by bounding-box centre, then left to right.
 * 2. Only lines in the top [HEADER_FRACTION] of the receipt are considered.
 * 3. A line qualifies when it carries at least [MIN_LETTERS] letters — this skips
 *    logos recognized as punctuation, reference numbers, and dates.
 * 4. The line is normalized by [normalizeForMatch] (uppercased, non-alphanumerics
 *    collapsed to single spaces) and truncated to its first [KEY_TOKENS] tokens, so a
 *    branch suffix such as `STARBUCKS GRAND INDONESIA LANTAI 2` keys the same merchant
 *    as `STARBUCKS GRAND INDONESIA`.
 *
 * Returns null when no line qualifies, in which case nothing is learned or looked up.
 */
object MerchantKey {

    fun from(result: OcrResult): String? = from(result.lines)

    fun from(lines: List<OcrLine>): String? = lines
        .filter { it.boundingBox.centerY <= HEADER_FRACTION }
        .sortedWith(compareBy<OcrLine> { it.boundingBox.centerY }.thenBy { it.boundingBox.left })
        .firstNotNullOfOrNull { keyOf(it.text) }

    /** Same derivation applied to a single already-known header line. */
    fun keyOf(text: String): String? {
        if (text.count(Char::isLetter) < MIN_LETTERS) return null
        val tokens = normalizeForMatch(text).trim().split(' ').filter(String::isNotEmpty)
        if (tokens.isEmpty()) return null
        return tokens.take(KEY_TOKENS).joinToString(" ")
    }

    private const val HEADER_FRACTION = 0.4f
    private const val MIN_LETTERS = 3
    private const val KEY_TOKENS = 3
}
