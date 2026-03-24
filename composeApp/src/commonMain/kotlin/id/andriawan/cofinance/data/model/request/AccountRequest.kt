package id.andriawan.cofinance.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountRequest(
    val name: String,
    val balance: Long,
    val group: String,
    @SerialName("account_type")
    val accountType: String = "REGULAR_BALANCE",
    @SerialName("users_id")
    val usersId: String = ""
)
