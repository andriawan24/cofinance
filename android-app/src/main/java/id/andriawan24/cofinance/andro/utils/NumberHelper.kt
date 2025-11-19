package id.andriawan24.cofinance.andro.utils

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object NumberHelper {
    fun formatRupiah(number: Long): String {
        val indonesianLocale = Locale.Builder()
            .setLanguage("id")
            .setRegion("ID")
            .build()

        val currencyInstance = NumberFormat.getCurrencyInstance(indonesianLocale)
        currencyInstance.roundingMode = RoundingMode.DOWN
        currencyInstance.maximumFractionDigits = 0
        return currencyInstance.format(number)
    }

    fun rupiahToNumber(value: String): Long {
        try {
            val indonesianLocale = Locale.Builder()
                .setLanguage("id")
                .setRegion("ID")
                .build()

            val currencyInstance = NumberFormat.getCurrencyInstance(indonesianLocale)
            currencyInstance.roundingMode = RoundingMode.DOWN
            currencyInstance.maximumFractionDigits = 0
            return currencyInstance.parse(value)?.toLong() ?: 0
        } catch (_: Exception) {
            return 0
        }
    }

    fun formatNumber(number: Long): String {
        val indonesianLocale = Locale.Builder()
            .setLanguage("id")
            .setRegion("ID")
            .build()

        val numberFormat = NumberFormat.getNumberInstance(indonesianLocale)
        numberFormat.roundingMode = RoundingMode.DOWN
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(number)
    }

    fun parseNumber(number: String): Long {
        val indonesianLocale = Locale.Builder()
            .setLanguage("id")
            .setRegion("ID")
            .build()

        val numberFormat = NumberFormat.getNumberInstance(indonesianLocale)
        numberFormat.roundingMode = RoundingMode.DOWN
        numberFormat.maximumFractionDigits = 0
        return numberFormat.parse(number)?.toLong() ?: 0
    }
}