package id.andriawan24.cofinance.andro.utils.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastFilter
import id.andriawan24.cofinance.andro.R

enum class TransactionCategory(
    val color: Color,
    val iconColor: Color,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int
) {
    FOOD(
        color = Color(0xFFF0F3FF),
        iconRes = R.drawable.ic_category_food,
        labelRes = R.string.label_category_food,
        iconColor = Color(0xFF6C89FE)
    ),
    TRANSPORT(
        color = Color(0xFFFEEEF1),
        iconRes = R.drawable.ic_category_transport,
        labelRes = R.string.label_category_transport,
        iconColor = Color(0xFFF55376)
    ),
    HOUSING(
        color = Color(0xFFEEF9F8),
        iconRes = R.drawable.ic_category_housing,
        labelRes = R.string.label_category_housing,
        iconColor = Color(0xFF57BEB5)
    ),
    APPAREL(
        color = Color(0xFFFEFAEE),
        iconRes = R.drawable.ic_category_apparel,
        labelRes = R.string.label_category_apparel,
        iconColor = Color(0xFFF5CB53)
    ),
    HEALTH(
        color = Color(0xFFFDF5F1),
        iconRes = R.drawable.ic_category_health,
        labelRes = R.string.label_category_health,
        iconColor = Color(0xFFEB7C55)
    ),
    EDUCATION(
        color = Color(0xFFEFFAFD),
        iconRes = R.drawable.ic_category_education,
        labelRes = R.string.label_category_education,
        iconColor = Color(0xFF5CCFEA)
    ),
    SUBSCRIPTION(
        color = Color(0xFFFFF2F6),
        iconRes = R.drawable.ic_category_subscription,
        labelRes = R.string.label_category_subscription,
        iconColor = Color(0xFFC23053)
    ),
    INTERNET(
        color = Color(0xFFF2F2F2),
        iconRes = R.drawable.ic_category_internet,
        labelRes = R.string.label_category_internet,
        iconColor = Color(0xFF7A7A7A)
    ),
    DEBT(
        color = Color(0xFFF6F4ED),
        iconRes = R.drawable.ic_category_debt,
        labelRes = R.string.label_category_debt,
        iconColor = Color(0xFFA38C4B)
    ),
    GIFT(
        color = Color(0xFFFFF4FD),
        iconRes = R.drawable.ic_category_gift,
        labelRes = R.string.label_category_gift,
        iconColor = Color(0xFFA33594)
    ),
    ADMINISTRATION(
        color = Color(0xFFEDF7EF),
        iconRes = R.drawable.ic_category_administration,
        labelRes = R.string.label_category_administration,
        iconColor = Color(0xFF4BAB63)
    ),
    OTHERS(
        color = Color(0xFFF6EDF9),
        iconRes = R.drawable.ic_category_others,
        labelRes = R.string.label_category_others,
        iconColor = Color(0xFFA146C2)
    ),
    SALARY(
        color = Color(0xFFEDF7EF),
        iconRes = R.drawable.ic_money,
        labelRes = R.string.label_category_salary,
        iconColor = Color(0xFF4BAB63)
    ),
    INVEST(
        color = Color(0xFFFEFAEE),
        iconRes = R.drawable.ic_chart,
        labelRes = R.string.label_category_invest,
        iconColor = Color(0xFFF5CB53)
    );

    companion object Companion {
        fun getCategoryByName(name: String): TransactionCategory {
            return TransactionCategory.entries.firstOrNull { it.name == name } ?: OTHERS
        }

        fun getExpenseCategories(): List<TransactionCategory> {
            return TransactionCategory.entries.filter { it != SALARY && it != INVEST }
        }

        fun getIncomeCategories(): List<TransactionCategory> {
            return TransactionCategory.entries.fastFilter { it == SALARY || it == INVEST }
        }
    }
}
