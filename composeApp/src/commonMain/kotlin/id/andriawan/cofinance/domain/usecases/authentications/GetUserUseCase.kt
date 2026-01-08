package id.andriawan.cofinance.domain.usecases.authentications

import id.andriawan.cofinance.data.repository.AuthenticationRepository

class GetUserUseCase(private val authRepository: AuthenticationRepository) {
    fun execute() = authRepository.getUser()
}
