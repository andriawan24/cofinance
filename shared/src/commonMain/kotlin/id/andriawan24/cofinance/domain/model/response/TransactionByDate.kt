package id.andriawan24.cofinance.domain.model.response

data class TransactionByDate(
    val dateLabel: Pair<Int, Int> = Pair(0, 0),
    val totalAmount: Long = 0L,
    val transactions: List<Transaction> = emptyList()
)