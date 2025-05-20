package id.andriawan24.cofinance.andro.ui.presentation.activity

import androidx.lifecycle.ViewModel
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase

class ActivityViewModel(getUserUseCase: GetUserUseCase) : ViewModel() {
    val user = getUserUseCase.execute()
}