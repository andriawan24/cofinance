package id.andriawan24.cofinance.domain.model.response

import id.andriawan24.cofinance.data.model.response.TransactionResponse
import id.andriawan24.cofinance.utils.enums.TransactionType

data class Transaction(
    val id: String,
    val amount: Long,
    val category: String,
    val date: String,
    val fee: Long,
    val notes: String,
    val account: Account,
    val type: TransactionType,
) {
    companion object {
        fun from(response: TransactionResponse): Transaction {
            return Transaction(
                id = response.id,
                category = response.category,
                amount = response.amount,
                date = response.date,
                fee = response.fee,
                notes = response.notes,
                account = Account.from(response.account),
                type = TransactionType.valueOf(response.type)
            )
        }
    }
}
