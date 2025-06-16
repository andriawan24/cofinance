package id.andriawan24.cofinance.andro.ui.presentation.addnew.models

import androidx.annotation.StringRes
import id.andriawan24.cofinance.andro.R

enum class ExpensesType(val index: Int, @StringRes val labelResId: Int) {
    EXPENSES(0, R.string.label_expenses),
    INCOME(1, R.string.label_income),
    TRANSFER(2, R.string.label_transfer);

    companion object {
        fun getByIndex(index: Int): ExpensesType {
            return ExpensesType.entries.firstOrNull { it.index == index } ?: EXPENSES
        }
    }
}