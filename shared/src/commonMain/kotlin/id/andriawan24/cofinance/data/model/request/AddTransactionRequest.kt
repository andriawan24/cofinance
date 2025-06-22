package id.andriawan24.cofinance.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddTransactionRequest(
    val amount: Long,
    val category: String,
    val date: String,
    val fee: Long,
    val notes: String,
    @SerialName("users_id")
    val usersId: String,
    @SerialName("accounts_id")
    val accountsId: String,
    @SerialName("is_draft")
    val isDraft: Boolean
)
