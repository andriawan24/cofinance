package id.andriawan24.cofinance.data.model.response

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountResponse(
    val id: String? = null,
    val name: String? = null,
    val group: String? = null,
    val balance: Int? = null,
    @SerialName(USERS_FIELD)
    val user: UserInfo? = null,
    @SerialName(CREATED_AT_FIELD)
    val createdAt: String? = null
) {
    companion object {
        const val TABLE_NAME = "accounts"
        const val CREATED_AT_FIELD = "created_at"
        const val USERS_FIELD = "users"
    }
}
