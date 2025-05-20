package id.andriawan24.cofinance.andro.ui.presentation.preview

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import id.andriawan24.cofinance.domain.usecase.transaction.ScanReceiptUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PreviewUiEvent {
    data class NavigateToBalance(val result: ReceiptScan) : PreviewUiEvent()
    data class ShowMessage(val message: String) : PreviewUiEvent()
}

data class PreviewUiState(
    var showLoading: Boolean = false
)

class PreviewViewModel(private val scanReceiptUseCase: ScanReceiptUseCase) : ViewModel() {

    private val _previewUiState = MutableStateFlow(PreviewUiState())
    val previewUiState = _previewUiState.asStateFlow()

    private val _previewUiEvent = Channel<PreviewUiEvent>(Channel.BUFFERED)
    val previewUiEvent = _previewUiEvent.receiveAsFlow()

    fun scanReceipt(contentResolver: ContentResolver, imageUri: Uri) {
        viewModelScope.launch {
            _previewUiState.update { it.copy(showLoading = true) }

            contentResolver.openInputStream(imageUri)?.use {
                val bytes = it.buffered().readBytes()

                scanReceiptUseCase.execute(bytes).collectLatest { result ->
                    _previewUiState.update { state -> state.copy(showLoading = false) }

                    if (result.isSuccess) {
                        val receiptScan = result.getOrNull()
                        if (receiptScan != null) {
                            _previewUiEvent.send(PreviewUiEvent.NavigateToBalance(result = receiptScan))
                        } else {
                            _previewUiEvent.send(PreviewUiEvent.ShowMessage(message = "Cannot send receipt"))
                        }
                    }

                    if (result.isFailure) {
                        _previewUiEvent.send(PreviewUiEvent.ShowMessage(result.exceptionOrNull()?.message.orEmpty()))
                    }
                }
            }
        }
    }
}