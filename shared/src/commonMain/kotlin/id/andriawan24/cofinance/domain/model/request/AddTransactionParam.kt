package id.andriawan24.cofinance.domain.model.request

import id.andriawan24.cofinance.data.model.request.AddTransactionRequest
import id.andriawan24.cofinance.utils.enums.TransactionType

data class AddTransactionParam(
    val id: String? = null,
    val amount: Long? = null,
    val category: String? = null,
    val date: String? = null,
    val fee: Long? = null,
    val notes: String? = null,
    val accountsId: String? = null,
    val usersId: String? = null,
    val receiverAccountsId: String? = null,
    val type: TransactionType
) {
    companion object {
        fun AddTransactionParam.toRequest(): AddTransactionRequest {
            return AddTransactionRequest(
                id = this.id,
                amount = this.amount,
                category = this.category,
                date = this.date,
                fee = this.fee,
                notes = this.notes,
                usersId = this.usersId,
                accountsId = this.accountsId,
                receiverAccountsId = this.receiverAccountsId,
                type = this.type.toString()
            )
        }
    }
}
