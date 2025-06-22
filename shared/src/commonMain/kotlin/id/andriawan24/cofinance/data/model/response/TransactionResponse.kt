package id.andriawan24.cofinance.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponse(
    val id: String,
    val amount: Long,
    val category: String,
    val date: String,
    val fee: Long,
    val notes: String,
    @SerialName("accounts")
    val account: AccountResponse,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val type: String
) {
    companion object {
        const val TABLE_NAME = "transactions"
        const val DATE_FIELD = "date"
    }
}
