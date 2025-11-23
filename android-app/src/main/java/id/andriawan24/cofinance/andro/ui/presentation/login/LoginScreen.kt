package id.andriawan24.cofinance.andro.ui.presentation.login

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.andriawan24.cofinance.andro.utils.AuthHelper
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit, viewModel: LoginViewModel = koinViewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackState = remember { SnackbarHostState() }
    val credentialManager = remember { CredentialManager.create(context) }

    val uiState by viewModel.loginUiState.collectAsStateWithLifecycle()

    viewModel.loginEvent.CollectAsEffect {
        when (it) {
            LoginEvent.NavigateHomePage -> onNavigateToHome()
            is LoginEvent.ShowMessage -> scope.launch {
                val message = context.getString(it.messageResId)
                snackState.showSnackbar(message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) {
        LoginContent(
            contentPadding = it,
            uiState = uiState,
            onContinueClicked = {
                viewModel.setLoading(true)
                scope.launch {
                    AuthHelper.signInGoogle(
                        context = context,
                        credentialManager = credentialManager,
                        onSignedIn = { idToken -> viewModel.signInWithIdToken(IdTokenParam(idToken)) },
                        onSignedInFailed = { message ->
                            viewModel.setLoading(false)
                            if (message.isNotBlank()) {
                                scope.launch {
                                    snackState.showSnackbar(message)
                                }
                            }
                        }
                    )
                }
            }
        )
    }
}