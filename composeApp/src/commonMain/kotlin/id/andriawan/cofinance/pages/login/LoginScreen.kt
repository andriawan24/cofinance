package id.andriawan.cofinance.pages.login

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.compose.LocalPlatformContext
import id.andriawan.cofinance.components.ErrorBottomSheet
import id.andriawan.cofinance.utils.UiText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit) {
    val viewModel = koinViewModel<LoginViewModel>()
    val uiState by viewModel.loginUiState.collectAsState()
    val context = LocalPlatformContext.current
    var errorUiText by remember { mutableStateOf<UiText?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loginEvent.collect { event ->
            when (event) {
                is LoginUiEvent.NavigateHomePage -> onNavigateToHome()
                is LoginUiEvent.ShowMessage -> errorUiText = event.message
            }
        }
    }

    Scaffold { contentPadding ->
        LoginContent(
            contentPadding = contentPadding,
            uiState = uiState,
            onContinueClicked = { viewModel.signInWithIdToken(context) }
        )
    }

    ErrorBottomSheet(
        message = errorUiText?.asString(),
        onDismiss = { errorUiText = null }
    )
}
