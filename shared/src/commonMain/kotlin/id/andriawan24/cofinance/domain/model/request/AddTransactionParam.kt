package id.andriawan24.cofinance.domain.model.request

import id.andriawan24.cofinance.data.model.request.AddTransactionRequest

data class AddTransactionParam(
    val amount: Long = 0,
    val category: String = "",
    val date: String = "",
    val fee: Long = 0,
    val notes: String = "",
    val usersId: String = "",
    val accountsId: String = "",
    val isDraft: Boolean = false
) {
    companion object {
        fun AddTransactionParam.toRequest(): AddTransactionRequest {
            return AddTransactionRequest(
                amount = this.amount,
                category = this.category,
                date = this.date,
                fee = this.fee,
                notes = this.notes,
                usersId = this.usersId,
                accountsId = this.accountsId,
                isDraft = this.isDraft
            )
        }
    }
}
