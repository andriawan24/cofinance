package id.andriawan.cofinance.domain.model.response

import id.andriawan.cofinance.data.model.response.FirebaseUserResponse

data class User(
    val avatarUrl: String = "",
    val name: String = "",
    val email: String = "",
    val id: String = "",
    val cycleStartDay: Int = 1,
    val lastCycleResetDate: String? = null
) {
    companion object {
        fun from(user: FirebaseUserResponse?): User {
            return User(
                avatarUrl = user?.avatarUrl.orEmpty(),
                name = user?.name.orEmpty(),
                email = user?.email.orEmpty(),
                id = user?.id.orEmpty(),
                cycleStartDay = user?.cycleStartDay?.coerceIn(1, 28) ?: 1,
                lastCycleResetDate = user?.lastCycleResetDate
            )
        }
    }
}
