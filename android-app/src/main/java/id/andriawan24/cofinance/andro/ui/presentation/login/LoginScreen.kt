package id.andriawan24.cofinance.andro.ui.presentation.login

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.utils.AuthHelper
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    appState: CofinanceAppState,
    context: Context = LocalContext.current,
    viewModel: LoginViewModel = koinViewModel()
) {
    val credentialManager = remember { CredentialManager.create(context) }
    viewModel.loginEvent.CollectAsEffect {
        when (it) {
            LoginEvent.NavigateHomePage -> appState.navController.navigate(Destinations.Activity) {
                launchSingleTop = true
                popUpTo(0) {
                    inclusive = true
                }
            }

            is LoginEvent.ShowMessage -> appState.coroutineScope.launch {
                appState.snackBarHostState.showSnackbar(it.message)
            }
        }
    }

    LoginContent(
        onContinueClicked = {
            appState.coroutineScope.launch {
                AuthHelper.signInGoogle(
                    context = context,
                    credentialManager = credentialManager,
                    onSignedIn = { viewModel.signInWithIdToken(IdTokenParam(it)) },
                    onSignedInFailed = appState::showSnackbar
                )
            }
        }
    )
}