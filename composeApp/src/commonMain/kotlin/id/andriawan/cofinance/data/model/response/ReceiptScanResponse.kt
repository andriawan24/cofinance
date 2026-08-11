package id.andriawan.cofinance.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptScanResponse(
    val totalPrice: Long? = null,
    val transactionDate: String? = null,
    val bankName: String? = null,
    val fee: Long? = null,
    val transactionType: String? = null,
    val category: String? = null,
    val sender: BankAccount? = null,
    val receiver: BankAccount? = null
) {
    @Serializable
    data class BankAccount(
        val name: String? = null,
        val accountNumber: String? = null
    )
}
