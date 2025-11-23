package id.andriawan24.cofinance.andro.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class ProfileEvent {
    data object NavigateToLoginPage : ProfileEvent()
    data class ShowMessage(val message: String) : ProfileEvent()
}

class ProfileViewModel(
    getUserUseCase: GetUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _profileEvent = Channel<ProfileEvent>(Channel.BUFFERED)
    val profileEvent = _profileEvent.receiveAsFlow()

    val user = getUserUseCase.execute()

    fun logout() {
        viewModelScope.launch {
            logoutUseCase.execute().collectLatest {
                when {
                    it.isSuccess -> _profileEvent.send(ProfileEvent.NavigateToLoginPage)
                    it.isFailure -> {
                        _profileEvent.send(ProfileEvent.ShowMessage(it.exceptionOrNull()?.message.orEmpty()))
                    }
                }
            }
        }
    }
}