package id.andriawan24.cofinance.andro.ui.presentation.login

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.AuthHelper
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    appState: CofinanceAppState,
    viewModel: LoginViewModel = koinViewModel()
) {
    val context: Context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }

    viewModel.loginEvent.CollectAsEffect {
        when (it) {
            LoginEvent.NavigateHomePage -> onSignedIn()
            is LoginEvent.ShowMessage -> appState.coroutineScope.launch {
                appState.snackBarHostState.showSnackbar(it.message)
            }

            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.SIZE_24, vertical = Dimensions.SIZE_36),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = stringResource(R.string.title_login),
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(height = Dimensions.SIZE_8))

        Text(
            text = stringResource(R.string.description_login),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        )

        Spacer(modifier = Modifier.height(height = Dimensions.SIZE_24))

        Button(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(vertical = Dimensions.SIZE_12),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceTint,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = {
                appState.coroutineScope.launch {
                    AuthHelper.signInGoogle(
                        context = context,
                        credentialManager = credentialManager,
                        onSignedIn = {
                            viewModel.signInWithIdToken(IdTokenParam(it))
                        },
                        onSignedInFailed = appState::showSnackbar
                    )
                }
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = Dimensions.SIZE_12,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.action_sign_in_google),
                    style = MaterialTheme.typography.labelLarge
                )

                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = stringResource(R.string.content_description_google_button)
                )
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LoginScreen(
                appState = rememberCofinanceAppState(),
                onSignedIn = { }
            )
        }
    }
}