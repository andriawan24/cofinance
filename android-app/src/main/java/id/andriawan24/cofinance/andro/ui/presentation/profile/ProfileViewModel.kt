package id.andriawan24.cofinance.andro.ui.presentation.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileEvent.NavigateToLoginPage
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileEvent.ShowMessage
import id.andriawan24.cofinance.andro.ui.util.mapAuthErrorMessage
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import id.andriawan24.cofinance.utils.ResultState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class ProfileEvent {
    data object NavigateToLoginPage : ProfileEvent()
    data class ShowMessage(val message: String) : ProfileEvent()
}

data class UiState(
    val isShowDialogLogout: Boolean = false
)

@Stable
class ProfileViewModel(
    getUserUseCase: GetUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _profileEvent = Channel<ProfileEvent>(Channel.BUFFERED)
    val profileEvent = _profileEvent.receiveAsFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    val user = getUserUseCase.execute()

    fun toggleDialogLogout(isShow: Boolean) {
        _uiState.update { state -> state.copy(isShowDialogLogout = isShow) }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase.execute().collectLatest {
                when (it) {
                    ResultState.Loading -> {
                        // Do nothing
                    }

                    is ResultState.Error -> {
                        _profileEvent.send(ShowMessage(mapAuthErrorMessage(it.exception)))
                    }

                    is ResultState.Success<*> -> {
                        _profileEvent.send(NavigateToLoginPage)
                    }
                }
            }
        }
    }
}