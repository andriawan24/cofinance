package id.andriawan24.cofinance.andro.ui.presentation.splashscreen

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import id.andriawan24.cofinance.domain.usecase.authentication.FetchUserUseCase


@Stable
class SplashViewModel(private val fetchUserUseCase: FetchUserUseCase) : ViewModel() {
    fun fetchUser() = fetchUserUseCase.execute()
}