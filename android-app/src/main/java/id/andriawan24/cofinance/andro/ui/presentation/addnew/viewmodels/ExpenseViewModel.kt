package id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import id.andriawan24.cofinance.andro.utils.emptyString
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
    var category: String = emptyString(),
    var dateTime: Date = Date(),
    var notes: String = emptyString()
)

sealed class ExpensesUiEvent {
    data class SetIncludeFee(val includeFee: Boolean) : ExpensesUiEvent()
    data class SetAmount(val amount: String) : ExpensesUiEvent()
    data class SetFee(val fee: String) : ExpensesUiEvent()
}

class ExpenseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState = _uiState.asStateFlow()

    fun init(totalPrice: Long, date: String, imageUri: String) {
        _uiState.update {
            it.copy(
                amount = totalPrice.takeIf { price -> price > 0 }?.toString().orEmpty(),
                dateTime = if (date.isNotBlank()) getDateTimeFromIsoString(date) else Date(),
                imageUri = if (imageUri.isBlank()) null else imageUri.toUri()
            )
        }
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

            is ExpensesUiEvent.SetAmount -> _uiState.update {
                it.copy(amount = event.amount)
            }

            is ExpensesUiEvent.SetFee -> _uiState.update {
                it.copy(fee = event.fee)
            }
        }
    }
}