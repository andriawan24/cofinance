package id.andriawan.cofinance.pages.login

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update


sealed class LoginUiEvent {
    data object NavigateHomePage : LoginUiEvent()
    data class ShowMessage(val message: String) : LoginUiEvent()
}

data class LoginUiState(
    val isLoading: Boolean = false
)


@Stable
class LoginViewModel() : ViewModel() {
    private val _loginUiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val loginEvent = _loginUiEvent.receiveAsFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState = _loginUiState.asStateFlow()

    fun signInWithIdToken() {
        // TODO: Implement sign in with id token
    }

    fun setLoading(isLoading: Boolean) {
        _loginUiState.update { state -> state.copy(isLoading = isLoading) }
    }
}