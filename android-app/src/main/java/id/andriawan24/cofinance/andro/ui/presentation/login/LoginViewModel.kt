package id.andriawan24.cofinance.andro.ui.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.ui.util.mapErrorMessage
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import id.andriawan24.cofinance.domain.usecase.authentication.LoginIdTokenUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class LoginEvent {
    data object NavigateHomePage : LoginEvent()
    data class ShowMessage(val message: String) : LoginEvent()
}

class LoginViewModel(private val loginIdTokenUseCase: LoginIdTokenUseCase) : ViewModel() {
    private val _loginEvent = Channel<LoginEvent>(Channel.BUFFERED)
    val loginEvent = _loginEvent.receiveAsFlow()

    fun signInWithIdToken(param: IdTokenParam) {
        viewModelScope.launch {
            loginIdTokenUseCase.execute(param).collectLatest {
                when {
                    it.isSuccess -> _loginEvent.send(LoginEvent.NavigateHomePage)
                    it.isFailure -> {
                        val message = mapErrorMessage(
                            exception = it.exceptionOrNull(),
                            fallbackMessage = AUTHENTICATION_GENERIC_ERROR_MESSAGE
                        )
                        _loginEvent.send(LoginEvent.ShowMessage(message))
                    }
                }
            }
        }
    }

    private companion object {
        const val AUTHENTICATION_GENERIC_ERROR_MESSAGE = "Authentication failed. Please try again."
    }
}