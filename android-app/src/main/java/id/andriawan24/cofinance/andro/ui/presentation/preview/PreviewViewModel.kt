package id.andriawan24.cofinance.andro.ui.presentation.preview

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.domain.model.request.AddTransactionParam
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import id.andriawan24.cofinance.domain.usecase.transaction.CreateTransactionUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.ScanReceiptUseCase
import id.andriawan24.cofinance.utils.enums.TransactionType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class PreviewUiEvent {
    data class NavigateToBalance(val transactionId: String) : PreviewUiEvent()
    data class ShowMessage(val message: String) : PreviewUiEvent()
}

data class PreviewUiState(
    var showLoading: Boolean = false
)

class PreviewViewModel(
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase
) : ViewModel() {

    private val _previewUiState = MutableStateFlow(PreviewUiState())
    val previewUiState = _previewUiState.asStateFlow()

    private val _previewUiEvent = Channel<PreviewUiEvent>(Channel.BUFFERED)
    val previewUiEvent = _previewUiEvent.receiveAsFlow()

    fun scanReceipt(contentResolver: ContentResolver, imageUri: Uri) {
        viewModelScope.launch {
            _previewUiState.value = previewUiState.value.copy(showLoading = true)

            contentResolver.openInputStream(imageUri)?.use {
                val bytes = it.buffered().readBytes()

                scanReceiptUseCase.execute(bytes).collectLatest { result ->
                    if (result.isSuccess) {
                        val receiptScan = result.getOrNull() ?: ReceiptScan()

                        val input = AddTransactionParam(
                            amount = receiptScan.totalPrice,
                            date = receiptScan.transactionDate,
                            type = TransactionType.EXPENSE.toString(),
                            isDraft = true
                        )

                        createTransactionUseCase.execute(input).collectLatest {
                            if (it.isSuccess) {
                                _previewUiState.value =
                                    previewUiState.value.copy(showLoading = false)
                                _previewUiEvent.send(PreviewUiEvent.NavigateToBalance(transactionId = it.getOrNull()?.id.orEmpty()))
                            }

                            if (it.isFailure) {
                                _previewUiState.value =
                                    previewUiState.value.copy(showLoading = false)
                                Napier.e { "Failed to save transaction ${it.exceptionOrNull()?.message}" }
                            }
                        }
                    }

                    if (result.isFailure) {
                        _previewUiState.value = previewUiState.value.copy(showLoading = false)
                        _previewUiEvent.send(PreviewUiEvent.ShowMessage(result.exceptionOrNull()?.message.orEmpty()))
                    }
                }
            }
        }
    }
}