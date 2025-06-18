package id.andriawan24.cofinance.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponse(
    val amount: Long,
    val category: String,
    val date: String,
    val fee: Long,
    val notes: String,
    @SerialName("users_id")
    val usersId: String,
    @SerialName("accounts_id")
    val accountsId: Int,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    companion object {
        const val TABLE_NAME = "transactions"
    }
}
