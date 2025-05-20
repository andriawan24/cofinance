package id.andriawan24.cofinance.domain.model.response

import id.andriawan24.cofinance.data.model.response.ReceiptScanResponse

data class ReceiptScan(
    val totalPrice: Long = 0,
    val transactionDate: String = "",
    val bankName: String = "",
    val transactionType: String = "",
    val category: String = "",
    val sender: BankAccount = BankAccount(),
    val receiver: BankAccount = BankAccount()
) {
    data class BankAccount(
        val name: String = "",
        val accountNumber: String = ""
    )

    companion object {
        fun from(response: ReceiptScanResponse): ReceiptScan {
            return ReceiptScan(
                totalPrice = response.totalPrice ?: 0L,
                transactionDate = response.transactionDate.orEmpty(),
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