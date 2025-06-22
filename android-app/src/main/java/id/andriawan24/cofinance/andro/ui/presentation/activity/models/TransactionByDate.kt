package id.andriawan24.cofinance.andro.ui.presentation.activity.models

import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.domain.model.response.Transaction

data class TransactionByDate(
    val dateLabel: String = emptyString(),
    val totalAmount: Long = 0L,
    val transactions: List<Transaction> = emptyList()
)
