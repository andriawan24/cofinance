package id.andriawan24.cofinance.andro.ui.presentation.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.util.mapErrorMessage
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class ProfileEvent {
    data object NavigateToLoginPage : ProfileEvent()
    data class ShowMessage(@StringRes val messageResId: Int) : ProfileEvent()
}

class ProfileViewModel(
    private val getUserUseCase: GetUserUseCase,
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
                        val messageResId = mapErrorMessage(
                            exception = it.exceptionOrNull(),
                            fallbackResId = R.string.error_logout_generic
                        )
                        _profileEvent.send(ProfileEvent.ShowMessage(messageResId))
                    }
                }
            }
        }
    }
}