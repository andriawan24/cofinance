package id.andriawan.cofinance.pages.login

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.PlatformContext
import com.andriawan.cofinance.BuildKonfig
import id.andriawan.cofinance.auth.GoogleAuthManager
import id.andriawan.cofinance.auth.GoogleAuthResult
import id.andriawan.cofinance.data.local.CofinanceDatabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class LoginUiEvent {
    data object NavigateHomePage : LoginUiEvent()
    data class ShowMessage(val message: String) : LoginUiEvent()
}

data class LoginUiState(
    val isLoading: Boolean = false
)

@Stable
class LoginViewModel(
    private val supabase: SupabaseClient,
    private val database: CofinanceDatabase,
    private val googleAuthManager: GoogleAuthManager
) : ViewModel() {
    private val _loginUiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val loginEvent = _loginUiEvent.receiveAsFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState = _loginUiState.asStateFlow()

    fun signInWithIdToken(context: PlatformContext) {
        viewModelScope.launch {
            _loginUiState.update { it.copy(isLoading = true) }

            when (val result = googleAuthManager.signIn(context)) {
                is GoogleAuthResult.Success -> {
                    try {
                        supabase.auth.signInWith(IDToken) {
                            idToken = result.idToken
                            provider = Google
                        }
                        database.connectSync(supabase, BuildKonfig.POWERSYNC_URL)
                        _loginUiEvent.send(LoginUiEvent.NavigateHomePage)
                    } catch (e: Exception) {
                        _loginUiEvent.send(
                            LoginUiEvent.ShowMessage(
                                e.message ?: "Failed to sign in. Please try again."
                            )
                        )
                    }
                }

                is GoogleAuthResult.Error -> {
                    _loginUiEvent.send(LoginUiEvent.ShowMessage(result.message))
                }

                is GoogleAuthResult.Cancelled -> {
                    // User canceled, no action needed
                }
            }

            _loginUiState.update { it.copy(isLoading = false) }
        }
    }
}
