package id.andriawan.cofinance.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.andriawan.cofinance.utils.Dimensions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BaseBottomSheet(
    state: SheetState,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(
            topStart = Dimensions.SIZE_10,
            topEnd = Dimensions.SIZE_10
        ),
        onDismissRequest = onDismissRequest
    ) {
        content()
    }
}
