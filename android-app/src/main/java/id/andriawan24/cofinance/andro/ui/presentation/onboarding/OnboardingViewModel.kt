package id.andriawan24.cofinance.andro.ui.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.utils.None
import id.andriawan24.cofinance.domain.usecase.FetchUserUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(private val fetchUserUseCase: FetchUserUseCase) : ViewModel() {

    private val _navigateToHome = Channel<None>(Channel.BUFFERED)
    val navigateToHome = _navigateToHome.receiveAsFlow()

    fun getUser() {
        viewModelScope.launch {
            fetchUserUseCase.execute()
                .collectLatest {
                    if (it.isSuccess) {
                        _navigateToHome.send(None)
                    }
                }
        }
    }
}