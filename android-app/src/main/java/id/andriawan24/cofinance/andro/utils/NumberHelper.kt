package id.andriawan24.cofinance.andro.utils

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object NumberHelper {
    fun formatRupiah(number: Long): String {
        val currencyInstance = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        currencyInstance.roundingMode = RoundingMode.DOWN
        currencyInstance.maximumFractionDigits = 0
        val currency = currencyInstance.format(0).replace("0", "")
        return currencyInstance.format(number).replace(currency, "$currency ")
    }

    fun rupiahToNumber(value: String): Long {
        try {
            val currencyInstance = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            currencyInstance.roundingMode = RoundingMode.DOWN
            currencyInstance.maximumFractionDigits = 0
            return currencyInstance.parse(value)?.toLong() ?: 0
        } catch (_: Exception) {
            return 0
        }
    }

    fun formatNumber(number: Long): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
        numberFormat.roundingMode = RoundingMode.DOWN
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(number)
    }

    fun parseNumber(number: String): Long {
        val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
        numberFormat.roundingMode = RoundingMode.DOWN
        numberFormat.maximumFractionDigits = 0
        return numberFormat.parse(number)?.toLong() ?: 0
    }
}