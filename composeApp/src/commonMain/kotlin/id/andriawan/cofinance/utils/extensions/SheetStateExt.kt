package id.andriawan.cofinance.utils.extensions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
suspend fun SheetState.hideAndDismiss(onDismiss: () -> Unit) {
    hide()
    withContext(Dispatchers.Main) {
        onDismiss()
    }
}
