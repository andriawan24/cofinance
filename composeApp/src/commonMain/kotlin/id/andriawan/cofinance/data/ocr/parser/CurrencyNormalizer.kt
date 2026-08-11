package id.andriawan.cofinance.data.ocr.parser

/**
 * Normalizes Indonesian currency tokens: an optional `Rp`/`IDR` prefix, `.` as the
 * thousands separator, `,` as the decimal separator, and the common `,-` suffix.
 *
 * Fractions are truncated because the app stores amounts as whole rupiah. Malformed
 * input yields `null` rather than an exception.
 */
internal object CurrencyNormalizer {

    private const val PLAIN_TOKEN_MAX = 9_999_999L
    private const val PLAIN_TOKEN_MIN = 100L

    private val PREFIX = Regex("^(RP|IDR)\\.?")
    private val GROUPED = Regex("^\\d{1,3}(\\.\\d{3})+$")
    private val PLAIN = Regex("^\\d+$")

    private val TOKEN = Regex(
        "(?:(?:RP|IDR)\\.?\\s*)?\\d[\\d.]*(?:,(?:\\d{1,2}|-))?",
        RegexOption.IGNORE_CASE
    )

    fun parse(raw: String): Long? {
        var text = raw.trim().uppercase().replace(" ", "").trimEnd('.')
        text = PREFIX.replace(text, "")
        if (text.endsWith(",-")) text = text.dropLast(2)
        if (text.isEmpty()) return null

        val commaIndex = text.indexOf(',')
        val integerPart = if (commaIndex >= 0) text.substring(0, commaIndex) else text
        if (commaIndex >= 0) {
            val fraction = text.substring(commaIndex + 1)
            if (fraction.length !in 1..2 || !fraction.all(Char::isDigit)) return null
        }

        if (!GROUPED.matches(integerPart) && !PLAIN.matches(integerPart)) return null
        return integerPart.replace(".", "").toLongOrNull()
    }

    /**
     * Currency values appearing in [line], in reading order. Digit runs that belong to
     * dates, times, or identifiers are skipped, and unmarked plain numbers are accepted
     * only inside a plausible receipt range so account numbers do not become candidates.
     */
    fun tokensIn(line: String): List<Long> = TOKEN.findAll(line).mapNotNull { match ->
        if (!hasTokenBoundaries(line, match.range.first, match.range.last)) return@mapNotNull null

        val raw = match.value.trim().trimEnd('.')
        val value = parse(raw) ?: return@mapNotNull null

        val marked = raw.uppercase().let { it.startsWith("RP") || it.startsWith("IDR") } ||
            raw.contains('.') || raw.contains(',')

        if (marked || value in PLAIN_TOKEN_MIN..PLAIN_TOKEN_MAX) value else null
    }.toList()

    private fun hasTokenBoundaries(line: String, start: Int, end: Int): Boolean {
        val before = line.getOrNull(start - 1)
        val after = line.getOrNull(end + 1)
        return before?.isTokenBreak() != false && after?.isTokenBreak() != false
    }

    private fun Char.isTokenBreak(): Boolean =
        !isLetterOrDigit() && this != '/' && this != '-' && this != ':' && this != ','
}
