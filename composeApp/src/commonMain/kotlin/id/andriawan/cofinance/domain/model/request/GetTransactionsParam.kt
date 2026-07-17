package id.andriawan.cofinance.domain.model.request

data class GetTransactionsParam(
    val startDate: String? = null,
    val endDate: String? = null,
    val isDraft: Boolean = false,
    val transactionId: String? = null
)
