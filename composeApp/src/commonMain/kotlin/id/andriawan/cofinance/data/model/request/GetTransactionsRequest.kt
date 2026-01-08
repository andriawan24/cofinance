package id.andriawan.cofinance.data.model.request

data class GetTransactionsRequest(
    val month: Int? = null,
    val year: Int? = null,
    val isDraft: Boolean = false,
    val transactionId: String? = null
)
