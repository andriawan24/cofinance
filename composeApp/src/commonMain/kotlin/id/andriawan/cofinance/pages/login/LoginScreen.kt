package id.andriawan.cofinance.pages.login

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import coil3.compose.LocalPlatformContext
import id.andriawan.cofinance.auth.GoogleAuthManager
import id.andriawan.cofinance.auth.GoogleAuthResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit) {
    val scope = rememberCoroutineScope()
    val supabase = koinInject<SupabaseClient>()
    val googleAuthManager = remember { GoogleAuthManager() }

    val snackState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalPlatformContext.current

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) {
        LoginContent(
            contentPadding = it,
            uiState = LoginUiState(isLoading = isLoading),
            onContinueClicked = {
                scope.launch {
                    when (val result = googleAuthManager.signIn(context)) {
                        is GoogleAuthResult.Success -> {
                            try {
                                supabase.auth.signInWith(IDToken) {
                                    idToken = result.idToken
                                    provider = Google
                                }
                                onNavigateToHome()
                            } catch (e: Exception) {
                                snackState.showSnackbar(
                                    e.message ?: "Failed to sign in. Please try again."
                                )
                            }
                        }

                        is GoogleAuthResult.Error -> {
                            snackState.showSnackbar(result.message)
                        }

                        is GoogleAuthResult.Cancelled -> {
                            // User cancelled, no action needed
                        }
                    }
                }
            }
        )
    }
}
