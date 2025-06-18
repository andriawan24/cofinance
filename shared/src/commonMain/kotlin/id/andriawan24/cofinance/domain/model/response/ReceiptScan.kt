package id.andriawan24.cofinance.domain.model.response

import id.andriawan24.cofinance.data.model.response.ReceiptScanResponse
import id.andriawan24.cofinance.utils.getCurrentLocalDateTime
import id.andriawan24.cofinance.utils.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptScan(
    val totalPrice: Long = 0,
    val transactionDate: LocalDateTime = getCurrentLocalDateTime(),
    val bankName: String = "",
    val transactionType: String = "",
    val category: String = "",
    val fee: Long = 0,
    val sender: BankAccount = BankAccount(),
    val receiver: BankAccount = BankAccount()
) {
    @Serializable
    data class BankAccount(
        val name: String = "",
        val accountNumber: String = ""
    )

    companion object {
        fun from(response: ReceiptScanResponse): ReceiptScan {
            return ReceiptScan(
                totalPrice = response.totalPrice ?: 0L,
                transactionDate = response.transactionDate.orEmpty().toLocalDateTime(),
                bankName = response.bankName.orEmpty(),
                transactionType = response.transactionType.orEmpty(),
                category = response.category.orEmpty(),
                sender = BankAccount(
                    name = response.sender?.name.orEmpty(),
                    accountNumber = response.sender?.accountNumber.orEmpty()
                ),
                receiver = BankAccount(
                    name = response.receiver?.name.orEmpty(),
                    accountNumber = response.receiver?.accountNumber.orEmpty()
                )
            )
        }
    }
}