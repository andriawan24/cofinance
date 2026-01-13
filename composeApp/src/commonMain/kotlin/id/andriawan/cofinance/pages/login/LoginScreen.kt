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
import org.koin.compose.koinInject

@Composable
fun LoginScreen(onNavigateToHome: () -> Unit, supabase: SupabaseClient = koinInject()) {
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }
    val authState = supabase.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                NativeSignInResult.ClosedByUser -> {
                    println("Closed by user ${result}")
                }

                is NativeSignInResult.Error -> {
                    println("Error ${result.message}")
                }

                is NativeSignInResult.NetworkError -> {
                    println("Network Error: ${result.message}")
                }

                NativeSignInResult.Success -> {
                    println("Success sign in: $result")
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
