package id.andriawan24.cofinance.domain.usecase.authentication

import id.andriawan24.cofinance.data.repository.AuthenticationRepository


class GetUserUseCase(private val authRepository: AuthenticationRepository) {
    fun execute() = authRepository.getUser()
}