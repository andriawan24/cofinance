package id.andriawan24.cofinance.andro.ui.presentation.home

import androidx.lifecycle.ViewModel
import id.andriawan24.cofinance.domain.usecase.GetUserUseCase

class HomeViewModel(getUserUseCase: GetUserUseCase) : ViewModel() {
    val user = getUserUseCase.execute()
}