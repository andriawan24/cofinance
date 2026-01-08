package id.andriawan.cofinance.domain.model.response

import androidx.compose.runtime.Stable

@Stable
data class TransactionByDate(
    val dateLabel: Pair<Int, Int> = Pair(0, 0),
    val totalAmount: Long = 0L,
    val transactions: List<Transaction> = emptyList()
)
