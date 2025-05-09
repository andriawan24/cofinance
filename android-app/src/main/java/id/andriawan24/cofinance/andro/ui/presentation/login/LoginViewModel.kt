package id.andriawan24.cofinance.andro.ui.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import id.andriawan24.cofinance.domain.usecase.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.LoginIdTokenUseCase
import id.andriawan24.cofinance.domain.usecase.LogoutUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class LoginEvent {
    data object NavigateHomePage : LoginEvent()
    data object NavigateLoginPage : LoginEvent()
    data class ShowMessage(val message: String) : LoginEvent()
}

class LoginViewModel(
    private val loginIdTokenUseCase: LoginIdTokenUseCase,
    getUserUseCase: GetUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _loginEvent = Channel<LoginEvent>(Channel.BUFFERED)
    val loginEvent = _loginEvent.receiveAsFlow()

    val user = getUserUseCase.execute()
        .catch { Napier.e("Failed to get user", it) }
        .map { it.getOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun logout() {
        viewModelScope.launch {
            logoutUseCase.execute().collectLatest {
                when {
                    it.isSuccess -> _loginEvent.send(LoginEvent.NavigateLoginPage)
                    it.isFailure -> {
                        _loginEvent.send(LoginEvent.ShowMessage(it.exceptionOrNull()?.message.orEmpty()))
                    }
                }
            }
        }
    }

    fun signInWithIdToken(param: IdTokenParam) {
        viewModelScope.launch {
            loginIdTokenUseCase.execute(param).collectLatest {
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