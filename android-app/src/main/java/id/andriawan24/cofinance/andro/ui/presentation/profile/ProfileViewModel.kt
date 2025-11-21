package id.andriawan24.cofinance.andro.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.domain.model.response.User
import id.andriawan24.cofinance.domain.usecase.authentication.FetchUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class ProfileEvent {
    data object NavigateToLoginPage : ProfileEvent()
    data class ShowMessage(val message: String) : ProfileEvent()
}

class ProfileViewModel(
    private val getUserUseCase: GetUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val fetchUserUseCase: FetchUserUseCase
) : ViewModel() {

    private val _profileEvent = Channel<ProfileEvent>(Channel.BUFFERED)
    val profileEvent = _profileEvent.receiveAsFlow()

    private val _user = MutableStateFlow(getUserUseCase.execute())
    val user: StateFlow<User> = _user.asStateFlow()

    init {
        refreshUser()
    }

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

    fun refreshUser() {
        viewModelScope.launch {
            fetchUserUseCase.execute().collectLatest { result ->
                when {
                    result.isSuccess -> {
                        result.getOrNull()?.let { updatedUser ->
                            _user.value = updatedUser
                        }
                    }

                    result.isFailure -> {
                        _profileEvent.send(
                            ProfileEvent.ShowMessage(result.exceptionOrNull()?.message.orEmpty())
                        )
                    }
                }
            }
        }
    }
}