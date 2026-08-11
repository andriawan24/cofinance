package id.andriawan.cofinance.pages.preview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.error_generic
import cofinance.composeapp.generated.resources.error_receipt_scan_failed
import id.andriawan.cofinance.data.ocr.parser.ReceiptField
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.usecases.transactions.CreateTransactionUseCase
import id.andriawan.cofinance.domain.usecases.transactions.ScanReceiptUseCase
import id.andriawan.cofinance.utils.UiText
import id.andriawan.cofinance.utils.collectResult
import id.andriawan.cofinance.utils.compressImage
import id.andriawan.cofinance.utils.enums.TransactionType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class PreviewUiEvent {
    data class NavigateToBalance(
        val transactionId: String,
        val lowConfidenceFields: Set<ReceiptField> = emptySet()
    ) : PreviewUiEvent()

    data class ShowMessage(val message: UiText) : PreviewUiEvent()
}

data class PreviewUiState(var showLoading: Boolean = false)


@Immutable
class PreviewViewModel(
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase
) : ViewModel() {

    private val _previewUiState = MutableStateFlow(PreviewUiState())
    val previewUiState = _previewUiState.asStateFlow()

    private val _previewUiEvent = Channel<PreviewUiEvent>(Channel.BUFFERED)
    val previewUiEvent = _previewUiEvent.receiveAsFlow()

    fun scanReceipt(file: ByteArray?) {
        viewModelScope.launch {
            _previewUiState.update { state -> state.copy(showLoading = true) }

            if (file == null) return@launch

            val compressed = compressImage(file)
            scanReceiptUseCase.execute(compressed).collectResult(
                onSuccess = { data ->
                    if (data.transactionDate.isBlank()) {
                        _previewUiState.update { it.copy(showLoading = false) }
                        _previewUiEvent.send(
                            PreviewUiEvent.ShowMessage(UiText.Res(Res.string.error_receipt_scan_failed))
                        )
                        return@collectResult
                    }

                    val input = AddTransactionParam(
                        amount = data.totalPrice,
                        category = data.category.ifBlank { null },
                        fee = if (data.fee > 0) data.fee else null,
                        date = data.transactionDate,
                        type = TransactionType.DRAFT
                    )

                    createTransaction(input, data.lowConfidenceFields)
                },
                onError = { exception ->
                    _previewUiState.update { state -> state.copy(showLoading = false) }
                    _previewUiEvent.send(
                        PreviewUiEvent.ShowMessage(
                            exception.message?.let { UiText.Raw(it) }
                                ?: UiText.Res(Res.string.error_generic)
                        )
                    )
                }
            )
        }
    }

    private suspend fun createTransaction(
        input: AddTransactionParam,
        lowConfidenceFields: Set<ReceiptField>
    ) {
        createTransactionUseCase.execute(input).collectResult(
            onSuccess = { data ->
                _previewUiState.update { state -> state.copy(showLoading = false) }
                _previewUiEvent.send(
                    PreviewUiEvent.NavigateToBalance(
                        transactionId = data.id,
                        lowConfidenceFields = lowConfidenceFields
                    )
                )
            },
            onError = { exception ->
                _previewUiState.update { state -> state.copy(showLoading = false) }
                _previewUiEvent.send(
                    PreviewUiEvent.ShowMessage(
                        exception.message?.let { UiText.Raw(it) }
                            ?: UiText.Res(Res.string.error_generic)
                    )
                )
            }
        )
    }
}
