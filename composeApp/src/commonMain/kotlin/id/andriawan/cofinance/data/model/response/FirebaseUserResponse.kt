package id.andriawan.cofinance.data.model.response

data class FirebaseUserResponse(
    val avatarUrl: String = "",
    val name: String = "",
    val email: String = "",
    val id: String = "",
    val cycleStartDay: Int = 1,
    val lastCycleResetDate: String? = null
)
