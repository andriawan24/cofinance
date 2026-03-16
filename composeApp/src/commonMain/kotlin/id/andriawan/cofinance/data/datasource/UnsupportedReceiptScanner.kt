package id.andriawan.cofinance.data.datasource

import id.andriawan.cofinance.data.model.response.ReceiptScanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnsupportedReceiptScanner : ReceiptScannerService {

    private val _status = MutableStateFlow<ModelStatus>(
        ModelStatus.Error("Receipt scanning is not available on this platform")
    )

    override suspend fun scanReceipt(image: ByteArray): ReceiptScanResponse {
        throw UnsupportedOperationException("Receipt scanning is only available on mobile devices")
    }

    override suspend fun isModelReady(): Boolean = false

    override fun getModelStatus(): StateFlow<ModelStatus> = _status.asStateFlow()

    override suspend fun downloadModel(onProgress: (Float) -> Unit) {
        throw UnsupportedOperationException("Receipt scanning is only available on mobile devices")
    }
}
