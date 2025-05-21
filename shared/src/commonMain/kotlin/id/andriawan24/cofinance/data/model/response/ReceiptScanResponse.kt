package id.andriawan24.cofinance.data.model.response

import id.andriawan24.cofinance.utils.GeminiHelper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptScanResponse(
    @SerialName(GeminiHelper.TOTAL_PRICE_FIELD)
    val totalPrice: Long? = null,
    @SerialName(GeminiHelper.TRANSACTION_DATE_FIELD)
    val transactionDate: String? = null,
    @SerialName(GeminiHelper.BANK_NAME_FIELD)
    val bankName: String? = null,
    @SerialName(GeminiHelper.TRANSACTION_TYPE_FIELD)
    val transactionType: String? = null,
    val category: String? = null,
    @SerialName(GeminiHelper.SENDER_ACCOUNT_FIELD)
    val sender: BankAccount? = null,
    @SerialName(GeminiHelper.RECEIVER_ACCOUNT_FIELD)
    val receiver: BankAccount? = null
) {
    @Serializable
    data class BankAccount(
        val name: String? = null,
        @SerialName(GeminiHelper.SENDER_ACCOUNT_NUMBER_FIELD)
        val accountNumber: String? = null
    )
}