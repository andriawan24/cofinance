package id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.andro.utils.enums.ExpenseCategory
import id.andriawan24.cofinance.domain.model.response.Account
import id.andriawan24.cofinance.domain.model.response.ReceiptScan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

data class ExpenseUiState(
    var amount: String = emptyString(),
    var fee: String = emptyString(),
    var includeFee: Boolean = false,
    var imageUri: Uri? = null,
    var account: Account? = null,
    var category: ExpenseCategory? = null,
    var dateTime: Date = Date(),
    var notes: String = emptyString(),
    var isValid: Boolean = false
)

sealed class ExpensesUiEvent {
    data class SetIncludeFee(val includeFee: Boolean) : ExpensesUiEvent()
    data class SetAmount(val amount: String) : ExpensesUiEvent()
    data class SetCategory(val category: ExpenseCategory) : ExpensesUiEvent()
    data class SetAccount(val account: Account) : ExpensesUiEvent()
    data class SetDateTime(val dateTime: Date) : ExpensesUiEvent()
    data class SetFee(val fee: String) : ExpensesUiEvent()
    data class SetNote(val note: String) : ExpensesUiEvent()
}

class ExpenseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState = _uiState.asStateFlow()

    fun init(receiptScan: ReceiptScan, onDateTime: (Date) -> Unit) {
        var date = Date()
        if (receiptScan.totalPrice > 0) {
            date = getDateTimeFromIsoString(receiptScan.transactionDate)
            _uiState.update {
                it.copy(
                    amount = receiptScan.totalPrice.toString(),
                    fee = receiptScan.fee.toString(),
                    includeFee = receiptScan.fee > 0,
                    dateTime = date
                )
            }
        }
        onDateTime(date)
    }

    private fun getDateTimeFromIsoString(dateTime: String): Date {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val offsetDateTime = OffsetDateTime.parse(dateTime, formatter)
        return Date.from(offsetDateTime.toInstant())
    }

    fun onEvent(event: ExpensesUiEvent) {
        when (event) {
            is ExpensesUiEvent.SetIncludeFee -> _uiState.update {
                it.copy(
                    includeFee = event.includeFee,
                    fee = if (!event.includeFee) emptyString() else it.fee
                )
            }

            is ExpensesUiEvent.SetAmount -> {
                _uiState.update {
                    it.copy(amount = event.amount)
                }
                validateInputs()
            }

            is ExpensesUiEvent.SetFee -> {
                _uiState.update {
                    it.copy(fee = event.fee)
                }
                validateInputs()
            }

            is ExpensesUiEvent.SetCategory -> {
                _uiState.update {
                    it.copy(category = event.category)
                }
                validateInputs()
            }

            is ExpensesUiEvent.SetAccount -> {
                _uiState.update {
                    it.copy(account = event.account)
                }
                validateInputs()
            }

            is ExpensesUiEvent.SetDateTime -> {
                _uiState.update {
                    it.copy(dateTime = event.dateTime)
                }
                validateInputs()
            }

            is ExpensesUiEvent.SetNote -> _uiState.update {
                it.copy(notes = event.note)
            }
        }
    }

    private fun validateInputs() {
        _uiState.update { currentState ->
            val isValid = currentState.amount.isNotBlank() &&
                    currentState.category != null &&
                    currentState.account != null
            currentState.copy(isValid = isValid)
        }
    }
}