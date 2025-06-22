package id.andriawan24.cofinance.domain.model.response

import id.andriawan24.cofinance.data.model.response.TransactionResponse
import id.andriawan24.cofinance.utils.enums.TransactionType

data class Transaction(
    val id: String = "",
    val amount: Long = 0,
    val category: String = "",
    val date: String = "",
    val fee: Long = 0,
    val notes: String = "",
    val account: Account = Account(),
    val type: TransactionType = TransactionType.EXPENSE,
) {
    companion object {
        fun from(response: TransactionResponse): Transaction {
            return Transaction(
                id = response.id.orEmpty(),
                category = response.category.orEmpty(),
                amount = response.amount ?: 0,
                date = response.date.orEmpty(),
                fee = response.fee ?: 0,
                notes = response.notes.orEmpty(),
                account = Account.from(response.account),
                type = TransactionType.valueOf(response.type.orEmpty())
            )
        }
    }
}
