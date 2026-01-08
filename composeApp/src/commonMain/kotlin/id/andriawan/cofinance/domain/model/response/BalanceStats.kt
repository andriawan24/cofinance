package id.andriawan.cofinance.domain.model.response

data class BalanceStats(
    val balance: Long,
    val income: Long,
    val expenses: Long
)
