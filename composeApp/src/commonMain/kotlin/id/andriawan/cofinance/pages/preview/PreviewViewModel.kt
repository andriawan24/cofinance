package id.andriawan.cofinance.pages.preview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.domain.model.request.AddTransactionParam
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.domain.usecases.transactions.CreateTransactionUseCase
import id.andriawan.cofinance.domain.usecases.transactions.ScanReceiptUseCase
import id.andriawan.cofinance.utils.ResultState
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

    fun scanReceipt(file: ByteArray?) {
        viewModelScope.launch {
            _previewUiState.update { state -> state.copy(showLoading = true) }

            if (file == null) {
                return@launch
            }

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
                                PreviewUiEvent.ShowMessage("Couldn't read the receipt image, please try again")
                            )
                            return@collectLatest
                        }

                        val input = AddTransactionParam(
                            amount = result.data.totalPrice,
                            date = result.data.transactionDate,
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
                        PreviewUiEvent.ShowMessage(result.exception.message.orEmpty())
                    )
                }
            }
        }
    }
}
