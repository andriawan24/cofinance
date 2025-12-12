package id.andriawan24.cofinance.andro.ui.presentation.login

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.ui.presentation.login.LoginUiEvent.NavigateHomePage
import id.andriawan24.cofinance.andro.ui.presentation.login.LoginUiEvent.ShowMessage
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import id.andriawan24.cofinance.domain.usecase.authentication.LoginIdTokenUseCase
import id.andriawan24.cofinance.utils.ResultState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class LoginUiEvent {
    data object NavigateHomePage : LoginUiEvent()
    data class ShowMessage(val exception: Exception) : LoginUiEvent()
}

data class LoginUiState(
    val isLoading: Boolean = false
)


@Stable
class LoginViewModel(private val loginIdTokenUseCase: LoginIdTokenUseCase) : ViewModel() {
    private val _loginUiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val loginEvent = _loginUiEvent.receiveAsFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState = _loginUiState.asStateFlow()

    fun signInWithIdToken(param: IdTokenParam) {
        viewModelScope.launch {
            loginIdTokenUseCase.execute(param).collectLatest {
                when (it) {
                    ResultState.Loading -> setLoading(true)
                    is ResultState.Error -> {
                        setLoading(false)
                        _loginUiEvent.send(ShowMessage(it.exception))
                    }

                    is ResultState.Success<Boolean> -> {
                        setLoading(false)
                        _loginUiEvent.send(NavigateHomePage)
                    }
                }
            }
        }
    }

    fun setLoading(isLoading: Boolean) {
        _loginUiState.update { state -> state.copy(isLoading = isLoading) }
    }
}