package id.andriawan.cofinance.utils.enums

import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.label_expenses
import cofinance.composeapp.generated.resources.label_income
import cofinance.composeapp.generated.resources.label_transfer
import org.jetbrains.compose.resources.StringResource

enum class TransactionTabType(val labelRes: StringResource) {
    EXPENSES(Res.string.label_expenses),
    INCOME(Res.string.label_income),
    TRANSFER(Res.string.label_transfer);

    companion object {
        fun getByIndex(index: Int): TransactionTabType {
            return entries.firstOrNull { it.ordinal == index } ?: EXPENSES
        }
    }
}
