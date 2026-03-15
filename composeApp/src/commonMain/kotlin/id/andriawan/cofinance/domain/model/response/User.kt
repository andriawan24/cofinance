package id.andriawan.cofinance.domain.model.response

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.jsonPrimitive

data class User(
    val avatarUrl: String = "",
    val name: String = "",
    val email: String = "",
    val id: String = ""
) {
    companion object {
        fun from(user: UserInfo?): User {
            val metadata = user?.userMetadata
            val customName = metadata?.get("custom_name")?.jsonPrimitive?.content
            val customAvatar = metadata?.get("custom_avatar_url")?.jsonPrimitive?.content
            val googleAvatar = metadata?.get("avatar_url")?.jsonPrimitive?.content

            return User(
                avatarUrl = customAvatar?.ifBlank { null } ?: googleAvatar.orEmpty(),
                name = customName.orEmpty(),
                email = user?.email.orEmpty(),
                id = user?.id.orEmpty()
            )
        }
    }
}
