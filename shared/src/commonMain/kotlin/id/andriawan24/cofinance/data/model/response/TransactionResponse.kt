package id.andriawan24.cofinance.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponse(
    val id: String? = null,
    val amount: Long? = null,
    val category: String? = null,
    val date: String? = null,
    val fee: Long? = null,
    val notes: String? = null,
    @SerialName("accounts_id")
    val senderAccountId: String? = null,
    @SerialName("receiver_accounts_id")
    val receiverAccountId: String? = null,
    @SerialName(CREATED_AT_FIELD)
    val createdAt: String? = null,
    @SerialName(UPDATED_AT_FIELD)
    val updatedAt: String? = null,
    val type: String? = null,
    @SerialName(IS_DRAFT_FIELD)
    val isDraft: Boolean? = null,

    val sender: AccountResponse? = null,
    val receiver: AccountResponse? = null,
) {
    companion object {
        const val TABLE_NAME = "transactions"
        const val ID_FIELD = "id"
        const val ACCOUNT_FIELD = "sender"
        const val RECEIVER_ACCOUNT_FIELD = "receiver"
        const val DATE_FIELD = "date"
        const val CREATED_AT_FIELD = "created_at"
        const val UPDATED_AT_FIELD = "updated_at"
        const val IS_DRAFT_FIELD = "is_draft"
    }
}
