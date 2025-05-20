package id.andriawan24.cofinance.domain.usecase.authentication

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LogoutUseCase(private val authRepository: AuthenticationRepository) {

    fun execute(): Flow<Result<Boolean>> = flow {
        try {
            authRepository.logout()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}