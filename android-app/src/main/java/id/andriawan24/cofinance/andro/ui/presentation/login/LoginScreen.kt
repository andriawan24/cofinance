package id.andriawan24.cofinance.andro.ui.presentation.login

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.presentation.login.components.LoginContent
import id.andriawan24.cofinance.andro.ui.presentation.login.components.OnboardingContent
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.AuthHelper
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
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
            LoginEvent.NavigateHomePage -> appState.navController.navigate(Destinations.Home) {
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