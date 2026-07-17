package id.andriawan.cofinance.pages.login

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.error_sign_in_failed
import coil3.PlatformContext
import id.andriawan.cofinance.auth.GoogleAuthManager
import id.andriawan.cofinance.auth.GoogleAuthResult
import id.andriawan.cofinance.domain.model.request.IdTokenParam
import id.andriawan.cofinance.domain.usecases.authentications.LoginIdTokenUseCase
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class LoginUiEvent {
    data object NavigateHomePage : LoginUiEvent()
    data class ShowMessage(val message: UiText) : LoginUiEvent()
}

data class LoginUiState(
    val isLoading: Boolean = false
)

@Stable
class LoginViewModel(
    private val loginIdTokenUseCase: LoginIdTokenUseCase,
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
                    loginIdTokenUseCase.execute(IdTokenParam(result.idToken)).collect { state ->
                        when (state) {
                            is ResultState.Success -> _loginUiEvent.send(LoginUiEvent.NavigateHomePage)
                            is ResultState.Error -> _loginUiEvent.send(
                                LoginUiEvent.ShowMessage(
                                    state.exception.message?.let(UiText::Raw)
                                        ?: UiText.Res(Res.string.error_sign_in_failed)
                                )
                            )
                            ResultState.Loading -> Unit
                        }
                    }
                }

                is GoogleAuthResult.Error -> {
                    _loginUiEvent.send(LoginUiEvent.ShowMessage(UiText.Raw(result.message)))
                }

                is GoogleAuthResult.Cancelled -> {
                    // User canceled, no action needed
                }
            }

            _loginUiState.update { it.copy(isLoading = false) }
        }
    }
}
