package id.andriawan24.cofinance.andro.utils

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object NumberHelper {
    fun formatRupiah(number: Int): String {
        val currencyInstance = NumberFormat.getCurrencyInstance(Locale.getDefault())
        currencyInstance.roundingMode = RoundingMode.DOWN
        currencyInstance.maximumFractionDigits = 0
        val currency = currencyInstance.format(0).replace("0", "")
        return currencyInstance.format(number).replace(currency, "$currency ")
    }
}