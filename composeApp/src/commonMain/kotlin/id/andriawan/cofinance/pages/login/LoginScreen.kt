package id.andriawan.cofinance.pages.login

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }

//    val uiState by viewModel.loginUiState.collectAsStateWithLifecycle()
//
//    viewModel.loginEvent.CollectAsEffect {
//        when (it) {
//            LoginUiEvent.NavigateHomePage -> onNavigateToHome()
//            is LoginUiEvent.ShowMessage -> scope.launch {
//                snackState.showSnackbar(it.message)
//            }
//        }
//    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) {
        LoginContent(
            contentPadding = it,
            uiState = LoginUiState(),
            onContinueClicked = {
                // TODO: Implement google sign in
//                viewModel.setLoading(true)
//                scope.launch {
//                    AuthHelper.signInGoogle(
//                        context = context,
//                        credentialManager = credentialManager,
//                        onSignedIn = { idToken -> viewModel.signInWithIdToken(IdTokenParam(idToken)) },
//                        onSignedInFailed = { message ->
//                            viewModel.setLoading(false)
//                            if (message.isNotBlank()) {
//                                scope.launch {
//                                    snackState.showSnackbar(message)
//                                }
//                            }
//                        }
//                    )
//                }
            }
        )
    }
}