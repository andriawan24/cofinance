package id.andriawan.cofinance.data.ocr.parser

import id.andriawan.cofinance.data.local.merchant.MerchantCategoryLocalDataSource
import id.andriawan.cofinance.data.ocr.OcrResult

/**
 * Bridges the device-local association store and the pure [ReceiptParser].
 *
 * The parser stays synchronous and Room-free: this layer resolves the stored associations
 * before a parse and hands them in, and records a correction afterwards.
 *
 * ### Recording a correction
 *
 * [recordCategoryCorrection] is the single entry point the UI layer calls. Call it when
 * the user changes the category on a draft that came from a scan, passing the category
 * the user picked (a `TransactionCategory.name`). It keys the association to the merchant
 * of the most recent scan, remembered by [parse]; when the draft did not come from a scan,
 * or the receipt header yielded no key, the call is a no-op.
 *
 * The remembered key is in-memory and per-process, which matches the flow: a scan opens
 * exactly one draft for review before the next scan can start.
 */
class MerchantCategoryLearning(
    private val store: MerchantCategoryLocalDataSource,
    private val parser: ReceiptParser = ReceiptParser()
) {
    private var lastScannedMerchantKey: String? = null

    /** Parses [result] with learned associations applied ahead of the static dictionary. */
    suspend fun parse(result: OcrResult): ParsedReceipt {
        lastScannedMerchantKey = MerchantKey.from(result)
        return parser.parse(result, store.getAssociations())
    }

    /** Records the user's category choice against the merchant of the most recent scan. */
    suspend fun recordCategoryCorrection(category: String) {
        val merchantKey = lastScannedMerchantKey ?: return
        store.recordAssociation(merchantKey, category)
    }
}
