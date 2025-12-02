package id.andriawan24.cofinance.andro.ui.presentation.login

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.util.mapErrorMessage
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

sealed class LoginEvent {
    data object NavigateHomePage : LoginEvent()
    data class ShowMessage(@StringRes val messageResId: Int) : LoginEvent()
}

data class LoginUiState(
    val isLoading: Boolean = false
)

@Stable
class LoginViewModel(private val loginIdTokenUseCase: LoginIdTokenUseCase) : ViewModel() {
    private val _loginEvent = Channel<LoginEvent>(Channel.BUFFERED)
    val loginEvent = _loginEvent.receiveAsFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState = _loginUiState.asStateFlow()

    fun signInWithIdToken(param: IdTokenParam) {
        viewModelScope.launch {
            loginIdTokenUseCase.execute(param).collectLatest { result ->
                when (result) {
                    ResultState.Loading -> setLoading(true)
                    is ResultState.Error -> {
                        setLoading(false)
                        val messageResId = mapErrorMessage(
                            exception = result.exception,
                            fallbackResId = R.string.error_authentication_generic
                        )
                        _loginEvent.send(LoginEvent.ShowMessage(messageResId))
                    }

                    is ResultState.Success<Boolean> -> {
                        setLoading(false)
                        _loginEvent.send(LoginEvent.NavigateHomePage)
                    }
                }
            }
        }
    }

    fun setLoading(isLoading: Boolean) {
        _loginUiState.update { state -> state.copy(isLoading = isLoading) }
    }
}