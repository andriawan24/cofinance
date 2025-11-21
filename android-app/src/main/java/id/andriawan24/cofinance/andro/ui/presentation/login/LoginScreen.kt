package id.andriawan24.cofinance.andro.ui.presentation.login

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import id.andriawan24.cofinance.andro.utils.AuthHelper
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    context: Context = LocalContext.current,
    viewModel: LoginViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }
    val credentialManager = remember { CredentialManager.create(context) }

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
            modifier = Modifier.padding(it),
            onContinueClicked = {
                scope.launch {
                    AuthHelper.signInGoogle(
                        context = context,
                        credentialManager = credentialManager,
                        onSignedIn = { idToken ->
                            viewModel.signInWithIdToken(IdTokenParam(idToken))
                        },
                        onSignedInFailed = { message ->
                            scope.launch {
                                snackState.showSnackbar(message)
                            }
                        }
                    )
                }
            }
        )
    }
}