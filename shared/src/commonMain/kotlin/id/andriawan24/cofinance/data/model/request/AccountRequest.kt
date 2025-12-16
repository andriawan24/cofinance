package id.andriawan24.cofinance.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountRequest(
    val name: String,
    val balance: Long,
    val group: String,
    @SerialName("users_id")
    val usersId: String = ""
)
