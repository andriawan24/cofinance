package id.andriawan24.cofinance.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountResponse(
    val id: Int? = null,
    val name: String? = null,
    val group: String? = null,
    val balance: Int? = null,
    @SerialName("users_id")
    val usersId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    companion object {
        const val TABLE_NAME = "accounts"
    }
}
