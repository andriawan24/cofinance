package id.andriawan24.cofinance.domain.model.response

import id.andriawan24.cofinance.data.model.response.TransactionResponse

data class Transaction(
    val amount: Long,
    val category: String,
    val date: String,
    val fee: Long,
    val notes: String,
    val usersId: String,
    val accountsId: Int,
) {
    companion object {
        fun from(response: TransactionResponse): Transaction {
            return Transaction(
                category = response.category,
                amount = response.amount,
                date = response.date,
                fee = response.fee,
                notes = response.notes,
                usersId = response.usersId,
                accountsId = response.accountsId
            )
        }
    }
}
