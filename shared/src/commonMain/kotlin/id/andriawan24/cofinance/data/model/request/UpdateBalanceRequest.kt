package id.andriawan24.cofinance.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateBalanceRequest(
    val amount: Long,
    @SerialName("id")
    val accountsId: String,
    @SerialName("users_id")
    val usersId: String = ""
)
