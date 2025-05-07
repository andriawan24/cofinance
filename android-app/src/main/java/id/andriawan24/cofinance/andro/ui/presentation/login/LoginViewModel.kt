package id.andriawan24.cofinance.andro.ui.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.domain.model.IdTokenParam
import id.andriawan24.cofinance.domain.usecase.SignInIdTokenUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class LoginEvent {
    data object NavigateHomePage : LoginEvent()
    data class ShowMessage(val message: String) : LoginEvent()
}

class LoginViewModel(private val signInIdTokenUseCase: SignInIdTokenUseCase) : ViewModel() {

    private val _loginEvent = Channel<LoginEvent>(Channel.BUFFERED)
    val loginEvent = _loginEvent.receiveAsFlow()

    fun signInWithIdToken(param: IdTokenParam) {
        viewModelScope.launch {
            signInIdTokenUseCase.execute(param).collectLatest {
                when {
                    it.isSuccess -> _loginEvent.send(LoginEvent.NavigateHomePage)
                    it.isFailure -> {
                        _loginEvent.send(LoginEvent.ShowMessage(it.exceptionOrNull()?.message.orEmpty()))
                    }
                }
            }
        }
    }
}