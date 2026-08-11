package id.andriawan.cofinance.data.datasource

import com.diamondedge.logging.logging
import id.andriawan.cofinance.data.ocr.OcrEngine
import id.andriawan.cofinance.data.ocr.parser.MerchantCategoryLearning
import id.andriawan.cofinance.data.ocr.parser.ParsedReceipt

interface ReceiptScanner {
    suspend fun scanReceipt(image: ByteArray): ParsedReceipt
}

/**
 * Extracts transaction fields from a receipt image without leaving the device:
 * platform text recognition followed by the shared Indonesian receipt parser.
 *
 * Parsing goes through [MerchantCategoryLearning] rather than [id.andriawan.cofinance.data.ocr.parser.ReceiptParser] directly so
 * that categories the user has previously corrected win over the static dictionary, and so
 * the scanned merchant is remembered for a later correction.
 */
class OnDeviceReceiptScanner(
    private val ocrEngine: OcrEngine,
    private val learning: MerchantCategoryLearning
) : ReceiptScanner {
    override suspend fun scanReceipt(image: ByteArray): ParsedReceipt {
        val result = learning.parse(ocrEngine.recognize(image))

        return result
    }
}
