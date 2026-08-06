package id.andriawan.cofinance.data.model.document

import kotlinx.serialization.Serializable

@Serializable
data class AccountDocument(
    val name: String = "",
    val group: String = "",
    val balance: Long = 0,
    val accountType: String = "",
    val createdAt: String = ""
)
