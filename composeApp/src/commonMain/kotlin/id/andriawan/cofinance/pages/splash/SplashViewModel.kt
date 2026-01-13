package id.andriawan.cofinance.pages.splash

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import id.andriawan.cofinance.domain.usecases.authentications.FetchUserUseCase

@Stable
class SplashViewModel(private val fetchUserUseCase: FetchUserUseCase) : ViewModel() {
    fun fetchUser() = fetchUserUseCase.execute()
}
