package id.andriawan24.cofinance.andro.ui.presentation.addnew.models

import androidx.annotation.StringRes
import id.andriawan24.cofinance.andro.R

enum class TransactionTabType(@param:StringRes val labelResId: Int) {
    EXPENSES(R.string.label_expenses),
    INCOME(R.string.label_income),
    TRANSFER(R.string.label_transfer);

    companion object {
        fun getByIndex(index: Int): TransactionTabType {
            return TransactionTabType.entries.firstOrNull { it.ordinal == index } ?: EXPENSES
        }
    }
}