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
import id.andriawan.cofinance.auth.GoogleAuthManager
import id.andriawan.cofinance.auth.GoogleAuthResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Displays the login UI and manages the Google sign-in flow.
 *
 * Initiates Google sign-in when the user continues, exchanges the returned ID token with Supabase to authenticate,
 * navigates to the home screen on successful authentication, and shows error messages in a snackbar on failure.
 *
 * @param onNavigateToHome Callback invoked after a successful Supabase sign-in to navigate to the home screen.
 */
@Composable
fun LoginScreen(onNavigateToHome: () -> Unit) {
    val scope = rememberCoroutineScope()
    val supabase = koinInject<SupabaseClient>()
    val googleAuthManager = remember { GoogleAuthManager() }

    val snackState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) {
        LoginContent(
            contentPadding = it,
            uiState = LoginUiState(isLoading = isLoading),
            onContinueClicked = {
                scope.launch {
                    when (val result = googleAuthManager.signIn()) {
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