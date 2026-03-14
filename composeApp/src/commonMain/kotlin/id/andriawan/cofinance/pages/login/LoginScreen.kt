package id.andriawan.cofinance.pages.login

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit) {
    val viewModel = koinViewModel<LoginViewModel>()
    val uiState by viewModel.loginUiState.collectAsState()
    val snackState = remember { SnackbarHostState() }
    val context = LocalPlatformContext.current

    LaunchedEffect(Unit) {
        viewModel.loginEvent.collect { event ->
            when (event) {
                is LoginUiEvent.NavigateHomePage -> onNavigateToHome()
                is LoginUiEvent.ShowMessage -> snackState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) {
        LoginContent(
            contentPadding = it,
            uiState = uiState,
            onContinueClicked = { viewModel.signInWithIdToken(context) }
        )
    }
}
