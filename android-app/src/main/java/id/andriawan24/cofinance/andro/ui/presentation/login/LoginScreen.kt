package id.andriawan24.cofinance.andro.ui.presentation.login

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.utils.AuthHelper
import id.andriawan24.cofinance.andro.utils.BiometricAuthHelper
import id.andriawan24.cofinance.andro.utils.BiometricPreferenceManager
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.SecureTokenStorage
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val biometricEnabled by BiometricPreferenceManager
        .biometricEnabledFlow(context)
        .collectAsState(initial = false)
    var cachedToken by remember { mutableStateOf(SecureTokenStorage.getToken(context)) }
    val canUseBiometric =
        biometricEnabled && !cachedToken.isNullOrBlank() && BiometricAuthHelper.isBiometricAvailable(context)
    val activity = context as? FragmentActivity

    fun showMessage(message: String) {
        scope.launch {
            snackState.showSnackbar(message)
        }
    }

    fun signInWithGoogle() {
        scope.launch {
            AuthHelper.signInGoogle(
                context = context,
                credentialManager = credentialManager,
                onSignedIn = { idToken ->
                    viewModel.signInWithIdToken(IdTokenParam(idToken))
                    scope.launch {
                        val isEnabled = BiometricPreferenceManager.isBiometricEnabled(context)
                        withContext(Dispatchers.IO) {
                            if (isEnabled) {
                                SecureTokenStorage.saveToken(context, idToken)
                            } else {
                                SecureTokenStorage.clearToken(context)
                            }
                        }
                        cachedToken = if (isEnabled) idToken else null
                    }
                },
                onSignedInFailed = { message ->
                    showMessage(message)
                }
            )
        }
    }

    fun signInWithBiometrics() {
        val savedToken = cachedToken
        if (savedToken.isNullOrBlank()) {
            showMessage(context.getString(R.string.message_biometric_token_missing))
            return
        }

        val currentActivity = activity
        if (currentActivity == null) {
            showMessage(context.getString(R.string.error_biometric_generic))
            return
        }

        BiometricAuthHelper.authenticate(
            activity = currentActivity,
            onSuccess = {
                viewModel.signInWithIdToken(IdTokenParam(savedToken))
            },
            onFallback = { signInWithGoogle() },
            onError = { message -> showMessage(message) }
        )
    }

    viewModel.loginEvent.CollectAsEffect {
        when (it) {
            LoginEvent.NavigateHomePage -> onNavigateToHome()
            is LoginEvent.ShowMessage -> scope.launch {
                snackState.showSnackbar(it.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { paddingValues ->
        LoginContent(
            modifier = Modifier.padding(paddingValues),
            onContinueClicked = ::signInWithGoogle,
            showBiometricOption = canUseBiometric,
            onBiometricClicked = ::signInWithBiometrics
        )
    }
}