package id.andriawan24.cofinance.domain.model.request

import id.andriawan24.cofinance.data.model.request.GetTransactionsRequest

data class GetTransactionsParam(
    val month: Int? = null,
    val year: Int? = null,
    val isDraft: Boolean = false,
    val transactionId: String? = null
) {
    companion object {
        fun GetTransactionsParam.toRequest(): GetTransactionsRequest {
            return GetTransactionsRequest(
                month = this.month,
                year = this.year,
                isDraft = this.isDraft,
                transactionId = this.transactionId
            )
        }
    }
}