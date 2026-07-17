package id.andriawan.cofinance.domain.model.request

import id.andriawan.cofinance.utils.enums.TransactionType

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
)
