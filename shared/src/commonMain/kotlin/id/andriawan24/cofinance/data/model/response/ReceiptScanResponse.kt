package id.andriawan24.cofinance.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptScanResponse(
    @SerialName("total_price")
    val totalPrice: Long? = null,
    @SerialName("transaction_date")
    val transactionDate: String? = null,
    @SerialName("bank_name")
    val bankName: String? = null,
    @SerialName("transaction_type")
    val transactionType: String? = null,
    val category: String? = null,
    @SerialName("sender_account")
    val sender: BankAccount? = null,
    @SerialName("receiver_account")
    val receiver: BankAccount? = null
) {
    @Serializable
    data class BankAccount(
        val name: String? = null,
        @SerialName("account_number")
        val accountNumber: String? = null
    )
}