package id.andriawan24.cofinance.andro.ui.presentation.addexpenses.models

enum class ExpensesType(val index: Int, val label: String) {
    EXPENSES(0, "Expenses"),
    INCOME(1, "Income"),
    TRANSFER(2, "Transfer");

    companion object {
        fun getByIndex(index: Int): ExpensesType {
            return ExpensesType.entries.firstOrNull { it.index == index } ?: EXPENSES
        }
    }
}