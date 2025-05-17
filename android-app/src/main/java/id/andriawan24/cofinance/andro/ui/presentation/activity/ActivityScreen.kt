package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel

@Composable
fun ActivityScreen(onSeeAllTransactionClicked: () -> Unit) {
    ActivityContent(onBookmarkClicked = {
        // TODO: Handle bookmark page open
    })
}
