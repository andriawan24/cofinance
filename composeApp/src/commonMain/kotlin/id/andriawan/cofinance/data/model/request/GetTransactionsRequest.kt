package id.andriawan.cofinance.data.model.request

data class GetTransactionsRequest(
    val startDate: String? = null,
    val endDate: String? = null,
    val isDraft: Boolean = false,
    val transactionId: String? = null
)
