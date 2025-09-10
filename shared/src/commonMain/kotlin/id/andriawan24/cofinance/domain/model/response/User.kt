package id.andriawan24.cofinance.domain.model.response

import io.github.aakira.napier.Napier
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
            Napier.d { "User data is $user" }
            return User(
                avatarUrl = user?.userMetadata?.getValue("avatar_url")?.jsonPrimitive?.content.orEmpty(),
                name = user?.userMetadata?.getValue("name")?.jsonPrimitive?.content.orEmpty(),
                email = user?.email.orEmpty(),
                id = user?.id.orEmpty()
            )
        }
    }
}
