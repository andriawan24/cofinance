package id.andriawan.cofinance.utils

object NumberHelper {
    private const val CURRENCY_SYMBOL = "Rp"
    private const val THOUSAND_SEPARATOR = '.'

    fun formatRupiah(number: Long): String {
        return "$CURRENCY_SYMBOL${formatNumber(number)}"
    }

    fun rupiahToNumber(value: String): Long {
        return try {
            val cleanedValue = value
                .replace(CURRENCY_SYMBOL, "")
                .replace(THOUSAND_SEPARATOR.toString(), "")
                .trim()
            cleanedValue.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun formatNumber(number: Long): String {
        val isNegative = number < 0
        val absoluteNumber = if (isNegative) -number else number
        val numberString = absoluteNumber.toString()

        val formatted = buildString {
            numberString.reversed().forEachIndexed { index, char ->
                if (index > 0 && index % 3 == 0) {
                    append(THOUSAND_SEPARATOR)
                }
                append(char)
            }
        }.reversed()

        return if (isNegative) "-$formatted" else formatted
    }

    fun parseNumber(number: String): Long {
        return try {
            val cleanedValue = number
                .replace(THOUSAND_SEPARATOR.toString(), "")
                .trim()
            cleanedValue.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
