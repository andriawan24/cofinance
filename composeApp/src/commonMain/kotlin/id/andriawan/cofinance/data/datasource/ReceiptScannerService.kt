package id.andriawan.cofinance.data.datasource

import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import kotlinx.coroutines.flow.StateFlow

interface ReceiptScannerService {
    suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse
    suspend fun isModelReady(): Boolean
    fun getModelStatus(): StateFlow<ModelStatus>
    suspend fun downloadModel(onProgress: (Float) -> Unit = {})
}

sealed class ModelStatus {
    data object NotDownloaded : ModelStatus()
    data class Downloading(val progress: Float) : ModelStatus()
    data object Ready : ModelStatus()
    data object LoadingModel : ModelStatus()
    data object Inferring : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}
