package id.andriawan.cofinance.domain.model.request

import id.andriawan.cofinance.utils.enums.TransactionType

data class GetTransactionsParam(
    val startDate: String? = null,
    val endDate: String? = null,
    val isDraft: Boolean = false,
    val transactionId: String? = null,
    val expenseOnly: Boolean? = null
)
