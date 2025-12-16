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
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.utils.AuthHelper
import id.andriawan24.cofinance.andro.utils.BiometricAuthHelper
import id.andriawan24.cofinance.andro.utils.BiometricAuthHelper.BiometricStatus
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.ext.findFragmentActivity
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit, viewModel: LoginViewModel = koinViewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackState = remember { SnackbarHostState() }
    val credentialManager = remember { CredentialManager.create(context) }
    val hostActivity = context.findFragmentActivity()

    val uiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val biometricStatus = BiometricAuthHelper.getStatus(context)
    val showBiometricButton =
        uiState.isBiometricEnabled && uiState.hasStoredToken && biometricStatus == BiometricStatus.Available

    val biometricHelperText = when {
        !uiState.isBiometricEnabled -> null
        uiState.isBiometricEnabled && !uiState.hasStoredToken ->
            context.getString(R.string.message_biometric_missing_token)
        uiState.isBiometricEnabled && biometricStatus == BiometricStatus.NoneEnrolled ->
            context.getString(R.string.message_biometric_not_enrolled)
        uiState.isBiometricEnabled && biometricStatus == BiometricStatus.NoHardware ->
            context.getString(R.string.message_biometric_no_hardware)
        uiState.isBiometricEnabled && biometricStatus == BiometricStatus.HardwareUnavailable ->
            context.getString(R.string.message_biometric_hw_unavailable)
        uiState.isBiometricEnabled && biometricStatus == BiometricStatus.SecurityUpdateRequired ->
            context.getString(R.string.message_biometric_security_update)
        uiState.isBiometricEnabled && biometricStatus == BiometricStatus.Unsupported ->
            context.getString(R.string.message_biometric_unsupported)
        else -> null
    }

    fun showSnackbar(message: String) {
        if (message.isBlank()) return
        scope.launch {
            snackState.showSnackbar(message)
        }
    }

    val startGoogleSignIn: () -> Unit = {
        viewModel.setLoading(true)
        scope.launch {
            AuthHelper.signInGoogle(
                context = context,
                credentialManager = credentialManager,
                onSignedIn = { idToken -> viewModel.authenticateWithGoogleToken(idToken) },
                onSignedInFailed = { message ->
                    viewModel.setLoading(false)
                    if (message.isNotBlank()) {
                        showSnackbar(message)
                    }
                }
            )
        }
    }

    viewModel.loginEvent.CollectAsEffect {
        when (it) {
            LoginUiEvent.NavigateHomePage -> onNavigateToHome()
            is LoginUiEvent.ShowMessage -> scope.launch {
                val message = it.exception.message.orEmpty() // TODO: Create error handler
                snackState.showSnackbar(message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) {
        LoginContent(
            contentPadding = it,
            uiState = uiState,
            showBiometricButton = showBiometricButton,
            biometricHelperText = biometricHelperText,
            onBiometricClicked = {
                if (hostActivity == null) {
                    showSnackbar(context.getString(R.string.error_biometric_no_activity))
                    return@LoginContent
                }
                BiometricAuthHelper.authenticate(
                    activity = hostActivity,
                    onSuccess = {
                        viewModel.authenticateWithStoredToken(
                            onTokenMissing = {
                                showSnackbar(context.getString(R.string.message_biometric_missing_token))
                                startGoogleSignIn()
                            }
                        )
                    },
                    onFallback = {
                        showSnackbar(context.getString(R.string.message_biometric_cancelled))
                    },
                    onError = { message ->
                        showSnackbar(message.ifBlank { context.getString(R.string.error_biometric_failed) })
                    }
                )
            },
            onContinueClicked = startGoogleSignIn
        )
    }
}