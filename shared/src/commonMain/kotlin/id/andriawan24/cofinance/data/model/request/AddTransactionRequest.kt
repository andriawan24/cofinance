package id.andriawan24.cofinance.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddTransactionRequest(
    val id: String? = null,
    val amount: Long? = null,
    val category: String? = null,
    val date: String? = null,
    val fee: Long? = null,
    val notes: String? = null,
    @SerialName("users_id")
    val usersId: String? = null,
    @SerialName("accounts_id")
    val accountsId: String? = null,
    @SerialName("receiver_accounts_id")
    val receiverAccountsId: String? = null,
    @SerialName("is_draft")
    val isDraft: Boolean? = null,
    val type: String? = null
)
