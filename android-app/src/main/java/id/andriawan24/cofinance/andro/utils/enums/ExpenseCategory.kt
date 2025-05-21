package id.andriawan24.cofinance.andro.utils.enums

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import id.andriawan24.cofinance.andro.R

enum class ExpenseCategory(val color: Color, @DrawableRes val iconRes: Int, val label: String) {
    FOOD(Color(0xFFF0F3FF), R.drawable.ic_category_food, "Food"),
    TRANSPORT(Color(0xFFFEEEF1), R.drawable.ic_category_transport, "Transport"),
    HOUSING(Color(0xFFEEF9F8), R.drawable.ic_category_housing, "Housing"),
    APPAREL(Color(0xFFFEFAEE), R.drawable.ic_category_apparel, "Apparel"),
    HEALTH(Color(0xFFFDF5F1), R.drawable.ic_category_health, "Health"),
    EDUCATION(Color(0xFFEFFAFD), R.drawable.ic_category_education, "Education"),
    SUBSCRIPTION(Color(0xFFFFF2F6), R.drawable.ic_category_subscription, "Subscription"),
    INTERNET(Color(0xFFF2F2F2), R.drawable.ic_category_internet, "Internet"),
    DEBT(Color(0xFFF6F4ED), R.drawable.ic_category_debt, "Debt"),
    GIFT(Color(0xFFFFF4FD), R.drawable.ic_category_gift, "Gift"),
    ADMINISTRATION(Color(0xFFEDF7EF), R.drawable.ic_category_administration, "Administration"),
    OTHERS(Color(0xFFF6EDF9), R.drawable.ic_category_others, "Others")
}
