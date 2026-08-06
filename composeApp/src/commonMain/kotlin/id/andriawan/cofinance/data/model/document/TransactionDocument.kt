package id.andriawan.cofinance.data.model.document

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDocument(
    val amount: Long = 0,
    val category: String = "",
    val date: String = "",
    val fee: Long = 0,
    val notes: String = "",
    val senderAccountId: String = "",
    val receiverAccountId: String? = null,
    val type: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
