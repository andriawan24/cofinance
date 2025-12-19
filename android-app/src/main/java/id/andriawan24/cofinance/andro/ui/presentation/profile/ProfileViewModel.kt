package id.andriawan24.cofinance.andro.ui.presentation.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileEvent.NavigateToLoginPage
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileEvent.ShowMessage
import id.andriawan24.cofinance.domain.model.response.User
import id.andriawan24.cofinance.domain.usecase.authentication.FetchUserUseCase
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
    val isShowDialogLogout: Boolean = false,
    val user: User = User(),
    val isRefreshingProfile: Boolean = false
)

@Stable
class ProfileViewModel(
    private val getUserUseCase: GetUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val fetchUserUseCase: FetchUserUseCase
) : ViewModel() {

    private val _profileEvent = Channel<ProfileEvent>(Channel.BUFFERED)
    val profileEvent = _profileEvent.receiveAsFlow()

    private val _uiState = MutableStateFlow(UiState(user = getUserUseCase.execute()))
    val uiState = _uiState.asStateFlow()

    init {
        refreshUser()
    }

    fun toggleDialogLogout(isShow: Boolean) {
        _uiState.update { state -> state.copy(isShowDialogLogout = isShow) }
    }

    fun refreshUser() {
        viewModelScope.launch {
            fetchUserUseCase.execute().collectLatest { result ->
                when (result) {
                    ResultState.Loading -> {
                        _uiState.update { it.copy(isRefreshingProfile = true) }
                    }

                    is ResultState.Success<User> -> {
                        _uiState.update {
                            it.copy(
                                user = result.data,
                                isRefreshingProfile = false
                            )
                        }
                    }

                    is ResultState.Error -> {
                        _uiState.update { it.copy(isRefreshingProfile = false) }
                        _profileEvent.send(ShowMessage(result.exception.message.orEmpty()))
                    }
                }
            }
        }
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