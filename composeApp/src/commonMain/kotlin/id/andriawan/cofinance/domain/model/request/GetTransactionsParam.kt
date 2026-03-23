package id.andriawan.cofinance.domain.model.request

import id.andriawan.cofinance.data.model.request.GetTransactionsRequest

data class GetTransactionsParam(
    val startDate: String? = null,
    val endDate: String? = null,
    val isDraft: Boolean = false,
    val transactionId: String? = null
) {
    companion object {
        fun GetTransactionsParam.toRequest(): GetTransactionsRequest {
            return GetTransactionsRequest(
                startDate = this.startDate,
                endDate = this.endDate,
                isDraft = this.isDraft,
                transactionId = this.transactionId
            )
        }
    }
}
