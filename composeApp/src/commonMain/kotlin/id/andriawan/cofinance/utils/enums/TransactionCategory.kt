package id.andriawan.cofinance.utils.enums

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastFilter
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_category_administration
import cofinance.composeapp.generated.resources.ic_category_apparel
import cofinance.composeapp.generated.resources.ic_category_debt
import cofinance.composeapp.generated.resources.ic_category_education
import cofinance.composeapp.generated.resources.ic_category_food
import cofinance.composeapp.generated.resources.ic_category_gift
import cofinance.composeapp.generated.resources.ic_category_health
import cofinance.composeapp.generated.resources.ic_category_housing
import cofinance.composeapp.generated.resources.ic_category_internet
import cofinance.composeapp.generated.resources.ic_category_others
import cofinance.composeapp.generated.resources.ic_category_subscription
import cofinance.composeapp.generated.resources.ic_category_transport
import cofinance.composeapp.generated.resources.ic_chart
import cofinance.composeapp.generated.resources.ic_money
import cofinance.composeapp.generated.resources.label_category_administration
import cofinance.composeapp.generated.resources.label_category_apparel
import cofinance.composeapp.generated.resources.label_category_debt
import cofinance.composeapp.generated.resources.label_category_education
import cofinance.composeapp.generated.resources.label_category_food
import cofinance.composeapp.generated.resources.label_category_gift
import cofinance.composeapp.generated.resources.label_category_health
import cofinance.composeapp.generated.resources.label_category_housing
import cofinance.composeapp.generated.resources.label_category_internet
import cofinance.composeapp.generated.resources.label_category_invest
import cofinance.composeapp.generated.resources.label_category_others
import cofinance.composeapp.generated.resources.label_category_salary
import cofinance.composeapp.generated.resources.label_category_subscription
import cofinance.composeapp.generated.resources.label_category_transport
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class TransactionCategory(
    val color: Color,
    val iconColor: Color,
    val icon: DrawableResource,
    val label: StringResource
) {
    FOOD(
        color = Color(0xFFF0F3FF),
        icon = Res.drawable.ic_category_food,
        label = Res.string.label_category_food,
        iconColor = Color(0xFF6C89FE)
    ),
    TRANSPORT(
        color = Color(0xFFFEEEF1),
        icon = Res.drawable.ic_category_transport,
        label = Res.string.label_category_transport,
        iconColor = Color(0xFFF55376)
    ),
    HOUSING(
        color = Color(0xFFEEF9F8),
        icon = Res.drawable.ic_category_housing,
        label = Res.string.label_category_housing,
        iconColor = Color(0xFF57BEB5)
    ),
    APPAREL(
        color = Color(0xFFFEFAEE),
        icon = Res.drawable.ic_category_apparel,
        label = Res.string.label_category_apparel,
        iconColor = Color(0xFFF5CB53)
    ),
    HEALTH(
        color = Color(0xFFFDF5F1),
        icon = Res.drawable.ic_category_health,
        label = Res.string.label_category_health,
        iconColor = Color(0xFFEB7C55)
    ),
    EDUCATION(
        color = Color(0xFFEFFAFD),
        icon = Res.drawable.ic_category_education,
        label = Res.string.label_category_education,
        iconColor = Color(0xFF5CCFEA)
    ),
    SUBSCRIPTION(
        color = Color(0xFFFFF2F6),
        icon = Res.drawable.ic_category_subscription,
        label = Res.string.label_category_subscription,
        iconColor = Color(0xFFC23053)
    ),
    INTERNET(
        color = Color(0xFFF2F2F2),
        icon = Res.drawable.ic_category_internet,
        label = Res.string.label_category_internet,
        iconColor = Color(0xFF7A7A7A)
    ),
    DEBT(
        color = Color(0xFFF6F4ED),
        icon = Res.drawable.ic_category_debt,
        label = Res.string.label_category_debt,
        iconColor = Color(0xFFA38C4B)
    ),
    GIFT(
        color = Color(0xFFFFF4FD),
        icon = Res.drawable.ic_category_gift,
        label = Res.string.label_category_gift,
        iconColor = Color(0xFFA33594)
    ),
    ADMINISTRATION(
        color = Color(0xFFEDF7EF),
        icon = Res.drawable.ic_category_administration,
        label = Res.string.label_category_administration,
        iconColor = Color(0xFF4BAB63)
    ),
    OTHERS(
        color = Color(0xFFF6EDF9),
        icon = Res.drawable.ic_category_others,
        label = Res.string.label_category_others,
        iconColor = Color(0xFFA146C2)
    ),
    SALARY(
        color = Color(0xFFEDF7EF),
        icon = Res.drawable.ic_money,
        label = Res.string.label_category_salary,
        iconColor = Color(0xFF4BAB63)
    ),
    INVEST(
        color = Color(0xFFFEFAEE),
        icon = Res.drawable.ic_chart,
        label = Res.string.label_category_invest,
        iconColor = Color(0xFFF5CB53)
    );

    companion object Companion {
        fun getCategoryByName(name: String): TransactionCategory {
            return entries.firstOrNull { it.name == name } ?: OTHERS
        }

        fun getExpenseCategories(): List<TransactionCategory> {
            return entries.filter { it != SALARY && it != INVEST }
        }

        fun getIncomeCategories(): List<TransactionCategory> {
            return entries.fastFilter { it == SALARY || it == INVEST }
        }
    }
}
