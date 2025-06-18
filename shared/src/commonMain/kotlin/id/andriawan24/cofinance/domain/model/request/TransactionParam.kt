package id.andriawan24.cofinance.domain.model.request

import id.andriawan24.cofinance.data.model.request.TransactionRequest

data class TransactionParam(
    val amount: Long,
    val category: String,
    val date: String,
    val fee: Long,
    val notes: String,
    val usersId: String = "",
    val accountsId: Int,
) {
    companion object {
        fun TransactionParam.toRequest(): TransactionRequest {
            return TransactionRequest(
                amount = this.amount,
                category = this.category,
                date = this.date,
                fee = this.fee,
                notes = this.notes,
                usersId = this.usersId,
                accountsId = this.accountsId
            )
        }
    }
}
