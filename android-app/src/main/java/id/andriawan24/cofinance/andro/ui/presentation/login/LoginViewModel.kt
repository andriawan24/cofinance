package id.andriawan24.cofinance.andro.ui.presentation.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.util.mapErrorMessage
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import id.andriawan24.cofinance.domain.usecase.authentication.LoginIdTokenUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class LoginEvent {
    data object NavigateHomePage : LoginEvent()
    data class ShowMessage(@StringRes val messageResId: Int) : LoginEvent()
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
                        val messageResId = mapErrorMessage(
                            exception = it.exceptionOrNull(),
                            fallbackResId = R.string.error_authentication_generic
                        )
                        _loginEvent.send(LoginEvent.ShowMessage(messageResId))
                    }
                }
            }
        }
    }
}