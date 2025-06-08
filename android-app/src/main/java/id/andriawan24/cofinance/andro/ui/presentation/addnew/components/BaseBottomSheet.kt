package id.andriawan24.cofinance.andro.ui.presentation.addnew.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import id.andriawan24.cofinance.andro.utils.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseBottomSheet(
    state: SheetState,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(
            topStart = Dimensions.SIZE_10,
            topEnd = Dimensions.SIZE_10
        ),
        onDismissRequest = onDismissRequest,
    ) {
        content()
    }
}