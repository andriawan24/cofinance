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
    @SerialName("users")
    val user: UserInfo? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    companion object {
        const val TABLE_NAME = "accounts"
    }
}
