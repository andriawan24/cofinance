package id.andriawan24.cofinance.andro.ui.presentation.preview

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.domain.model.request.AddTransactionParam
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import id.andriawan24.cofinance.domain.model.response.Transaction
import id.andriawan24.cofinance.domain.usecase.transaction.CreateTransactionUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.ScanReceiptUseCase
import id.andriawan24.cofinance.utils.ResultState
import id.andriawan24.cofinance.utils.enums.TransactionType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PreviewUiEvent {
    data class NavigateToBalance(val transactionId: String) : PreviewUiEvent()
    data class ShowMessage(val message: String) : PreviewUiEvent()
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

    fun scanReceipt(contentResolver: ContentResolver, imageUri: Uri) {
        viewModelScope.launch {
            _previewUiState.update { state -> state.copy(showLoading = true) }

            contentResolver.openInputStream(imageUri)?.use { uploadedImage ->
                val bytes = uploadedImage.buffered().readBytes()

                scanReceiptUseCase.execute(bytes).collectLatest { result ->
                    when (result) {
                        ResultState.Loading -> {
                            /* no-op */
                        }

                        is ResultState.Success<ReceiptScan> -> {
                            if (result.data?.transactionDate?.isBlank() == true) {
                                _previewUiState.update { it.copy(showLoading = false) }
                                _previewUiEvent.send(
                                    PreviewUiEvent.ShowMessage("Couldn't read the receipt image, please try again")
                                )
                                return@collectLatest
                            }

                            val input = AddTransactionParam(
                                amount = result.data?.totalPrice ?: 0,
                                date = result.data?.transactionDate.orEmpty(),
                                type = TransactionType.DRAFT
                            )

                            handleCreateTransaction(input)
                        }

                        is ResultState.Error -> {
                            _previewUiState.update { state -> state.copy(showLoading = false) }
                            _previewUiEvent.send(
                                PreviewUiEvent.ShowMessage(result.exception.message.orEmpty())
                            )
                        }
                    }
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
                        PreviewUiEvent.NavigateToBalance(transactionId = result.data?.id.orEmpty())
                    )
                }

                is ResultState.Error -> {
                    Napier.e(result.exception) { "Failed to create new transaction" }
                    _previewUiState.update { state -> state.copy(showLoading = false) }
                    _previewUiEvent.send(
                        PreviewUiEvent.ShowMessage(result.exception.message.orEmpty())
                    )
                }
            }
        }
    }
}