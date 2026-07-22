package id.andriawan.cofinance.data.datasource

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import kotlinx.serialization.json.Json

interface ReceiptScanner {
    suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse
}

class GeminiDataSource(private val model: GenerativeModel, private val json: Json) : ReceiptScanner {
    override suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse {
        val inputContent = content { image(image) }
        val response = model.generateContent(inputContent)
        return json.decodeFromString(response.text.orEmpty())
    }
}
