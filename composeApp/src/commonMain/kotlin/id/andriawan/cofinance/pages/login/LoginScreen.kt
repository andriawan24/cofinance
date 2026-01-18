package id.andriawan.cofinance.pages.login

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit) {
    val scope = rememberCoroutineScope()
    val supabase = koinInject<SupabaseClient>()

    val snackState = remember { SnackbarHostState() }
    val authState = supabase.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Error -> {
                    scope.launch {
                        snackState.showSnackbar(result.message)
                    }
                }

                NativeSignInResult.Success -> onNavigateToHome()

                else -> {
                    /* no-op */
                }
            }
        }
    )

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) {
        LoginContent(
            contentPadding = it,
            uiState = LoginUiState(),
            onContinueClicked = {
                authState.startFlow()
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
