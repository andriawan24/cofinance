package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ActivityScreen() {
    val activityViewModel: ActivityViewModel = koinViewModel()
    val uiState by activityViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        activityViewModel.fetchTransaction()
    }

    ActivityContent(
        uiState = uiState,
        onEvent = { activityViewModel.onEvent(it) },
        onBookmarkClicked = {
            // TODO: Handle bookmark page open
        }
    )
}
