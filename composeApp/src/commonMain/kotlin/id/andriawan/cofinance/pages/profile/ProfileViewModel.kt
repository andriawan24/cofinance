package id.andriawan.cofinance.pages.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.domain.usecases.authentications.GetUserUseCase
import id.andriawan.cofinance.domain.usecases.authentications.LogoutUseCase
import id.andriawan.cofinance.pages.profile.ProfileEvent.NavigateToLoginPage
import id.andriawan.cofinance.pages.profile.ProfileEvent.ShowMessage
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.UiText
import id.andriawan.cofinance.utils.mapAuthErrorMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import id.andriawan.cofinance.utils.collectResult
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class ProfileEvent {
    data object NavigateToLoginPage : ProfileEvent()
    data class ShowMessage(val message: UiText) : ProfileEvent()
}

data class UiState(
    val isShowDialogLogout: Boolean = false
)

@Stable
class ProfileViewModel(
    private val getUserUseCase: GetUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val database: CofinanceDatabase
) : ViewModel() {

    private val _profileEvent = Channel<ProfileEvent>(Channel.BUFFERED)
    val profileEvent = _profileEvent.receiveAsFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _user = MutableStateFlow(getUserUseCase.execute())
    val user = _user.asStateFlow()

    fun refreshUser() {
        _user.value = getUserUseCase.execute()
    }

    fun toggleDialogLogout(isShow: Boolean) {
        _uiState.update { state -> state.copy(isShowDialogLogout = isShow) }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                database.disconnectSync()
            } catch (_: Exception) {
                // Non-fatal - proceed with logout anyway
            }

            logoutUseCase.execute().collectResult(
                onError = { _profileEvent.send(ShowMessage(mapAuthErrorMessage(it))) },
                onSuccess = { _profileEvent.send(NavigateToLoginPage) }
            )
        }
    }
}
