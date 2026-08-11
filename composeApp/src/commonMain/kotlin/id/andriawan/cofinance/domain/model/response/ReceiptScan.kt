package id.andriawan.cofinance.domain.model.response

import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import id.andriawan.cofinance.data.ocr.parser.ParsedReceipt
import id.andriawan.cofinance.data.ocr.parser.ReceiptField
import id.andriawan.cofinance.data.ocr.parser.ReceiptParser
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptScan(
    val totalPrice: Long = 0,
    val transactionDate: String = "",
    val bankName: String = "",
    val transactionType: String = "",
    val category: String = "",
    val fee: Long = 0,
    val sender: BankAccount = BankAccount(),
    val receiver: BankAccount = BankAccount(),
    val confidence: Map<ReceiptField, Float> = emptyMap()
) {
    /** Fields the extraction was unsure about, to be flagged on the review screen. */
    val lowConfidenceFields: Set<ReceiptField>
        get() = confidence
            .filterValues { it < ReceiptParser.LOW_CONFIDENCE_THRESHOLD }
            .keys

    @Serializable
    data class BankAccount(
        val name: String = "",
        val accountNumber: String = ""
    )

    companion object {
        fun from(parsed: ParsedReceipt): ReceiptScan = from(parsed.response, parsed.confidence)

        fun from(
            response: ReceiptScanResponse,
            confidence: Map<ReceiptField, Float> = emptyMap()
        ): ReceiptScan {
            return ReceiptScan(
                totalPrice = response.totalPrice ?: 0L,
                transactionDate = response.transactionDate.orEmpty(),
                bankName = response.bankName.orEmpty(),
                transactionType = response.transactionType.orEmpty(),
                category = response.category.orEmpty(),
                fee = response.fee ?: 0L,
                sender = BankAccount(
                    name = response.sender?.name.orEmpty(),
                    accountNumber = response.sender?.accountNumber.orEmpty()
                ),
                receiver = BankAccount(
                    name = response.receiver?.name.orEmpty(),
                    accountNumber = response.receiver?.accountNumber.orEmpty()
                ),
                confidence = confidence
            )
        }
    }
}
