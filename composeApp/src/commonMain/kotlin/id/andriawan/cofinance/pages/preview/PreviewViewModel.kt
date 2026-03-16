package id.andriawan.cofinance.pages.preview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.error_generic
import cofinance.composeapp.generated.resources.error_receipt_scan_failed
import id.andriawan.cofinance.data.datasource.ReceiptScannerService
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.domain.usecases.transactions.CreateTransactionUseCase
import id.andriawan.cofinance.domain.usecases.transactions.ScanReceiptUseCase
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.UiText
import id.andriawan.cofinance.utils.compressImage
import id.andriawan.cofinance.utils.enums.TransactionType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class PreviewUiEvent {
    data class NavigateToBalance(val transactionId: String) : PreviewUiEvent()
    data class ShowMessage(val message: UiText) : PreviewUiEvent()
}

data class PreviewUiState(
    val showLoading: Boolean = false,
    val showDownloadPrompt: Boolean = false,
    val downloadProgress: Float? = null,
    val loadingMessage: String? = null
)


@Immutable
class PreviewViewModel(
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val receiptScanner: ReceiptScannerService
) : ViewModel() {

    private val _previewUiState = MutableStateFlow(PreviewUiState())
    val previewUiState = _previewUiState.asStateFlow()

    private val _previewUiEvent = Channel<PreviewUiEvent>(Channel.BUFFERED)
    val previewUiEvent = _previewUiEvent.receiveAsFlow()

    private var pendingFile: ByteArray? = null

    fun scanReceipt(file: ByteArray?) {
        viewModelScope.launch {
            if (file == null) {
                _previewUiEvent.send(
                    PreviewUiEvent.ShowMessage(UiText.Res(Res.string.error_generic))
                )
                return@launch
            }

            if (!receiptScanner.isModelReady()) {
                pendingFile = file
                _previewUiState.update { it.copy(showDownloadPrompt = true) }
                return@launch
            }

            performScan(file)
        }
    }

    fun dismissDownloadPrompt() {
        pendingFile = null
        _previewUiState.update { it.copy(showDownloadPrompt = false) }
    }

    fun startDownloadAndScan() {
        viewModelScope.launch {
            _previewUiState.update {
                it.copy(showDownloadPrompt = false, showLoading = true, downloadProgress = 0f)
            }

            try {
                receiptScanner.downloadModel { progress ->
                    _previewUiState.update { it.copy(downloadProgress = progress) }
                }

                // Download complete, now scan
                _previewUiState.update { it.copy(downloadProgress = null) }
                pendingFile?.let { performScan(it) }
            } catch (e: Exception) {
                _previewUiState.update { it.copy(showLoading = false, downloadProgress = null) }
                _previewUiEvent.send(
                    PreviewUiEvent.ShowMessage(
                        UiText.Raw(e.message ?: "Download failed")
                    )
                )
            } finally {
                pendingFile = null
            }
        }
    }

    private suspend fun performScan(file: ByteArray) {
        _previewUiState.update { it.copy(showLoading = true) }

        val compressed = compressImage(file)
        scanReceiptUseCase.execute(compressed).collectLatest { result ->
            when (result) {
                ResultState.Loading -> {
                    /* no-op */
                }

                is ResultState.Success<ReceiptScan> -> {
                    if (result.data.transactionDate.isBlank()) {
                        _previewUiState.update { it.copy(showLoading = false) }
                        _previewUiEvent.send(
                            PreviewUiEvent.ShowMessage(UiText.Res(Res.string.error_receipt_scan_failed))
                        )
                        return@collectLatest
                    }

                    val input = AddTransactionParam(
                        amount = result.data.totalPrice,
                        category = result.data.category.ifBlank { null },
                        fee = if (result.data.fee > 0) result.data.fee else null,
                        date = result.data.transactionDate,
                        type = TransactionType.DRAFT
                    )

                    handleCreateTransaction(input)
                }

                is ResultState.Error -> {
                    _previewUiState.update { state -> state.copy(showLoading = false) }
                    _previewUiEvent.send(
                        PreviewUiEvent.ShowMessage(
                            result.exception.message?.let { UiText.Raw(it) }
                                ?: UiText.Res(Res.string.error_generic)
                        )
                    )
                }
            }
        }
    }

    private suspend fun handleCreateTransaction(input: AddTransactionParam) {
        createTransactionUseCase.execute(input).collectLatest { result ->
            when (result) {
                ResultState.Loading -> {
                    /* no-op */
                }

                is ResultState.Success<Transaction> -> {
                    _previewUiState.update { state -> state.copy(showLoading = false) }
                    _previewUiEvent.send(
                        PreviewUiEvent.NavigateToBalance(transactionId = result.data.id)
                    )
                }

                is ResultState.Error -> {
                    _previewUiState.update { state -> state.copy(showLoading = false) }
                    _previewUiEvent.send(
                        PreviewUiEvent.ShowMessage(
                            result.exception.message?.let { UiText.Raw(it) }
                                ?: UiText.Res(Res.string.error_generic)
                        )
                    )
                }
            }
        }
    }
}
