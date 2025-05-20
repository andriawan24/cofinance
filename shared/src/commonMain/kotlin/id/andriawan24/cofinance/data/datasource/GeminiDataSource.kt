package id.andriawan24.cofinance.data.datasource

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import id.andriawan24.cofinance.data.model.response.ReceiptScanResponse
import kotlinx.serialization.json.Json

class GeminiDataSource(private val model: GenerativeModel, private val json: Json) {
    suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse {
        val inputContent = content { image(image) }
        val response = model.generateContent(inputContent)
        return json.decodeFromString(response.text.orEmpty())
    }
}