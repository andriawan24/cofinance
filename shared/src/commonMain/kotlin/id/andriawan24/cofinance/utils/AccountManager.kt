package id.andriawan24.cofinance.utils

import id.andriawan24.cofinance.domain.usecase.GetUserUseCase
import kotlinx.coroutines.flow.map

class AccountManager(private val getUserUseCase: GetUserUseCase) {
    fun getUser() = getUserUseCase.execute().map { it.getOrNull() }
}