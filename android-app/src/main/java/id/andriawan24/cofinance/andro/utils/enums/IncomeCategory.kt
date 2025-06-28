package id.andriawan24.cofinance.andro.utils.enums

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import id.andriawan24.cofinance.andro.R

enum class IncomeCategory(val color: Color, @DrawableRes val iconRes: Int, val label: String) {
    FOOD(Color(0xFFEDF7EF), R.drawable.ic_money, "Salary"),
    TRANSPORT(Color(0xFFFEFAEE), R.drawable.ic_chart, "Invest");

    companion object {
        fun getCategoryByName(name: String): IncomeCategory {
            return IncomeCategory.valueOf(name)
        }
    }
}
