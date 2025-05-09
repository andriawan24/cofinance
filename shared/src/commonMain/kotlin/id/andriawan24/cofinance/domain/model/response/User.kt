package id.andriawan24.cofinance.domain.model.response

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.jsonPrimitive

data class User(
    val profileUrl: String,
    val name: String,
    val id: String
) {
    companion object {
        fun from(user: UserInfo?): User {
            return User(
                profileUrl = user?.userMetadata?.getValue("avatar_url")?.jsonPrimitive?.content.orEmpty(),
                name = user?.userMetadata?.getValue("name")?.jsonPrimitive?.content.orEmpty(),
                id = user?.id.orEmpty()
            )
        }
    }
}
